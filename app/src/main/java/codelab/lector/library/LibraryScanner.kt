package codelab.lector.library

import codelab.lector.data.db.Book
import codelab.lector.data.db.BookFile
import codelab.lector.data.db.BookmarkKind
import codelab.lector.data.db.Chapter
import codelab.lector.data.db.CoverSource
import codelab.lector.data.db.FileMeta
import codelab.lector.data.db.LectorDatabase
import codelab.lector.data.db.SegmentPosition
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
    /** Discreto (el rápido al abrir): solo la línea de progreso, sin texto ni tarjetas grises. */
    val quiet: Boolean = false,
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

    /**
     * Rápido: solo relee lo que cambió. Completo ([full]): relee todo y rehace las portadas.
     * Devuelve el trabajo en curso (el que ya corría, si lo había) para esperar a que termine.
     * [quiet]: solo la línea de progreso. Pedir uno no discreto con otro en curso lo hace visible.
     */
    fun start(full: Boolean = false, quiet: Boolean = false): Job {
        job?.takeIf { it.isActive }?.let { running ->
            if (!quiet) _state.update { it.copy(quiet = false) }
            return running
        }
        return scope.launch(Dispatchers.IO) {
            runCatching { scan(full, quiet) }.onFailure { e ->
                _state.update { it.copy(running = false, currentFolder = null, error = e.message ?: e.toString()) }
            }
        }.also { job = it }
    }

    private suspend fun scan(full: Boolean, quiet: Boolean) {
        _state.value = ScanState(running = true, quiet = quiet)
        val folders = db.folders().folders().map { it.path }
        val roots = folders.map(::File).filter { it.isDirectory }
        var rules = db.folders().rules()
        val stored = db.fileMeta().all()
        // Primera búsqueda (nada leído aún): lee todo, se muestra entera aunque se pidiera discreta.
        if (stored.isEmpty()) _state.update { it.copy(quiet = false) }
        val cache = if (full) emptyMap() else stored.associateBy { it.path }
        val seen = ConcurrentHashMap.newKeySet<String>()
        val fresh = ConcurrentHashMap.newKeySet<FileMeta>()

        // Fase 1: recorrer y leer. El recuento de encontrados se actualiza por subcarpeta de primer nivel.
        val trees = roots.map { walk(it, cache, seen, fresh, depth = 0) }
        db.fileMeta().upsert(fresh.toList())
        stored.map { it.path }.filterNot { it in seen }.chunked(500).forEach { db.fileMeta().delete(it) }

        // Fase 2: detectar, corregir, reconciliar y guardar.
        _state.update { it.copy(currentFolder = null) }
        val existing = db.books().all()
        val filesByBook = db.books().allFiles().groupBy { it.bookId }
        // La clase sigue a su carpeta si se movió o se renombró.
        // Huella con todos sus libros y solo con los accesibles: uno puede estar marcado inaccesible desde
        // antes (archivos borrados) o desde que arrancó el reproductor sin encontrarlo.
        val before = rules.associate { rule ->
            val inside = existing.filter { !it.removed && isInside(it.path, rule.folderPath) }
            fun key(books: List<Book>) = contentKey(books.flatMap { b -> filesByBook[b.id].orEmpty().map { it.relativePath to it.sizeBytes } })
            rule.folderPath to setOfNotNull(key(inside), key(inside.filterNot { it.inaccessible }))
        }
        val moved = movedRules(rules, trees, before) { File(it).isDirectory }
        if (moved.isNotEmpty()) {
            db.withWriteTransaction {
                for (rule in rules) {
                    val to = moved[rule.folderPath] ?: continue
                    db.folders().clearRule(rule.folderPath)
                    db.folders().setRule(rule.copy(folderPath = to))
                }
            }
            rules = db.folders().rules()
        }
        val raw = trees.flatMap { detectBooks(it, rules) }
        // Unir y separar siguen a sus libros si se movieron.
        val followed = followCorrections(raw, db.corrections().all(), existing, filesByBook)
        if (followed.isNotEmpty()) db.withWriteTransaction { followed.forEach { db.corrections().replace(it) } }
        val detected = applyCorrections(raw, db.corrections().all())
        val contents = filesByBook
            .mapNotNull { (id, files) -> contentKey(files.map { it.relativePath to it.sizeBytes })?.let { id to it } }.toMap()
        val result = reconcile(detected, existing, folders, contents)
        val now = System.currentTimeMillis()
        val written = mutableListOf<Pair<String, DetectedBook>>()
        val created = mutableSetOf<String>()
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
                if (m.existing == null) created += book.id
            }
            // Reagrupados (cambio de clase): marcadores y posición pasan a los libros que tienen ahora
            // sus archivos y el libro antiguo se borra. Los demás que faltan quedan inaccesibles.
            val placements = written.flatMap { (id, book) -> book.parts.map { it.file.path to Placement(id, it.relativePath) } }.toMap()
            val chaptersAt = written.flatMap { (id, book) -> book.parts.map { Placement(id, it.relativePath) to it.file.meta.chapters.items } }.toMap()
            // Posiciones por tramo que pasan a los libros nuevos: las del antiguo y la suya propia,
            // para que al unir no se pierda la de cada parte y al deshacer vuelva.
            val memories = mutableListOf<SegmentPosition>()
            val inaccessible = mutableListOf<String>()
            // Libros antiguos de los que viene cada libro, para pasarle sus ajustes.
            val sources = mutableMapOf<String, MutableList<Book>>()
            for (old in result.missing) {
                val moves = regroup(baseFolder(old), db.books().files(old.id).map { it.relativePath }, placements)
                val marks = db.bookmarks().forBook(old.id)
                // Un marcador sin sitio (su archivo no está en el libro) impediría borrarlo: queda inaccesible.
                if (moves == null || marks.any { it.file !in moves }) {
                    inaccessible += old.id
                    continue
                }
                for (mark in marks) {
                    val to = moves.getValue(mark.file)
                    if (mark.kind == BookmarkKind.PAUSE) db.bookmarks().replacePauseMarker(mark.copy(bookId = to.bookId, file = to.relativePath))
                    else db.bookmarks().upsert(mark.copy(bookId = to.bookId, file = to.relativePath))
                }
                val at = old.positionUpdatedAt
                val to = old.positionFile?.let { moves[it] }
                if (to != null && at != null) {
                    val current = db.books().get(to.bookId)?.positionUpdatedAt
                    if (current == null || current < at) db.books().movePosition(to.bookId, to.relativePath, old.positionMs, at)
                    memories += SegmentPosition(to.bookId, to.relativePath, segmentStart(chaptersAt[to].orEmpty(), old.positionMs), old.positionMs, at)
                }
                db.segmentPositions().forBook(old.id).forEach { m ->
                    moves[m.file]?.let { memories += m.copy(bookId = it.bookId, file = it.relativePath) }
                }
                moves.values.map { it.bookId }.distinct().forEach { sources.getOrPut(it) { mutableListOf() } += old }
                db.books().delete(old.id)
            }
            for ((id, list) in memories.groupBy { it.bookId }) {
                val book = db.books().get(id) ?: continue
                val newest = list.groupBy { it.file to it.startMs }.values.map { it.maxBy(SegmentPosition::updatedAt) }
                    .filter { id in created || it.updatedAt > (db.segmentPositions().forBook(id).firstOrNull { e -> e.file == it.file && e.startMs == it.startMs }?.updatedAt ?: 0) }
                // Un libro creado sin posición (deshacer una unión) empieza en la más reciente.
                val start = if (id in created && book.positionFile == null) newest.maxByOrNull { it.updatedAt } else null
                if (start != null) db.books().movePosition(id, start.file, start.positionMs, start.updatedAt)
                val file = start?.file ?: book.positionFile
                val ms = start?.positionMs ?: book.positionMs
                // La del tramo donde queda la posición del libro sobra: es la misma.
                val currentKey = file?.let { it to segmentStart(chaptersAt[Placement(id, it)].orEmpty(), ms) }
                newest.filter { (it.file to it.startMs) != currentKey }.forEach { db.segmentPositions().save(it) }
            }
            // Solo a los libros creados en este escaneo: uno que ya existía conserva sus ajustes.
            for ((id, olds) in sources) {
                if (id !in created) continue
                val carried = carryOver(olds) ?: continue
                db.books().setSpeed(id, carried.speed)
                db.books().setSkipSilence(id, carried.skipSilence)
                db.books().setSound(id, carried.ownSound, carried.preampDb, carried.eqEnabled, carried.eqBands)
                db.books().setFinished(id, carried.finished)
            }
            if (inaccessible.isNotEmpty()) db.books().markInaccessible(inaccessible)
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
        // Sin ocultos: la papelera de Android deja lo borrado como ".trashed-…" en la misma carpeta.
        val audio = entries.filter { it.isFile && !it.name.startsWith(".") && extensionOf(it.name) in AudioExtensions }
        val files = coroutineScope {
            audio.map { f -> async { readers.withPermit { ScannedFile(f.path, f.name, meta(f, cache, seen, fresh)) } } }.awaitAll()
        }
        val images = entries.filter { it.isFile && !it.name.startsWith(".") && extensionOf(it.name) in ImageExtensions }.map { it.name }
        // Las subcarpetas solo de imágenes ("Scans", "Artwork") quedan: dan portada al libro de encima.
        val subfolders = entries.filter { it.isDirectory && isScannableDir(it) }
            .map { walk(it, cache, seen, fresh, depth + 1) }
            .filter { it.files.isNotEmpty() || it.subfolders.isNotEmpty() || it.images.isNotEmpty() }
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
