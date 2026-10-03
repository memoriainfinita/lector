package codelab.lector.playback

import codelab.lector.data.db.Book
import codelab.lector.data.db.FolderRule
import codelab.lector.data.db.OnFinish
import codelab.lector.data.db.WorkUnit
import codelab.lector.library.NaturalOrder

/**
 * Deshacer salto. Solo saltos grandes (barra, capítulo o archivo, marcador). Los saltos encadenados
 * dentro de [chainMs] comparten origen: deshacer vuelve a la posición previa al primero.
 */
class JumpUndo(private val chainMs: Long = 5_000) {
    var origin: Long? = null
        private set
    var lastJumpAt: Long = 0
        private set

    fun onJump(fromBookMs: Long, now: Long) {
        if (origin == null || now - lastJumpAt > chainMs) origin = fromBookMs
        lastJumpAt = now
    }

    /** Posición a la que volver; consume el origen. */
    fun take(): Long? = origin.also { origin = null }

    fun clear() {
        origin = null
    }
}

/** Qué hacer al llegar al final de la obra, según la clase de carpeta. */
enum class FinishAction {
    /** Libros: terminado; pasa al siguiente libro si está activado. */
    FINISH_THEN_NEXT,

    /** Episodios: terminado, sin pasar a otra obra. */
    FINISH,

    /** Álbumes y Sesiones: vuelve al inicio, nunca terminado. */
    RESTART,
}

/** Sin regla, la carpeta es de clase Libros. */
fun finishActionFor(rule: FolderRule?): FinishAction = when {
    rule == null -> FinishAction.FINISH_THEN_NEXT
    rule.onFinish == OnFinish.RESTART -> FinishAction.RESTART
    rule.workUnit == WorkUnit.FILE -> FinishAction.FINISH
    else -> FinishAction.FINISH_THEN_NEXT
}

/** Ms del libro que mueve un salto de [seconds] s. Con "dividir por la velocidad", el tiempo se divide por ella. */
fun skipAmountMs(seconds: Int, speed: Float, divideBySpeed: Boolean): Long =
    if (divideBySpeed && speed > 0f) (seconds * 1000 / speed).toLong() else seconds * 1000L

/**
 * Ir al marcador anterior: el último antes de la posición. Si la posición está a menos de
 * [thresholdMs] de un marcador (se acaba de saltar a él), el anterior a ese.
 */
fun previousBookmarkTarget(bookmarkPositions: List<Long>, bookMs: Long, thresholdMs: Long = BookTimeline.RestartThresholdMs): Long? =
    bookmarkPositions.filter { it < bookMs - thresholdMs }.maxOrNull()

/** Siguiente libro: el que sigue por ruta en orden natural, saltando los inaccesibles. */
fun nextBook(current: Book, all: List<Book>): Book? =
    all.filter { !it.inaccessible && it.id != current.id }
        .sortedWith(compareBy(NaturalOrder) { it.path })
        .firstOrNull { NaturalOrder.compare(it.path, current.path) > 0 }
