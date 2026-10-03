package codelab.lector.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class LectorColors(
    val isDark: Boolean,
    val background: Color,
    /** Hojas, tarjetas, selector segmentado. */
    val surface: Color,
    /** Menús emergentes y diálogos. */
    val popup: Color,
    val divider: Color,
    val dividerOnSurface: Color,
    /** Pistas de barra, elemento seleccionado. */
    val track: Color,
    val outline: Color,
    /** Marcas de la barra, estado inactivo. */
    val inactive: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val iconSoft: Color,
    val accent: Color,
    val onAccent: Color,
    val danger: Color,
    val scrim: Color,
)

val DarkBackground = Color(0xFF0A0A0A)
val LightBackground = Color(0xFFF5F3EF)

fun darkColors(accent: Color) = LectorColors(
    isDark = true,
    background = DarkBackground,
    surface = Color(0xFF161616),
    popup = Color(0xFF1F1F1F),
    divider = Color(0xFF222222),
    dividerOnSurface = Color(0xFF262626),
    track = Color(0xFF2A2A2A),
    outline = Color(0xFF3A3A3A),
    inactive = Color(0xFF5A5A5A),
    text = Color(0xFFEDEDED),
    textSecondary = Color(0xFF9A9A9A),
    textTertiary = Color(0xFF7A7A7A),
    iconSoft = Color(0xFFCFCFCF),
    accent = accent,
    onAccent = DarkBackground,
    danger = Color(0xFFE5484D),
    scrim = Color(0x80000000),
)

fun lightColors(accent: Color) = LectorColors(
    isDark = false,
    background = LightBackground,
    surface = Color(0xFFFFFFFF),
    popup = Color(0xFFFFFFFF),
    divider = Color(0xFFDDD8D0),
    dividerOnSurface = Color(0xFFDDD8D0),
    track = Color(0xFFDDD8D0),
    outline = Color(0xFFC9C3B9),
    inactive = Color(0xFFA8A29A),
    text = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF6B6B6B),
    textTertiary = Color(0xFF8A8580),
    iconSoft = Color(0xFF3A3A3A),
    accent = accent,
    onAccent = Color(0xFFFFFFFF),
    danger = Color(0xFFB3261E),
    scrim = Color(0x80000000),
)
