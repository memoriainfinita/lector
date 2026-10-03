package codelab.lector.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codelab.lector.ui.theme.LectorTheme

/** Arrastre y toque sobre un eje: fracción 0..1 en cada movimiento y aviso al soltar. */
private fun Modifier.dragFraction(
    vertical: Boolean,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
): Modifier = pointerInput(vertical) {
    fun fraction(o: Offset) =
        if (vertical) 1f - (o.y / size.height).coerceIn(0f, 1f) else (o.x / size.width).coerceIn(0f, 1f)
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        onChange(fraction(down.position))
        drag(down.id) { change ->
            change.consume()
            onChange(fraction(change.position))
        }
        onFinished()
    }
}

private fun Modifier.sliderSemantics(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
) = semantics {
    progressBarRangeInfo = ProgressBarRangeInfo(value, range)
    setProgress { target ->
        onChange(target.coerceIn(range.start, range.endInclusive))
        onFinished()
        true
    }
}

/** Deslizador de hoja: pista 4, relleno de acento, botón 18. Zona táctil de 32 de alto. */
@Composable
fun LectorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: () -> Unit = {},
) {
    val c = LectorTheme.colors
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    val span = valueRange.endInclusive - valueRange.start
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .sliderSemantics(value, valueRange, { change(it) }, { finished() })
            .dragFraction(vertical = false, { change(valueRange.start + it * span) }, { finished() }),
    ) {
        val track = 4.dp.toPx()
        val thumb = 9.dp.toPx()
        val y = size.height / 2
        val inner = size.width - 2 * thumb
        val x = thumb + inner * fraction
        drawRoundRect(c.track, Offset(thumb, y - track / 2), Size(inner, track), CornerRadius(track / 2))
        drawRoundRect(c.accent, Offset(thumb, y - track / 2), Size(x - thumb, track), CornerRadius(track / 2))
        drawCircle(c.text, thumb, Offset(x, y))
    }
}

/** Banda del ecualizador: pista vertical de 4, botón 18, relleno desde el centro (0 dB). */
@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    onValueChangeFinished: () -> Unit = {},
) {
    val c = LectorTheme.colors
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    val span = valueRange.endInclusive - valueRange.start
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val zero = ((0f - valueRange.start) / span).coerceIn(0f, 1f)
    val input = if (enabled) {
        Modifier
            .sliderSemantics(value, valueRange, { change(it) }, { finished() })
            .dragFraction(vertical = true, { change(valueRange.start + it * span) }, { finished() })
    } else Modifier
    Canvas(modifier.width(32.dp).fillMaxHeight().then(input)) {
        val track = 4.dp.toPx()
        val thumb = 9.dp.toPx()
        val x = size.width / 2
        val inner = size.height - 2 * thumb
        fun yOf(f: Float) = thumb + inner * (1f - f)
        val fill = if (enabled) c.accent else c.inactive
        drawRoundRect(c.track, Offset(x - track / 2, thumb), Size(track, inner), CornerRadius(track / 2))
        val top = minOf(yOf(fraction), yOf(zero))
        val bottom = maxOf(yOf(fraction), yOf(zero))
        drawRect(fill, Offset(x - track / 2, top), Size(track, bottom - top))
        drawCircle(if (enabled) c.text else c.inactive, thumb, Offset(x, yOf(fraction)))
    }
}

/**
 * Barra de progreso que se arrastra: el reproductor la usa para el libro (con marcas de tramo)
 * y para el tramo actual. Mientras se arrastra muestra la fracción del dedo; al soltar llama a [onSeek].
 */
@Composable
fun SeekBar(
    fraction: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    thickness: Dp = 6.dp,
    color: Color = LectorTheme.colors.accent,
    marks: List<Float> = emptyList(),
    onDrag: (Float?) -> Unit = {},
) {
    val c = LectorTheme.colors
    val seek by rememberUpdatedState(onSeek)
    val dragging by rememberUpdatedState(onDrag)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val shown = (dragFraction ?: fraction).coerceIn(0f, 1f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(24.dp)
            .sliderSemantics(shown, 0f..1f, { seek(it) }, {})
            .dragFraction(
                vertical = false,
                onChange = {
                    dragFraction = it
                    dragging(it)
                },
                onFinished = {
                    dragFraction?.let { seek(it) }
                    dragFraction = null
                    dragging(null)
                },
            ),
    ) {
        val h = thickness.toPx()
        val y = size.height / 2
        val r = CornerRadius(h / 2)
        drawRoundRect(c.track, Offset(0f, y - h / 2), Size(size.width, h), r)
        drawRoundRect(color, Offset(0f, y - h / 2), Size(size.width * shown, h), r)
        val markHalf = 6.dp.toPx()
        // Con muchos tramos las marcas serían ruido: solo si caben a 4 dp de media.
        val drawMarks = marks.isNotEmpty() && size.width / marks.size >= 4.dp.toPx()
        if (drawMarks) marks.forEach { m ->
            val x = size.width * m
            drawRect(c.inactive, Offset(x, y - markHalf), Size(1.dp.toPx(), markHalf * 2))
        }
    }
}
