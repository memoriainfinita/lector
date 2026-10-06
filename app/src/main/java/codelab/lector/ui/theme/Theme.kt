package codelab.lector.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import codelab.lector.data.settings.AppearanceSettings
import kotlinx.coroutines.delay
import java.time.LocalTime

val LocalLectorColors = staticCompositionLocalOf { darkColors(AccentPreset.AMBER.dark) }
val LocalLectorType = staticCompositionLocalOf { LectorType() }

object LectorTheme {
    val colors: LectorColors
        @Composable @ReadOnlyComposable get() = LocalLectorColors.current
    val type: LectorType
        @Composable @ReadOnlyComposable get() = LocalLectorType.current
}

@Composable
fun LectorTheme(settings: AppearanceSettings = AppearanceSettings(), content: @Composable () -> Unit) {
    val now = rememberMinuteClock(enabled = settings.schedule.enabled)
    val dark = isDarkNow(settings.mode, settings.schedule, now, isSystemInDarkTheme())
    LectorTheme(dark = dark, accent = resolveAccent(settings.accent, dark), content = content)
}

/** Variante sin ajustes guardados, para el catálogo y las vistas previas. */
@Composable
fun LectorTheme(dark: Boolean, accent: Color, content: @Composable () -> Unit) {
    val colors = if (dark) darkColors(accent) else lightColors(accent)
    val type = remember { LectorType() }
    MaterialTheme(
        colorScheme = colors.toMaterial(),
        typography = Typography().let { t ->
            Typography(
                bodyLarge = t.bodyLarge.copy(fontFamily = PlexSans),
                bodyMedium = t.bodyMedium.copy(fontFamily = PlexSans),
                labelLarge = t.labelLarge.copy(fontFamily = PlexSans),
            )
        },
    ) {
        CompositionLocalProvider(LocalLectorColors provides colors, LocalLectorType provides type, content = content)
    }
}

@Composable
fun resolveAccent(choice: AccentChoice, dark: Boolean): Color = accentColor(choice, dark, LocalContext.current)

/** Acento fuera de Compose (widget). */
fun accentColor(choice: AccentChoice, dark: Boolean, context: Context): Color = when (choice) {
    is AccentChoice.Preset -> if (dark) choice.preset.dark else choice.preset.light
    is AccentChoice.Custom -> if (dark) choice.dark else lightVariant(choice.dark)
    AccentChoice.System -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
        } else {
            if (dark) AccentPreset.AMBER.dark else AccentPreset.AMBER.light
        }
    }
}

/** Hora actual, refrescada al cambiar de minuto solo si hace falta. */
@Composable
private fun rememberMinuteClock(enabled: Boolean): LocalTime {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(enabled) {
        while (enabled) {
            now = LocalTime.now()
            delay((60 - now.second) * 1000L)
        }
    }
    return now
}

private fun LectorColors.toMaterial() = if (isDark) {
    darkColorScheme(
        primary = accent, onPrimary = onAccent,
        background = background, onBackground = text,
        surface = surface, onSurface = text, onSurfaceVariant = textSecondary,
        surfaceContainer = surface, surfaceContainerHigh = popup, surfaceContainerLow = surface,
        outline = outline, outlineVariant = divider, error = danger, scrim = scrim,
    )
} else {
    lightColorScheme(
        primary = accent, onPrimary = onAccent,
        background = background, onBackground = text,
        surface = surface, onSurface = text, onSurfaceVariant = textSecondary,
        surfaceContainer = surface, surfaceContainerHigh = popup, surfaceContainerLow = surface,
        outline = outline, outlineVariant = divider, error = danger, scrim = scrim,
    )
}
