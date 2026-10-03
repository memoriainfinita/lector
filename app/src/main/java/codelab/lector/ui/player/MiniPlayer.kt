package codelab.lector.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codelab.lector.R
import codelab.lector.playback.NowPlaying
import codelab.lector.playback.PlayerAction
import codelab.lector.ui.components.BookCover
import codelab.lector.ui.formatDuration
import codelab.lector.ui.theme.LectorTheme

/** Alto que ocupa con su margen inferior: barra de 2, fila de 60 y 8 de margen. Las pestañas lo reservan. */
val MiniPlayerHeight = 70.dp

/**
 * Minirreproductor sobre el menú inferior: progreso del libro, portada, título, tramo y botones
 * marcar, −N, play / pausa, +N. Tocar la portada o el título abre Escuchando.
 */
@Composable
fun MiniPlayer(
    np: NowPlaying,
    skipBack: Int,
    skipForward: Int,
    onAct: (PlayerAction) -> Unit,
    onOpen: () -> Unit,
) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Column(
        Modifier
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.surface),
    ) {
        val fraction = if (np.durationMs > 0) (np.positionMs.toFloat() / np.durationMs).coerceIn(0f, 1f) else 0f
        Box(Modifier.fillMaxWidth().height(2.dp).background(c.track)) {
            Box(Modifier.fillMaxWidth(fraction).height(2.dp).background(c.accent))
        }
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.open_player), onClick = onOpen)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BookCover(np.coverPath, np.title, Modifier.size(44.dp), radius = 4.dp, titleStyle = t.label.copy(fontSize = 9.sp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(np.title, style = t.body, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val inSegment = (np.positionMs - np.segmentStartMs).coerceAtLeast(0)
                    Text(
                        "${np.segmentTitle} · ${formatDuration(inSegment)}",
                        style = t.meta,
                        color = c.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            MiniButton(44.dp, stringResource(R.string.add_bookmark), { onAct(PlayerAction.ADD_BOOKMARK) }) {
                Icon(painterResource(R.drawable.ic_bookmark), null, Modifier.size(20.dp), tint = c.accent)
            }
            MiniButton(40.dp, stringResource(R.string.skip_back_seconds, skipBack), { onAct(PlayerAction.SKIP_BACK) }) {
                Text("−$skipBack", style = t.meta.copy(fontSize = 13.sp), color = c.iconSoft)
            }
            val playing = np.playWhenReady
            MiniButton(44.dp, stringResource(if (playing) R.string.pause else R.string.play), { onAct(PlayerAction.PLAY_PAUSE) }) {
                Icon(painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play), null, Modifier.size(22.dp), tint = c.text)
            }
            MiniButton(40.dp, stringResource(R.string.skip_forward_seconds, skipForward), { onAct(PlayerAction.SKIP_FORWARD) }) {
                Text("+$skipForward", style = t.meta.copy(fontSize = 13.sp), color = c.iconSoft)
            }
        }
    }
}

@Composable
private fun MiniButton(width: Dp, description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(width = width, height = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
