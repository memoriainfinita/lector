package codelab.lector.ui.theme

import java.time.LocalTime

enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** Cambiar de tema por hora: claro desde [lightAt], oscuro desde [darkAt]. */
data class ThemeSchedule(
    val enabled: Boolean = false,
    val lightAt: LocalTime = LocalTime.of(8, 0),
    val darkAt: LocalTime = LocalTime.of(21, 0),
)

fun isDarkNow(mode: ThemeMode, schedule: ThemeSchedule, now: LocalTime, systemDark: Boolean): Boolean {
    if (schedule.enabled && schedule.lightAt != schedule.darkAt) {
        val light = if (schedule.lightAt < schedule.darkAt) {
            now >= schedule.lightAt && now < schedule.darkAt
        } else {
            now >= schedule.lightAt || now < schedule.darkAt
        }
        return !light
    }
    return when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
}
