package codelab.lector.playback

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import codelab.lector.R

/** Acciones que se pueden asignar a un botón, en el orden de la hoja (design.md › Decisiones de diseño). */
val AssignableActions = listOf(
    PlayerAction.SKIP_BACK,
    PlayerAction.SKIP_FORWARD,
    PlayerAction.PREVIOUS,
    PlayerAction.NEXT,
    PlayerAction.PLAY_PAUSE,
    PlayerAction.ADD_BOOKMARK,
    PlayerAction.PREVIOUS_BOOKMARK,
    PlayerAction.UNDO_JUMP,
    PlayerAction.NONE,
)

val PlayerAction.isSkip get() = this == PlayerAction.SKIP_BACK || this == PlayerAction.SKIP_FORWARD

/** Nombre de la acción en la hoja de Ajustes › Botones. */
@StringRes
fun PlayerAction.nameRes(): Int = when (this) {
    PlayerAction.SKIP_BACK -> R.string.action_name_skip_back
    PlayerAction.SKIP_FORWARD -> R.string.action_name_skip_forward
    PlayerAction.PREVIOUS -> R.string.action_name_previous
    PlayerAction.NEXT -> R.string.action_name_next
    PlayerAction.PLAY_PAUSE -> R.string.action_name_play_pause
    PlayerAction.ADD_BOOKMARK -> R.string.add_bookmark
    PlayerAction.PREVIOUS_BOOKMARK -> R.string.action_name_previous_bookmark
    PlayerAction.UNDO_JUMP -> R.string.action_name_undo_jump
    PlayerAction.NEXT_BOOK -> R.string.next_book
    PlayerAction.NONE -> R.string.action_name_none
}

/** Icono de la acción; los saltos se dibujan con su número y play / pausa según el estado. */
@DrawableRes
fun PlayerAction.iconRes(): Int? = when (this) {
    PlayerAction.PREVIOUS -> R.drawable.ic_skip_previous
    PlayerAction.NEXT -> R.drawable.ic_skip_next
    PlayerAction.PLAY_PAUSE -> R.drawable.ic_play
    PlayerAction.ADD_BOOKMARK -> R.drawable.ic_bookmark
    PlayerAction.PREVIOUS_BOOKMARK -> R.drawable.ic_bookmark_previous
    PlayerAction.UNDO_JUMP -> R.drawable.ic_undo
    PlayerAction.NEXT_BOOK -> R.drawable.ic_next_book
    PlayerAction.SKIP_BACK, PlayerAction.SKIP_FORWARD, PlayerAction.NONE -> null
}

/** Texto corto de un salto: "−10" / "+10". */
fun ActionCall.skipText(): String = if (action == PlayerAction.SKIP_BACK) "−$seconds" else "+$seconds"
