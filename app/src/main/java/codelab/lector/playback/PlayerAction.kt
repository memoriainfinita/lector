package codelab.lector.playback

import android.os.Bundle
import androidx.media3.session.SessionCommand

/**
 * Acciones asignables a botones: reproductor, notificación, widget, auricular y teclas multimedia
 * usan este mismo conjunto (design.md › Arquitectura).
 */
enum class PlayerAction {
    SKIP_BACK,
    SKIP_FORWARD,
    PREVIOUS,
    NEXT,
    PLAY_PAUSE,
    ADD_BOOKMARK,
    PREVIOUS_BOOKMARK,
    UNDO_JUMP,
    NEXT_BOOK,
    NONE,
}

/** Una acción con sus segundos, para los saltos. */
data class ActionCall(val action: PlayerAction, val seconds: Int = 0)

/** Comandos propios de la sesión. Los argumentos van en los extras. */
object LectorCommands {
    const val ACTION = "codelab.lector.ACTION"
    const val OPEN_BOOK = "codelab.lector.OPEN_BOOK"
    const val JUMP_TO = "codelab.lector.JUMP_TO"
    const val JUMP_TO_SEGMENT = "codelab.lector.JUMP_TO_SEGMENT"
    const val SET_SKIP_SILENCE = "codelab.lector.SET_SKIP_SILENCE"
    const val SET_OWN_SOUND = "codelab.lector.SET_OWN_SOUND"
    const val SET_BOOK_SOUND = "codelab.lector.SET_BOOK_SOUND"
    /** Menú del libro sobre el libro cargado: reiniciar, releer (nombre, terminado) y descargar. */
    const val RESET_BOOK = "codelab.lector.RESET_BOOK"
    const val REFRESH_BOOK = "codelab.lector.REFRESH_BOOK"
    const val UNLOAD_BOOK = "codelab.lector.UNLOAD_BOOK"
    /** Pausa diferida: [ARG_MINUTES] > 0 temporizador, 0 apagar, [SLEEP_CHAPTER_END] al terminar el capítulo. */
    const val SET_SLEEP = "codelab.lector.SET_SLEEP"
    const val SLEEP_CHAPTER_END = -1
    /** Marcador en la posición actual desde la app: devuelve su id en [ARG_BOOKMARK_ID]. */
    const val ADD_BOOKMARK = "codelab.lector.ADD_BOOKMARK"
    /** "Escuchar desde aquí": [ARG_BOOK_ID] y [ARG_BOOK_MS]. */
    const val PLAY_FROM = "codelab.lector.PLAY_FROM"

    const val ARG_ACTION = "action"
    const val ARG_SECONDS = "seconds"
    const val ARG_BOOK_ID = "bookId"
    const val ARG_PLAY = "play"
    const val ARG_BOOK_MS = "bookMs"
    const val ARG_SEGMENT = "segment"
    const val ARG_ENABLED = "enabled"
    const val ARG_MINUTES = "minutes"
    const val ARG_PREAMP = "preampDb"
    const val ARG_EQ_ENABLED = "eqEnabled"
    const val ARG_BANDS = "bandsDb"
    const val ARG_BOOKMARK_ID = "bookmarkId"

    val all = listOf(ACTION, OPEN_BOOK, JUMP_TO, JUMP_TO_SEGMENT, SET_SKIP_SILENCE, SET_OWN_SOUND, SET_BOOK_SOUND, RESET_BOOK, REFRESH_BOOK, UNLOAD_BOOK, SET_SLEEP, ADD_BOOKMARK, PLAY_FROM)
        .map { SessionCommand(it, Bundle.EMPTY) }

    fun action(call: ActionCall) = SessionCommand(
        ACTION,
        Bundle().apply {
            putString(ARG_ACTION, call.action.name)
            putInt(ARG_SECONDS, call.seconds)
        },
    )

    fun soundArgs(sound: SoundSettings) = Bundle().apply {
        putFloat(ARG_PREAMP, sound.preampDb)
        putBoolean(ARG_EQ_ENABLED, sound.eqEnabled)
        putFloatArray(ARG_BANDS, sound.bandsDb.toFloatArray())
    }

    fun readSound(args: Bundle) = SoundSettings(
        preampDb = args.getFloat(ARG_PREAMP),
        eqEnabled = args.getBoolean(ARG_EQ_ENABLED),
        bandsDb = args.getFloatArray(ARG_BANDS)?.toList() ?: SoundSettings().bandsDb,
    )

    fun readAction(args: Bundle): ActionCall? {
        val name = args.getString(ARG_ACTION) ?: return null
        val action = runCatching { PlayerAction.valueOf(name) }.getOrNull() ?: return null
        return ActionCall(action, args.getInt(ARG_SECONDS))
    }
}
