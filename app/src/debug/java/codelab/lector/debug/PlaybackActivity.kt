package codelab.lector.debug

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.container
import codelab.lector.data.settings.InterruptedPlayback
import codelab.lector.playback.ActionCall
import codelab.lector.playback.NowPlaying
import codelab.lector.playback.PlaybackError
import codelab.lector.playback.PlayerAction
import codelab.lector.ui.components.ListDivider
import codelab.lector.ui.components.OutlineButton
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.SectionHeader
import codelab.lector.ui.theme.AccentPreset
import codelab.lector.ui.theme.LectorTheme

/**
 * Solo depuración: abre libros de la biblioteca y prueba el motor de reproducción.
 * Por adb (el móvil no admite toques inyectados):
 * `am start -n codelab.lector/.debug.PlaybackActivity --es open <bookId> | --es action <PlayerAction> | --el jump <ms>`
 */
class PlaybackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LectorTheme(dark = true, accent = AccentPreset.AMBER.dark) { PlaybackScreen() } }
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        val playback = container.playback
        intent.getStringExtra("open")?.let { playback.open(it, play = intent.getBooleanExtra("play", true)) }
        intent.getStringExtra("action")?.let { name ->
            runCatching { PlayerAction.valueOf(name) }.getOrNull()?.let {
                playback.act(ActionCall(it, intent.getIntExtra("seconds", 0)))
            }
        }
        if (intent.hasExtra("jump")) playback.jumpTo(intent.getLongExtra("jump", 0))
    }
}

@Composable
private fun PlaybackScreen() {
    val app = LocalContext.current.container
    val playback = app.playback
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val now by playback.state.collectAsStateWithLifecycle()
    val error by playback.error.collectAsStateWithLifecycle()
    val books by app.database.books().observeAll().collectAsStateWithLifecycle(emptyList())
    var interrupted by remember { mutableStateOf<InterruptedPlayback?>(null) }

    LaunchedEffect(Unit) {
        interrupted = playback.takeInterrupted()
        playback.connect()
    }

    LazyColumn(Modifier.fillMaxSize().background(c.background).safeDrawingPadding()) {
        item {
            Text("Reproducción", style = t.tabTitle, color = c.text, modifier = Modifier.padding(20.dp))
            interrupted?.let { i ->
                Text(
                    "Cerrada por el sistema mientras sonaba: ${books.firstOrNull { it.id == i.bookId }?.title ?: i.bookId}",
                    style = t.secondary, color = c.accent, modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            error?.let { e ->
                val text = when (e) {
                    is PlaybackError.Inaccessible -> "Libro inaccesible: ${books.firstOrNull { it.id == e.bookId }?.title ?: e.bookId}"
                    is PlaybackError.Failed -> "Error: ${e.message}"
                }
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text, style = t.secondary, color = c.danger, modifier = Modifier.weight(1f))
                    OutlineButton("Cerrar", { playback.clearError() })
                }
            }
            now?.let { Player(it) } ?: Text("Nada cargado", style = t.meta, color = c.textSecondary, modifier = Modifier.padding(20.dp))
            SectionHeader("Libros")
        }
        items(books, key = { it.id }) { b ->
            Row(
                Modifier.fillMaxWidth().clickable { playback.open(b.id) }.padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(b.title, style = t.row, color = if (b.inaccessible) c.textTertiary else c.text)
                    val pos = b.positionFile?.let { "${it.substringAfterLast('/')} @ ${hms(b.positionMs)}" } ?: "sin empezar"
                    Text("${hms(b.totalDurationMs)} · $pos${if (b.finished) " · terminado" else ""}${if (b.inaccessible) " · inaccesible" else ""}", style = t.meta, color = c.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun Player(now: NowPlaying) {
    val playback = LocalContext.current.container.playback
    val c = LectorTheme.colors
    val t = LectorTheme.type
    var dragging by remember { mutableStateOf<Float?>(null) }

    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            now.coverPath?.let { Cover(it) }
            Column {
                Text(now.title, style = t.row, color = c.text)
                now.author?.let { Text(it, style = t.secondary, color = c.textSecondary) }
                Text("x${now.speed} · ${if (now.isPlaying) "sonando" else "parado"}", style = t.meta, color = c.textSecondary)
            }
        }
        Text("Libro ${hms(now.positionMs)} / ${hms(now.durationMs)} · archivo ${now.fileIndex + 1}/${now.fileCount}", style = t.meta, color = c.text)
        Slider(
            value = dragging ?: (if (now.durationMs > 0) now.positionMs.toFloat() / now.durationMs else 0f),
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { playback.jumpTo((it * now.durationMs).toLong()) }
                dragging = null
            },
        )
        val kind = if (now.hasChapters) "Capítulo" else "Archivo"
        Text(
            "$kind ${now.segmentIndex + 1}/${now.segmentCount}: ${now.segmentTitle}\n" +
                "${hms(now.positionMs - now.segmentStartMs)} / ${hms(now.segmentEndMs - now.segmentStartMs)}",
            style = t.meta, color = c.textSecondary,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineButton("Anterior", { playback.act(PlayerAction.PREVIOUS) })
            OutlineButton("−10", { playback.act(PlayerAction.SKIP_BACK) })
            PrimaryButton(if (now.isPlaying) "Pausa" else "Play", { playback.act(PlayerAction.PLAY_PAUSE) })
            OutlineButton("+10", { playback.act(PlayerAction.SKIP_FORWARD) })
            OutlineButton("Siguiente", { playback.act(PlayerAction.NEXT) })
            OutlineButton("Marcar", { playback.act(PlayerAction.ADD_BOOKMARK) })
            OutlineButton("Marcador anterior", { playback.act(PlayerAction.PREVIOUS_BOOKMARK) })
            OutlineButton("Siguiente libro", { playback.act(PlayerAction.NEXT_BOOK) })
            val undoLeft = now.undoUntil?.let { it - System.currentTimeMillis() }
            if (undoLeft != null && undoLeft > 0) OutlineButton("Deshacer salto", { playback.act(PlayerAction.UNDO_JUMP) })
        }
        ListDivider()
    }
}

@Composable
private fun Cover(path: String) {
    val bitmap = remember(path) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    Box(Modifier.size(72.dp).clip(RoundedCornerShape(4.dp)).background(LectorTheme.colors.surface)) {
        if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

private fun hms(ms: Long): String {
    val s = ms.coerceAtLeast(0) / 1000
    return "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
}
