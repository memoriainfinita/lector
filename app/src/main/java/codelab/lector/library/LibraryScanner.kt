package codelab.lector.library

import codelab.lector.data.db.BookFile
import codelab.lector.data.db.Chapter
import codelab.lector.data.db.CoverSource
import codelab.lector.data.db.FileMeta
import codelab.lector.data.db.LectorDatabase
import androidx.room3.withWriteTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ScanState(
    val running: Boolean = false,
    val found: Int = 0,
    val currentFolder: String? = null,
    val finishedAt: Long? = null,
    val error: String? = null,
)

/**
 * Escaneo en dos fases: recorrer y leer (lento, con progreso), y después detectar,
 * corregir, reconciliar y guardar de una vez (correcciones y libros movidos necesitan la vista completa).
 */
class LibraryScanner(
    private val db: LectorDatabase,
    private val reader: MetadataReader,
    private val covers: CoverStore,
    private val scope: CoroutineScope,
    private val newBookSpeed: suspend () -> Float = { 1f },
) {
    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()
    private var job: Job? = null
    private val readers = Semaphore(4)

    /** Rápido: solo relee lo que cambió. Completo ([full]): relee todo y rehace las portadas. */
    fun start(full: Boolean = false) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            runCatching { scan(full) }.onFailure { e ->
                _state.update { it.copy(running = false, currentFolder = null, error = e.message ?: e.toString()) }
            }
        }
    }

    private suspend fun scan(full: Boolean) {
        _state.value = ScanState(running = true)
        val roots = db.folders().folders().map { File(it.path) }.filter { it.isDirectory }
        val rules = db.folders().rules()
        val stored = db.fileMeta().all()
        val cache = if (full) emptyMap() else stored.associateBy { it.path }
        val seen = ConcurrentHashMap.newKeySet<String>()
        val fresh = ConcurrentHashMap.newKeySet<FileMeta>()

        // Fase 1: recorrer y leer. El recuento de encontrados se actualiza por subcarpeta de primer nivel.
        val trees = roots.map { walk(it, cache, seen, fresh, depth = 0) }
        db.fileMeta().upsert(fresh.toList())
        stored.map { it.path }.filterNot { it in seen }.chunked(500).forEach { db.fileMeta().delete(it) }

        // Fase 2: detectar, corregir, reconciliar y guardar.
        _state.update { it.copy(currentFolder = null) }
        val detected = applyCorrections(trees.flatMap { detectBooks(it, rules) }, db.corrections().all())
        val existing = db.books().all()
        val result = reconcile(detected, existing)
        val now = System.currentTimeMillis()
        val written = mutableListOf<Pair<String, DetectedBook>>()
        db.withWriteTransaction {
            for (m in result.matches) {
                val source = when {
                    m.detected.parts.any { it.file.meta.hasArtwork } -> CoverSource.EMBEDDED
                    m.detected.folderCover() != null -> CoverSource.FOLDER
                    else -> CoverSource.NONE
                }
                val book = m.detected.toBook(m.existing, now, { UUID.randomUUID().toString() }, newBookSpeed(), source)
                db.books().upsert(book)
                db.books().deleteFiles(book.id)
                m.detected.parts.forEachIndexed { i, part ->
                    val meta = part.file.meta
                    val fileId = db.books().insertFile(
                        BookFile(bookId = book.id, relativePath = part.relativePath, sortIndex = i, durationMs = meta.durationMs, sizeBytes = meta.sizeBytes),
                    )
                    if (meta.chapters.items.isNotEmpty()) {
                        db.books().insertChapters(meta.chapters.items.map { Chapter(fileId = fileId, title = it.title, startMs = it.startMs, endMs = it.endMs) })
                    }
                }
                if (m.existing?.coverSource != source) covers.delete(book.id)
                written += book.id to m.detected
            }
            if (result.missing.isNotEmpty()) db.books().markInaccessible(result.missing.map { it.id })
        }
        _state.update { it.copy(found = result.matches.size) }

        for ((id, book) in written) {
            if (!full && covers.has(id)) continue
            makeCover(id, book)
        }
        _state.value = ScanState(found = result.matches.size, finishedAt = System.currentTimeMillis())
    }

    private suspend fun walk(
        dir: File,
        cache: Map<String, FileMeta>,
        seen: MutableSet<String>,
        fresh: MutableSet<FileMeta>,
        depth: Int,
    ): ScannedFolder {
        _state.update { it.copy(currentFolder = dir.path) }
        val entries = dir.listFiles().orEmpty()
        val audio = entries.filter { it.isFile && extensionOf(it.name) in AudioExtensions }
        val files = coroutineScope {
            audio.map { f -> async { readers.withPermit { ScannedFile(f.path, f.name, meta(f, cache, seen, fresh)) } } }.awaitAll()
        }
        val images = entries.filter { it.isFile && extensionOf(it.name) in ImageExtensions }.map { it.name }
        val subfolders = entries.filter { it.isDirectory && isScannableDir(it) }
            .map { walk(it, cache, seen, fresh, depth + 1) }
            .filter { it.files.isNotEmpty() || it.subfolders.isNotEmpty() }
        val folder = ScannedFolder(dir.path, dir.name, files, images, subfolders)
        // Recuento provisional (sin reglas ni correcciones): subárboles de primer nivel y archivos sueltos de la raíz.
        val counted = when (depth) {
            0 -> folder.copy(subfolders = emptyList())
            1 -> folder
            else -> null
        }
        if (counted != null) {
            val count = detectBooks(counted, emptyList()).size
            _state.update { it.copy(found = it.found + count) }
        }
        return folder
    }

    private suspend fun meta(file: File, cache: Map<String, FileMeta>, seen: MutableSet<String>, fresh: MutableSet<FileMeta>): FileMeta {
        seen += file.path
        val cached = cache[file.path]
        if (cached != null && cached.sizeBytes == file.length() && cached.modifiedAt == file.lastModified()) return cached
        val read = runCatching { reader.read(file) }
            .getOrElse { FileMeta(path = file.path, sizeBytes = file.length(), modifiedAt = file.lastModified(), durationMs = 0) }
        fresh += read
        return read
    }

    private suspend fun makeCover(bookId: String, book: DetectedBook) {
        val embedded = book.parts.firstOrNull { it.file.meta.hasArtwork }
        val ok = embedded != null && runCatching { reader.artwork(File(embedded.file.path))?.let { covers.save(bookId, it) } ?: false }.getOrDefault(false)
        if (!ok) book.folderCover()?.let { name -> runCatching { covers.save(bookId, File(book.folderPath, name)) } }
    }
}
