package codelab.lector.ui.theme

import androidx.compose.ui.graphics.Color
import codelab.lector.data.settings.AppearanceRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import kotlin.math.abs

class ThemeTest {
    private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)

    @Test
    fun modeAppliesWhenScheduleIsOff() {
        val off = ThemeSchedule(enabled = false)
        assertTrue(isDarkNow(ThemeMode.DARK, off, t(12), systemDark = false))
        assertFalse(isDarkNow(ThemeMode.LIGHT, off, t(23), systemDark = true))
        assertTrue(isDarkNow(ThemeMode.SYSTEM, off, t(12), systemDark = true))
    }

    @Test
    fun scheduleWithinOneDay() {
        val s = ThemeSchedule(enabled = true, lightAt = t(8), darkAt = t(21))
        assertTrue(isDarkNow(ThemeMode.LIGHT, s, t(7, 59), false))
        assertFalse(isDarkNow(ThemeMode.DARK, s, t(8), true))
        assertFalse(isDarkNow(ThemeMode.DARK, s, t(20, 59), true))
        assertTrue(isDarkNow(ThemeMode.LIGHT, s, t(21), false))
    }

    @Test
    fun scheduleAcrossMidnight() {
        val s = ThemeSchedule(enabled = true, lightAt = t(22), darkAt = t(6))
        assertFalse(isDarkNow(ThemeMode.DARK, s, t(23), true))
        assertFalse(isDarkNow(ThemeMode.DARK, s, t(2), true))
        assertTrue(isDarkNow(ThemeMode.LIGHT, s, t(12), false))
    }

    @Test
    fun presetsHaveEnoughContrast() {
        AccentPreset.entries.forEach {
            assertTrue("${it.name} dark", contrast(it.dark, DarkBackground) >= MinAccentContrast)
            assertTrue("${it.name} light", contrast(it.light, LightBackground) >= MinAccentContrast)
        }
    }

    @Test
    fun lightVariantKeepsHueAndReachesContrast() {
        listOf(Color(0xFF8E6BD8), Color(0xFFF2D24B), Color(0xFF7FB8E0), Color(0xFF40C8C8)).forEach { dark ->
            val light = lightVariant(dark)
            assertTrue(contrast(light, LightBackground) >= MinAccentContrast)
            assertTrue(abs(light.hsl()[0] - dark.hsl()[0]) < 2f)
            assertTrue(light.hsl()[2] <= dark.hsl()[2])
        }
    }

    @Test
    fun lowContrastIsDetectedAndAdjusted() {
        val dim = Color(0xFF3A2A6A)
        assertTrue(hasLowContrast(dim))
        val fixed = adjustForDark(dim)
        assertFalse(hasLowContrast(fixed))
        assertFalse(hasLowContrast(AccentPreset.AMBER.dark))
    }

    @Test
    fun accentRoundTripsThroughSettings() {
        listOf(
            AccentChoice.Preset(AccentPreset.CORAL),
            AccentChoice.System,
            AccentChoice.Custom(Color(0xFF8E6BD8)),
        ).forEach { assertEquals(it, AppearanceRepository.decodeAccent(AppearanceRepository.encodeAccent(it))) }
        assertEquals(AccentChoice.Preset(AccentPreset.AMBER), AppearanceRepository.decodeAccent("basura"))
    }
}
