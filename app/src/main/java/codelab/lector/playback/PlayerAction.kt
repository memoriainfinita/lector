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
    const val SET_SKIP_SILENCE = "codelab.lector.SET_SKIP_SILENCE"
    const val SET_OWN_SOUND = "codelab.lector.SET_OWN_SOUND"
    const val SET_BOOK_SOUND = "codelab.lector.SET_BOOK_SOUND"

    const val ARG_ACTION = "action"
    const val ARG_SECONDS = "seconds"
    const val ARG_BOOK_ID = "bookId"
    const val ARG_PLAY = "play"
    const val ARG_BOOK_MS = "bookMs"
    const val ARG_ENABLED = "enabled"
    const val ARG_PREAMP = "preampDb"
    const val ARG_EQ_ENABLED = "eqEnabled"
    const val ARG_BANDS = "bandsDb"

    val all = listOf(ACTION, OPEN_BOOK, JUMP_TO, SET_SKIP_SILENCE, SET_OWN_SOUND, SET_BOOK_SOUND).map { SessionCommand(it, Bundle.EMPTY) }

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
