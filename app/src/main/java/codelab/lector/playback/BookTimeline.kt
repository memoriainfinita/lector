package codelab.lector.playback

import codelab.lector.data.db.LectorDatabase
import codelab.lector.library.stemOf

/** Línea de tiempo de un libro guardado, sin cargarlo en el reproductor (marcadores, widget). */
suspend fun LectorDatabase.bookTimeline(bookId: String): BookTimeline {
    val chapters = books().chapters(bookId).groupBy { it.fileId }
    return BookTimeline(
        books().files(bookId).map { f ->
            TimelineFile(f.relativePath, f.durationMs, chapters[f.id].orEmpty().map { FileChapter(it.title, it.startMs, it.endMs) })
        },
    )
}

/** Capítulo dentro de un archivo, en ms relativos al archivo. */
data class FileChapter(val title: String, val startMs: Long, val endMs: Long)

data class TimelineFile(
    val relativePath: String,
    val durationMs: Long,
    val chapters: List<FileChapter> = emptyList(),
)

/** Punto dentro de un archivo del libro. */
data class FilePosition(val index: Int, val ms: Long)

/**
 * Tramo de navegación: un capítulo, o un archivo entero si no tiene capítulos.
 * Inicio y fin en ms del libro.
 */
data class Segment(val title: String, val startMs: Long, val endMs: Long, val isChapter: Boolean)

/**
 * Línea de tiempo del libro: une los archivos en una sola posición global y calcula los tramos
 * de anterior / siguiente. Un archivo con duración 0 (aún desconocida) ocupa 0 ms hasta que se corrige.
 */
class BookTimeline(val files: List<TimelineFile>) {

    /** Inicio de cada archivo en ms del libro. */
    val offsets: List<Long> = files.runningFold(0L) { acc, f -> acc + f.durationMs }.dropLast(1)

    val totalMs: Long = files.sumOf { it.durationMs }

    val hasChapters: Boolean = files.any { it.chapters.isNotEmpty() }

    val segments: List<Segment> = files.flatMapIndexed { i, f ->
        val base = offsets[i]
        if (f.chapters.isEmpty()) {
            listOf(Segment(stemOf(f.relativePath.substringAfterLast('/')), base, base + f.durationMs, isChapter = false))
        } else {
            val sorted = f.chapters.sortedBy { it.startMs }
            sorted.mapIndexed { j, c ->
                // El fin de un capítulo es el inicio del siguiente; el último acaba con el archivo.
                val end = if (j + 1 < sorted.size) sorted[j + 1].startMs else f.durationMs
                Segment(c.title, base + c.startMs.coerceIn(0, f.durationMs), base + end.coerceIn(0, f.durationMs), isChapter = true)
            }
        }
    }

    fun toBook(position: FilePosition): Long =
        offsets.getOrElse(position.index) { totalMs } + position.ms

    fun toFile(bookMs: Long): FilePosition {
        if (files.isEmpty()) return FilePosition(0, 0)
        val ms = bookMs.coerceIn(0, totalMs)
        // Último archivo con duración que empieza en o antes de la posición. En un límite exacto,
        // el inicio del archivo siguiente. Los de duración 0 solo se alcanzan por su índice.
        val index = files.indices.lastOrNull { files[it].durationMs > 0 && offsets[it] <= ms } ?: 0
        return FilePosition(index, (ms - offsets[index]).coerceIn(0, files[index].durationMs))
    }

    fun indexOfFile(relativePath: String): Int = files.indexOfFirst { it.relativePath == relativePath }

    fun segmentIndexAt(bookMs: Long): Int {
        if (segments.isEmpty()) return -1
        return segments.indexOfLast { it.startMs <= bookMs }.coerceAtLeast(0)
    }

    /** Anterior: con más de [restartThresholdMs] dentro del tramo vuelve a su inicio; si no, al tramo anterior. */
    fun previousTarget(bookMs: Long, restartThresholdMs: Long = RestartThresholdMs): Long {
        val i = segmentIndexAt(bookMs)
        if (i < 0) return 0
        val seg = segments[i]
        return if (bookMs - seg.startMs > restartThresholdMs || i == 0) seg.startMs else segments[i - 1].startMs
    }

    /** Siguiente tramo, o null si ya está en el último. */
    fun nextTarget(bookMs: Long): Long? {
        val i = segmentIndexAt(bookMs)
        return segments.getOrNull(i + 1)?.startMs
    }

    fun withFileDuration(index: Int, durationMs: Long) =
        BookTimeline(files.mapIndexed { i, f -> if (i == index) f.copy(durationMs = durationMs) else f })

    companion object {
        const val RestartThresholdMs = 3_000L
    }
}
