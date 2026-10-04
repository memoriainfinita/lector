package codelab.lector.data.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import codelab.lector.ui.theme.AccentChoice
import codelab.lector.ui.theme.AccentPreset
import codelab.lector.ui.theme.ThemeMode
import codelab.lector.ui.theme.ThemeSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

data class AppearanceSettings(
    val mode: ThemeMode = ThemeMode.DARK,
    val schedule: ThemeSchedule = ThemeSchedule(),
    val accent: AccentChoice = AccentChoice.Preset(AccentPreset.AMBER),
    /** Ajustes › Apariencia › Mostrar portadas; sin ellas, la superficie con el título. */
    val showCovers: Boolean = true,
)

/** Tema y acento. El idioma lo guarda AppCompat (selector de idioma por app). */
class AppearanceRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<AppearanceSettings> = store.data.map { p ->
        val defaults = ThemeSchedule()
        AppearanceSettings(
            mode = p[MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            schedule = ThemeSchedule(
                enabled = p[SCHEDULE_ON] ?: false,
                lightAt = p[LIGHT_AT]?.let { LocalTime.ofSecondOfDay(it.toLong()) } ?: defaults.lightAt,
                darkAt = p[DARK_AT]?.let { LocalTime.ofSecondOfDay(it.toLong()) } ?: defaults.darkAt,
            ),
            accent = decodeAccent(p[ACCENT]),
            showCovers = p[SHOW_COVERS] ?: true,
        )
    }

    suspend fun setMode(mode: ThemeMode) = store.edit { it[MODE] = mode.name }

    suspend fun setSchedule(schedule: ThemeSchedule) = store.edit {
        it[SCHEDULE_ON] = schedule.enabled
        it[LIGHT_AT] = schedule.lightAt.toSecondOfDay()
        it[DARK_AT] = schedule.darkAt.toSecondOfDay()
    }

    suspend fun setAccent(accent: AccentChoice) = store.edit { it[ACCENT] = encodeAccent(accent) }

    suspend fun setShowCovers(show: Boolean) = store.edit { it[SHOW_COVERS] = show }

    companion object {
        private val MODE = stringPreferencesKey("theme_mode")
        private val SCHEDULE_ON = booleanPreferencesKey("theme_schedule_on")
        private val LIGHT_AT = intPreferencesKey("theme_light_at")
        private val DARK_AT = intPreferencesKey("theme_dark_at")
        private val ACCENT = stringPreferencesKey("accent")
        private val SHOW_COVERS = booleanPreferencesKey("show_covers")

        fun encodeAccent(accent: AccentChoice): String = when (accent) {
            is AccentChoice.Preset -> "preset:${accent.preset.name}"
            AccentChoice.System -> "system"
            is AccentChoice.Custom -> "custom:%08X".format(accent.dark.toArgb())
        }

        fun decodeAccent(value: String?): AccentChoice {
            val fallback = AccentChoice.Preset(AccentPreset.AMBER)
            if (value == null) return fallback
            return when {
                value == "system" -> AccentChoice.System
                value.startsWith("preset:") ->
                    runCatching { AccentChoice.Preset(AccentPreset.valueOf(value.removePrefix("preset:"))) }
                        .getOrDefault(fallback)
                value.startsWith("custom:") ->
                    value.removePrefix("custom:").toLongOrNull(16)
                        ?.let { AccentChoice.Custom(Color(it.toInt())) } ?: fallback
                else -> fallback
            }
        }
    }
}
