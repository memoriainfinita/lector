package codelab.lector.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun durationUnderAnHour() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:09", formatDuration(9_999))
        assertEquals("31:58", formatDuration(1_918_000))
    }

    @Test
    fun durationWithHours() {
        assertEquals("1:47:57", formatDuration(6_477_000))
        assertEquals("24:22:14", formatDuration(87_734_000))
    }

    @Test
    fun negativeIsZero() {
        assertEquals("0:00", formatDuration(-500))
    }

    @Test
    fun speed() {
        assertEquals("1.0x", formatSpeed(1f))
        assertEquals("1.25x", formatSpeed(1.25f))
        assertEquals("0.8x", formatSpeed(0.8f))
        assertEquals("3.5x", formatSpeed(3.5f))
    }

    @Test
    fun decibels() {
        assertEquals("0", formatDb(0f))
        assertEquals("+3", formatDb(3f))
        assertEquals("−1.5", formatDb(-1.5f))
        assertEquals("+50", formatDb(50f))
    }
}
