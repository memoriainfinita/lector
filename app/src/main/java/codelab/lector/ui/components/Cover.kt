package codelab.lector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codelab.lector.ui.theme.LectorTheme
import coil3.compose.AsyncImage
import java.io.File

/**
 * Portada de un libro desde la caché de portadas. Sin portada: superficie con el título,
 * para que la cuadrícula y el reproductor no queden vacíos.
 */
@Composable
fun BookCover(
    path: String?,
    title: String,
    modifier: Modifier = Modifier,
    radius: Dp = 6.dp,
    titleStyle: TextStyle = LectorTheme.type.row,
    contentScale: ContentScale = ContentScale.Crop,
    /** Proporción (ancho / alto) de la imagen al cargarla, para ajustar el marco sin recortar. */
    onAspectRatio: ((Float) -> Unit)? = null,
) {
    val c = LectorTheme.colors
    val shape = RoundedCornerShape(radius)
    // Con imagen no hay fondo: las portadas con transparencia y el visor (Fit) no muestran un recuadro.
    Box(
        modifier.clip(shape).then(if (path == null) Modifier.background(c.surface, shape) else Modifier),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (path != null) {
            AsyncImage(
                model = File(path),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onSuccess = { state ->
                    val size = state.painter.intrinsicSize
                    if (onAspectRatio != null && size.width > 0 && size.height > 0) onAspectRatio(size.width / size.height)
                },
            )
        } else {
            Text(
                title,
                style = titleStyle,
                color = c.textSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(radius * 2),
            )
        }
    }
}
