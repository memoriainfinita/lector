package codelab.lector.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class SleepWindowTest {
    private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)

    @Test
    fun windowAcrossMidnight() {
        assertTrue(inWindow(t(23), t(23), t(7)))
        assertTrue(inWindow(t(2), t(23), t(7)))
        assertFalse(inWindow(t(7), t(23), t(7)))
        assertFalse(inWindow(t(12), t(23), t(7)))
    }

    @Test
    fun windowWithinDay() {
        assertTrue(inWindow(t(14), t(13), t(16)))
        assertFalse(inWindow(t(16), t(13), t(16)))
        assertFalse(inWindow(t(9), t(13), t(16)))
    }
}
