package codelab.lector.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SegmentMemoryTest {
    private val min = 60_000L

    // Dos archivos de 60 min; el segundo con capítulos en 0, 20 y 40 min.
    private val timeline = BookTimeline(
        listOf(
            TimelineFile("CD01.mp3", 60 * min),
            TimelineFile(
                "CD02.m4b",
                60 * min,
                listOf(FileChapter("A", 0, 20 * min), FileChapter("B", 20 * min, 40 * min), FileChapter("C", 40 * min, 60 * min)),
            ),
        ),
    )

    @Test
    fun keysAreFileAndStartInsideTheFile() {
        assertEquals(SegmentKey("CD01.mp3", 0), timeline.segmentKey(0))
        assertEquals(SegmentKey("CD02.m4b", 0), timeline.segmentKey(1))
        assertEquals(SegmentKey("CD02.m4b", 20 * min), timeline.segmentKey(2))
        assertNull(timeline.segmentKey(9))
        assertEquals(60 * min + 25 * min, timeline.savedBookMs(SegmentKey("CD02.m4b", 20 * min), 25 * min))
        assertNull(timeline.savedBookMs(SegmentKey("gone.mp3", 0), 1))
    }

    @Test
    fun leavingHalfwayRemembersNearTheEndForgetsAtTheStartKeeps() {
        val b = timeline.segments[2] // 80–100 min del libro
        assertEquals(LeaveAction.REMEMBER, leaveAction(b, 90 * min))
        assertEquals(LeaveAction.FORGET, leaveAction(b, 100 * min - 4_000))
        assertEquals(LeaveAction.KEEP, leaveAction(b, 80 * min + 2_000))
    }

    @Test
    fun enteringResumesTheSavedPointInsideTheSegment() {
        val b = timeline.segments[2]
        assertEquals(90 * min, entryPoint(b, 90 * min))
        assertEquals(b.startMs, entryPoint(b, null))
        // Fuera del tramo (archivo cambiado): al inicio.
        assertEquals(b.startMs, entryPoint(b, 110 * min))
    }

    @Test
    fun previousRestartsAfterThreeSecondsAndOtherwiseGoesBack() {
        assertNull(timeline.previousSegment(80 * min + 10_000))
        assertEquals(1, timeline.previousSegment(80 * min + 2_000))
        assertNull(timeline.previousSegment(2_000)) // primer tramo
    }
}
