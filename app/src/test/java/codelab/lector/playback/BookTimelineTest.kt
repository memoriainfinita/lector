package codelab.lector.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookTimelineTest {

    /** Tres archivos de 10 s sin capítulos. */
    private val parts = BookTimeline(listOf(TimelineFile("01.mp3", 10_000), TimelineFile("02.mp3", 10_000), TimelineFile("CD2/03.mp3", 10_000)))

    /** Un m4b de 60 s con capítulos en 0, 20 y 45 s. */
    private val m4b = BookTimeline(
        listOf(
            TimelineFile(
                "book.m4b", 60_000,
                listOf(FileChapter("Uno", 0, 20_000), FileChapter("Dos", 20_000, 45_000), FileChapter("Tres", 45_000, 0)),
            ),
        ),
    )

    @Test
    fun translatesBetweenBookAndFilePositions() {
        assertEquals(30_000, parts.totalMs)
        assertEquals(FilePosition(1, 5_000), parts.toFile(15_000))
        assertEquals(15_000, parts.toBook(FilePosition(1, 5_000)))
        // En un límite exacto, el inicio del archivo siguiente.
        assertEquals(FilePosition(1, 0), parts.toFile(10_000))
        assertEquals(FilePosition(2, 10_000), parts.toFile(30_000))
        assertEquals(FilePosition(0, 0), parts.toFile(-5))
        assertEquals(FilePosition(2, 10_000), parts.toFile(99_000))
    }

    @Test
    fun filesWithoutChaptersAreSegmentsNamedAfterTheFile() {
        assertFalse(parts.hasChapters)
        assertEquals(listOf("01", "02", "03"), parts.segments.map { it.title })
        assertEquals(20_000, parts.segments[2].startMs)
    }

    @Test
    fun chapterEndsAtNextStartAndLastEndsWithFile() {
        assertTrue(m4b.hasChapters)
        assertEquals(listOf(0L to 20_000L, 20_000L to 45_000L, 45_000L to 60_000L), m4b.segments.map { it.startMs to it.endMs })
        assertEquals(1, m4b.segmentIndexAt(30_000))
    }

    @Test
    fun previousRestartsSegmentAfterThreeSecondsOtherwiseGoesBack() {
        assertEquals(20_000, m4b.previousTarget(30_000))
        assertEquals(20_000, m4b.previousTarget(23_001))
        assertEquals(0, m4b.previousTarget(22_000))
        // En el primer tramo siempre vuelve al inicio.
        assertEquals(0, m4b.previousTarget(1_000))
        // Entre archivos.
        assertEquals(0, parts.previousTarget(11_000))
    }

    @Test
    fun nextGoesToFollowingSegmentAndStopsAtTheLast() {
        assertEquals(45_000L, m4b.nextTarget(30_000))
        assertNull(m4b.nextTarget(50_000))
        assertEquals(10_000L, parts.nextTarget(0))
    }

    @Test
    fun mixedBookUsesChaptersWhereTheyExist() {
        val mixed = BookTimeline(listOf(TimelineFile("intro.mp3", 5_000), TimelineFile("main.m4b", 20_000, listOf(FileChapter("A", 0, 0), FileChapter("B", 10_000, 0)))))
        assertEquals(listOf("intro", "A", "B"), mixed.segments.map { it.title })
        assertEquals(listOf(0L, 5_000L, 15_000L), mixed.segments.map { it.startMs })
    }

    @Test
    fun unknownDurationIsReachableOnlyByIndexUntilCorrected() {
        val t = BookTimeline(listOf(TimelineFile("a.mp3", 10_000), TimelineFile("big.m4b", 0), TimelineFile("c.mp3", 10_000)))
        assertEquals(FilePosition(2, 0), t.toFile(10_000))
        assertEquals(10_000, t.toBook(FilePosition(1, 0)))
        val fixed = t.withFileDuration(1, 50_000)
        assertEquals(70_000, fixed.totalMs)
        assertEquals(FilePosition(1, 5_000), fixed.toFile(15_000))
    }
}
