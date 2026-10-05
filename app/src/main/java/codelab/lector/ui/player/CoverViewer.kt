package codelab.lector.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import codelab.lector.R
import codelab.lector.container
import codelab.lector.data.db.Book
import codelab.lector.ui.components.BookCover
import codelab.lector.ui.components.IconAction
import codelab.lector.ui.theme.LectorTheme

/** Visor de portada: fondo negro, × arriba, título y autor abajo. Se cierra con × o deslizando hacia abajo. */
@Composable
fun CoverViewer(bookId: String, onClose: () -> Unit) {
    val app = LocalContext.current.container
    var book by remember { mutableStateOf<Book?>(null) }
    LaunchedEffect(bookId) { book = app.database.books().get(bookId) }
    val path = remember(bookId) { app.covers.file(bookId).takeIf { it.exists() }?.path }
    val close by rememberUpdatedState(onClose)
    var drag by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 120.dp.toPx() }
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val title = book?.let { it.customName ?: it.title }.orEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    // Cierra al soltar pasado el umbral; la imagen no se mueve (sola, parece que se despega).
                    onDragEnd = { if (drag > threshold) close() else drag = 0f },
                    onDragCancel = { drag = 0f },
                ) { _, dy -> drag = (drag + dy).coerceAtLeast(0f) }
            },
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(painterResource(R.drawable.ic_close), stringResource(R.string.close), onClose, tint = Color.White)
        }
        Box(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // Sin imagen, la portada tipográfica cuadrada.
            BookCover(
                path,
                title,
                Modifier.fillMaxWidth().then(if (path == null) Modifier.aspectRatio(1f) else Modifier),
                radius = 0.dp,
                titleStyle = t.headline,
                contentScale = ContentScale.Fit,
                author = book?.author,
            )
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = t.sheetTitle, color = Color.White)
            book?.author?.let { Text(it, style = t.secondary, color = c.textSecondary) }
        }
    }
}
