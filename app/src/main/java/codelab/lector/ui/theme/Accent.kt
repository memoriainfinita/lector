package codelab.lector.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/** Parejas oscuro / claro del lienzo. */
enum class AccentPreset(val dark: Color, val light: Color) {
    AMBER(Color(0xFFF58F00), Color(0xFFB86E0E)),
    BLUE(Color(0xFF7FB8E0), Color(0xFF2F6F9E)),
    GREEN(Color(0xFF9BC67A), Color(0xFF4F7F2E)),
    CORAL(Color(0xFFE08A7F), Color(0xFFB0493C)),
}

sealed interface AccentChoice {
    data class Preset(val preset: AccentPreset) : AccentChoice
    data object System : AccentChoice
    /** El color elegido es el de tema oscuro; el claro se deriva. */
    data class Custom(val dark: Color) : AccentChoice
}

/** Contraste mínimo del acento sobre el fondo (componentes gráficos, WCAG 1.4.11). */
const val MinAccentContrast = 3.0

fun contrast(a: Color, b: Color): Double {
    val la = a.luminance() + 0.05
    val lb = b.luminance() + 0.05
    return max(la, lb) / min(la, lb)
}

/** Tono (0–360), saturación y luminosidad (0–1). */
fun Color.hsl(): FloatArray {
    val mx = max(red, max(green, blue))
    val mn = min(red, min(green, blue))
    val l = (mx + mn) / 2f
    if (mx == mn) return floatArrayOf(0f, 0f, l)
    val d = mx - mn
    val s = if (l > 0.5f) d / (2f - mx - mn) else d / (mx + mn)
    val h = when (mx) {
        red -> (green - blue) / d + (if (green < blue) 6f else 0f)
        green -> (blue - red) / d + 2f
        else -> (red - green) / d + 4f
    } * 60f
    return floatArrayOf(h, s, l)
}

private fun hslColor(h: Float, s: Float, l: Float) = Color.hsl(h, s, l.coerceIn(0f, 1f))

/** Mismo tono, oscurecido hasta tener contraste suficiente sobre el fondo claro. */
fun lightVariant(dark: Color): Color {
    val (h, s, l) = dark.hsl()
    var lightness = l
    while (lightness > 0f) {
        val candidate = hslColor(h, s, lightness)
        if (contrast(candidate, LightBackground) >= MinAccentContrast) return candidate
        lightness -= 0.01f
    }
    return hslColor(h, s, 0f)
}

/** "Poco contraste": el acento oscuro no destaca lo bastante sobre el fondo oscuro. */
fun hasLowContrast(dark: Color): Boolean = contrast(dark, DarkBackground) < MinAccentContrast

/** "Ajustar": mismo tono, aclarado hasta el contraste mínimo. */
fun adjustForDark(dark: Color): Color {
    val (h, s, l) = dark.hsl()
    var lightness = l
    while (lightness < 1f) {
        val candidate = hslColor(h, s, lightness)
        if (contrast(candidate, DarkBackground) >= MinAccentContrast) return candidate
        lightness += 0.01f
    }
    return hslColor(h, s, 1f)
}
