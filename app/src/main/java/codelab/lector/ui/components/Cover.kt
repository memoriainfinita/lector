package codelab.lector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import codelab.lector.ui.theme.LectorTheme
import coil3.compose.AsyncImage
import java.io.File

/**
 * Portada de un libro desde la caché de portadas. Sin portada: portada tipográfica, con el autor
 * arriba y el título abajo sobre un tono apagado que sale del autor (los libros de un autor
 * comparten tono) o, sin autor, del título.
 */
@Composable
fun BookCover(
    path: String?,
    title: String,
    modifier: Modifier = Modifier,
    radius: Dp = 6.dp,
    titleStyle: TextStyle = LectorTheme.type.row,
    contentScale: ContentScale = ContentScale.Crop,
    /** Autor de la portada tipográfica; solo se escribe si se pasa (no en miniaturas). */
    author: String? = null,
    /** De dónde sale el tono; por defecto, el autor o el título. */
    toneKey: String = toneKeyOf(author, title),
    /** Proporción (ancho / alto) de la imagen al cargarla, para ajustar el marco sin recortar. */
    onAspectRatio: ((Float) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(radius)
    if (path != null) {
        // Con imagen no hay fondo: las portadas con transparencia y el visor (Fit) no muestran un recuadro.
        Box(modifier.clip(shape)) {
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
        }
        return
    }
    val (background, ink) = coverTone(toneKey, LectorTheme.colors.isDark)
    Column(
        modifier.clip(shape).background(background, shape).padding(radius * 2),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (author != null && author.isNotBlank()) {
            Text(
                author.uppercase(),
                style = LectorTheme.type.label.copy(fontSize = (titleStyle.fontSize.value * 0.5f).coerceIn(9f, 13f).sp, letterSpacing = 0.1.em),
                color = ink.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Spacer(Modifier.size(0.dp))
        }
        Text(
            title,
            style = titleStyle.copy(fontWeight = FontWeight.SemiBold, lineHeight = titleStyle.fontSize * 1.12f),
            color = ink,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Clave del tono de un libro: el primer autor si lo hay ("Joseph Goldstein/Joseph Goldstein" y
 * "Joseph Goldstein" comparten tono), si no el título. Igual en todas las portadas del libro.
 */
fun toneKeyOf(author: String?, title: String): String =
    author?.split('/', ';', ',', '&')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: title

/**
 * Tono de la portada tipográfica: fondo apagado y texto del mismo tono (oscuro: fondo oscuro y
 * texto casi blanco; claro: al revés). Misma luminosidad para todos, así se leen igual.
 */
fun coverTone(key: String, dark: Boolean): Pair<Color, Color> {
    val hue = Math.floorMod(key.trim().lowercase().hashCode(), 360).toFloat()
    return if (dark) Color.hsl(hue, 0.30f, 0.28f) to Color.hsl(hue, 0.35f, 0.93f)
    else Color.hsl(hue, 0.30f, 0.82f) to Color.hsl(hue, 0.35f, 0.18f)
}
