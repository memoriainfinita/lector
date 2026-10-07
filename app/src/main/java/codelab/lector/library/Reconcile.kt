package codelab.lector.library

import codelab.lector.data.db.Book
import codelab.lector.data.db.ChapterInfo
import codelab.lector.data.db.CoverSource
import codelab.lector.data.db.Correction
import codelab.lector.data.db.CorrectionType
import codelab.lector.data.db.FileMeta
import java.io.File
import kotlin.math.abs

/** Margen para reconocer un libro movido y renombrado por su duración. */
const val DurationToleranceMs = 1_000L

/**
 * Aplica las correcciones (unir / separar) en orden de creación sobre lo detectado.
 * Una corrección cuyos libros ya no aparecen se ignora; sigue guardada.
 */
fun applyCorrections(books: List<DetectedBook>, corrections: List<Correction>): List<DetectedBook> {
    var result = books
    for (c in corrections.sortedBy { it.createdAt }) {
        result = when (c.type) {
            CorrectionType.MERGE -> merge(result, c.identityKeys)
            CorrectionType.SPLIT -> split(result, c.identityKeys.firstOrNull(), c.splitStartFiles)
        }
    }
    return result
}

private fun merge(books: List<DetectedBook>, keys: List<String>): List<DetectedBook> {
    val members = keys.mapNotNull { k -> books.firstOrNull { it.identityKey == k } }
    if (members.size < 2) return books
    val base = members.first()
    val parts = members.flatMap { m ->
        m.parts.map { p -> p.copy(relativePath = relativeTo(base.folderPath, p.file.path)) }
    }
    val merged = base.copy(
        identityKey = sha256("merge\n" + keys.joinToString("\n")),
        path = base.folderPath,
        parts = parts,
        wholeFolder = false,
    )
    val index = books.indexOf(base)
    return books.filterNot { it in members }.toMutableList().apply { add(index.coerceAtMost(size), merged) }
}

private fun split(books: List<DetectedBook>, key: String?, startFiles: List<String>): List<DetectedBook> {
    val book = books.firstOrNull { it.identityKey == key } ?: return books
    val segments = mutableListOf<MutableList<BookPart>>()
    for (part in book.parts) {
        if (segments.isEmpty() || part.relativePath in startFiles) segments += mutableListOf<BookPart>()
        segments.last() += part
    }
    if (segments.size < 2) return books
    val split = segments.map { parts ->
        book.copy(
            identityKey = sha256("split\n" + book.identityKey + "\n" + parts.first().relativePath),
            path = if (parts.size == 1) parts.first().file.path else book.folderPath,
            parts = parts,
            wholeFolder = false,
        )
    }
    // Con el mismo título (el álbum de todas las partes), cada uno lleva su número: "… (2/3)".
    val pieces = if (split.map { it.detectedTitle() }.distinct().size == split.size) split
    else split.mapIndexed { i, piece -> piece.copy(partLabel = "${i + 1}/${split.size}") }
    val index = books.indexOf(book)
    return books.toMutableList().apply { removeAt(index); addAll(index, pieces) }
}

private fun relativeTo(base: String, path: String): String =
    File(base).toPath().relativize(File(path).toPath()).toString().replace(File.separatorChar, '/')

/** Carpeta base de las rutas relativas de un libro: la suya o, si el libro es un archivo, la que lo contiene. */
fun baseFolder(book: Book): String = if (extensionOf(book.path) in AudioExtensions) parentOf(book.path) else book.path

/** Dónde queda un archivo tras el escaneo: libro y ruta relativa dentro de él. */
data class Placement(val bookId: String, val relativePath: String)

/**
 * Libro que ya no aparece pero cuyos archivos siguen, ahora en otros libros (cambio de clase):
 * adónde va cada uno de sus archivos. Null si falta alguno: entonces queda inaccesible.
 */
fun regroup(base: String, relativePaths: List<String>, placements: Map<String, Placement>): Map<String, Placement>? {
    if (relativePaths.isEmpty()) return null
    return relativePaths.associateWith { placements[normalizePath("$base/$it")] ?: return null }
}

/**
 * Ruta sin "." ni "..": un libro unido con otra carpeta guarda sus archivos como "../Vol 2/01.mp3".
 * Con "/" siempre, como las rutas del móvil (File.normalize usaría "\" en las pruebas en Windows).
 */
fun normalizePath(path: String): String {
    val out = ArrayDeque<String>()
    for (segment in path.split('/')) {
        when (segment) {
            "", "." -> Unit
            ".." -> out.removeLastOrNull()
            else -> out.addLast(segment)
        }
    }
    return (if (path.startsWith("/")) "/" else "") + out.joinToString("/")
}

/** Inicio del tramo de un archivo que contiene [ms], en ms del archivo: su capítulo, o 0 sin capítulos. */
fun segmentStart(chapters: List<ChapterInfo>, ms: Long): Long = chapters.lastOrNull { it.startMs <= ms }?.startMs ?: 0

data class Match(val detected: DetectedBook, val existing: Book?)

data class Reconciliation(val matches: List<Match>, val missing: List<Book>)

/**
 * Huella del contenido: nombre y tamaño de cada archivo, sin carpetas. No cambia al mover el libro ni al
 * renombrar su carpeta. Null si falta algún tamaño (libros importados de otro móvil).
 */
fun contentKey(files: List<Pair<String, Long>>): String? =
    if (files.isEmpty() || files.any { it.second <= 0 }) null
    else files.map { (path, size) -> path.substringAfterLast('/') + "\u0000" + size }.sorted().joinToString("\n")

fun DetectedBook.contentKey(): String? = contentKey(parts.map { it.file.name to it.file.meta.sizeBytes })

/**
 * Por firma entre todos los libros (uno movido desde una carpeta quitada se reconecta); si no, por
 * contenido ([contents]: id del libro → [contentKey]) también entre todos; si no, por duración ±1 s
 * entre los que no han aparecido. Solo los de [folders] entran por duración y quedan como perdidos:
 * los de una carpeta quitada no se tocan.
 */
fun reconcile(detected: List<DetectedBook>, existing: List<Book>, folders: List<String>, contents: Map<String, String> = emptyMap()): Reconciliation {
    val unmatched = existing.toMutableList()
    val byIdentity = detected.associateWith { d ->
        unmatched.firstOrNull { it.identityKey == d.identityKey }?.also { unmatched.remove(it) }
    }
    // Los mismos archivos en otra carpeta, o con la carpeta renombrada.
    val byContent = detected.filter { byIdentity[it] == null }.associateWith { d ->
        d.contentKey()?.let { key -> unmatched.firstOrNull { contents[it.id] == key }?.also { unmatched.remove(it) } }
    }
    unmatched.retainAll { book -> folders.any { isInside(book.path, it) } }
    val matches = detected.map { d ->
        val found = byIdentity[d] ?: byContent[d] ?: unmatched
            .filter { abs(it.totalDurationMs - d.durationMs) <= DurationToleranceMs }
            .minByOrNull { abs(it.totalDurationMs - d.durationMs) }
            ?.also { unmatched.remove(it) }
        Match(d, found)
    }
    return Reconciliation(matches, unmatched)
}

/** [path] es [folder] o está dentro. */
fun isInside(path: String, folder: String) = path == folder || path.startsWith("$folder/")

/** Datos leídos del primer archivo con etiquetas. */
private fun DetectedBook.tag(pick: (FileMeta) -> String?): String? =
    parts.asSequence().mapNotNull { pick(it.file.meta)?.takeIf(String::isNotBlank) }.firstOrNull()

/**
 * Título: álbum; si no, título de la pista (libro de un archivo); si no, carpeta o archivo.
 * Autor: artista del álbum o artista. Narrador: compositor.
 */
fun DetectedBook.detectedTitle(): String =
    (if (albumIsTitle) tag { it.album } else null)
        ?: (if (isSingleFile) tag { it.title } else null)
        ?: if (isSingleFile) stemOf(parts.first().file.name) else folderName

fun DetectedBook.toBook(existing: Book?, now: Long, newId: () -> String, newBookSpeed: Float, coverSource: CoverSource): Book {
    val title = detectedTitle().let { t -> partLabel?.let { "${t.trimEnd()} ($it)" } ?: t }
    val base = existing ?: Book(
        id = newId(),
        identityKey = identityKey,
        totalDurationMs = durationMs,
        path = path,
        title = title,
        addedAt = now,
        speed = newBookSpeed,
    )
    return base.copy(
        identityKey = identityKey,
        totalDurationMs = durationMs,
        path = path,
        title = title,
        author = tag { it.albumArtist } ?: tag { it.artist },
        narrator = tag { it.composer },
        series = tag { it.series },
        seriesPart = tag { it.seriesPart },
        coverSource = coverSource,
        inaccessible = false,
        removed = false,
    )
}

/** Ajustes que pasan de los libros antiguos al libro nuevo que tiene ahora sus archivos (reagrupar). */
data class Carried(
    val speed: Float,
    val skipSilence: Boolean,
    val ownSound: Boolean,
    val preampDb: Float?,
    val eqEnabled: Boolean,
    val eqBands: List<Float>?,
    val finished: Boolean,
)

/**
 * Velocidad y sonido del antiguo escuchado más recientemente; terminado solo si lo estaban todos
 * (al separar hay uno solo: pasa tal cual). Nombre propio y "quitado de recientes" no pasan.
 */
fun carryOver(sources: List<Book>): Carried? {
    val latest = sources.maxByOrNull { it.positionUpdatedAt ?: it.lastPlayedAt ?: 0 } ?: return null
    return Carried(
        speed = latest.speed,
        skipSilence = latest.skipSilence,
        ownSound = latest.ownSound,
        preampDb = latest.preampDb,
        eqEnabled = latest.eqEnabled,
        eqBands = latest.eqBands,
        finished = sources.all { it.finished },
    )
}

/** Contraportadas y miniaturas de Windows Media Player (AlbumArtSmall, AlbumArt_{…}_Small): nunca portada. */
private fun isNotCover(stem: String) =
    "back" in stem || stem == "albumartsmall" || (stem.startsWith("albumart_") && stem.endsWith("_small"))

/**
 * Imagen de carpeta, ruta relativa a [DetectedBook.folderPath]: cover/folder/front; si no, la que contiene
 * cover o front; si no, la que lleva el nombre del libro; si no, la primera. En una carpeta con más libros,
 * solo la que lleva el nombre del libro.
 */
fun DetectedBook.folderCover(): String? {
    fun stem(path: String) = stemOf(path.substringAfterLast('/')).lowercase()
    val images = folderImages.filterNot { isNotCover(stem(it)) }
    val names = (parts.map { stemOf(it.file.name) } + detectedTitle() + listOfNotNull(folderName.takeUnless { sharedFolder }))
        .map(::normalize).toSet()
    val named = images.firstOrNull { normalize(stem(it)) in names }
    if (sharedFolder) return named
    return images.firstOrNull { stem(it) in setOf("cover", "folder", "front") }
        ?: images.firstOrNull { "cover" in stem(it) || "front" in stem(it) }
        ?: named
        ?: images.firstOrNull()
}
