package codelab.lector.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import codelab.lector.R
import codelab.lector.ui.theme.LectorTheme
import kotlinx.coroutines.delay

/** Aviso con "Deshacer", patrón de toda la app. Uno a la vez; el nuevo sustituye al anterior. */
@Stable
class UndoState {
    internal class Message(val text: String, val onUndo: () -> Unit)

    internal var current by mutableStateOf<Message?>(null)
        private set

    fun show(text: String, onUndo: () -> Unit) {
        current = Message(text, onUndo)
    }

    internal fun dismiss(message: Message) {
        if (current === message) current = null
    }
}

@Composable
fun rememberUndoState() = remember { UndoState() }

/** El aviso único de la app; lo pone la raíz y lo usan las pantallas. */
val LocalUndoState = staticCompositionLocalOf<UndoState> { error("UndoState sin proveer") }

const val UndoDurationMs = 5_000L

/** Se coloca sobre el menú inferior. */
@Composable
fun UndoBar(state: UndoState, modifier: Modifier = Modifier) {
    val message = state.current
    LaunchedEffect(message) {
        if (message != null) {
            delay(UndoDurationMs)
            state.dismiss(message)
        }
    }
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        val shown = message ?: return@AnimatedVisibility
        val c = LectorTheme.colors
        val shape = RoundedCornerShape(8.dp)
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .height(48.dp)
                .shadow(12.dp, shape)
                .background(c.track, shape)
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(shown.text, style = LectorTheme.type.body, color = c.text, modifier = Modifier.weight(1f))
            TextButton(
                text = stringResource(R.string.undo),
                onClick = {
                    shown.onUndo()
                    state.dismiss(shown)
                },
                color = c.accent,
                bold = true,
            )
        }
    }
}
