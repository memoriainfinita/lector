package codelab.lector.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFF58F00)

private val LectorColors = darkColorScheme(
    primary = Accent,
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF0A0A0A),
)

@Composable
fun LectorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LectorColors, content = content)
}
