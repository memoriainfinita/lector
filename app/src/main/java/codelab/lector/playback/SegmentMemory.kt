package codelab.lector.playback

/*
 * Posición por tramo (design.md › Reproducción): al salir de un tramo a medias se guarda dónde
 * estaba; volver a él la retoma. Escucharlo hasta el final la borra.
 */

/** Clave de un tramo: archivo y ms del archivo donde empieza. */
data class SegmentKey(val file: String, val startMs: Long)

/** Salir de un tramo en sus últimos segundos cuenta como escucharlo entero. */
const val SegmentEndMarginMs = 5_000L

fun BookTimeline.segmentKey(index: Int): SegmentKey? {
    val seg = segments.getOrNull(index) ?: return null
    val at = toFile(seg.startMs)
    return SegmentKey(files[at.index].relativePath, at.ms)
}

/** Ms del libro de una posición guardada (ms del archivo de [key]), o null si el archivo ya no está. */
fun BookTimeline.savedBookMs(key: SegmentKey, positionMs: Long): Long? =
    indexOfFile(key.file).takeIf { it >= 0 }?.let { toBook(FilePosition(it, positionMs)) }

/** Qué hacer al salir de [seg] en [bookMs]. */
enum class LeaveAction {
    /** A medias: se guarda. */
    REMEMBER,

    /** En los últimos segundos: escuchado, se borra. */
    FORGET,

    /** Al principio: se deja lo que hubiera guardado. */
    KEEP,
}

fun leaveAction(seg: Segment, bookMs: Long): LeaveAction = when {
    seg.endMs - bookMs <= SegmentEndMarginMs -> LeaveAction.FORGET
    bookMs - seg.startMs > BookTimeline.RestartThresholdMs -> LeaveAction.REMEMBER
    else -> LeaveAction.KEEP
}

/** Dónde entrar en [seg]: su posición guardada si cae dentro; si no, su inicio. */
fun entryPoint(seg: Segment, savedBookMs: Long?): Long =
    savedBookMs?.takeIf { it > seg.startMs && it < seg.endMs } ?: seg.startMs

/**
 * Anterior: null vuelve al inicio del tramo actual (más de 3 s dentro, o el primero); si no, el
 * índice del tramo anterior, en el que se entra por [entryPoint].
 */
fun BookTimeline.previousSegment(bookMs: Long, restartThresholdMs: Long = BookTimeline.RestartThresholdMs): Int? {
    val i = segmentIndexAt(bookMs)
    if (i <= 0) return null
    return if (bookMs - segments[i].startMs > restartThresholdMs) null else i - 1
}
