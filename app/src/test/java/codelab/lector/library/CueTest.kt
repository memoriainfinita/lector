package codelab.lector.library

import codelab.lector.data.db.ChapterInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CueTest {

    private val cue = """
        REM GENRE Audiobook
        PERFORMER "Autora"
        TITLE "El libro"
        FILE "El libro.mp3" MP3
          TRACK 01 AUDIO
            TITLE "Prólogo"
            INDEX 01 00:00:00
          TRACK 02 AUDIO
            TITLE "Capítulo 1"
            INDEX 00 12:29:70
            INDEX 01 12:30:37
          TRACK 03 AUDIO
            INDEX 01 75:00:00
    """.trimIndent()

    @Test
    fun parsesTracksTitlesAndFrames() {
        val file = parseCue(cue).single()
        assertEquals("El libro.mp3", file.name)
        // 37 tramas de 1/75 s = 493 ms; el INDEX 00 (pausa previa) no cuenta. Sin TITLE, vacío.
        assertEquals(
            listOf(CueTrack("Prólogo", 0), CueTrack("Capítulo 1", 750_493), CueTrack("", 4_500_000)),
            file.tracks,
        )
    }

    @Test
    fun decodesUtf8AndOldWindowsEncoding() {
        assertEquals("TITLE \"Prólogo\"", decodeCue("﻿TITLE \"Prólogo\"".toByteArray(Charsets.UTF_8)))
        assertEquals("TITLE \"Prólogo\"", decodeCue("TITLE \"Prólogo\"".toByteArray(charset("windows-1252"))))
    }

    @Test
    fun chaptersEndWhereTheNextStarts() {
        val cues = mapOf("El libro.cue" to parseCue(cue))
        assertEquals(
            listOf(ChapterInfo(0, 750_493, "Prólogo"), ChapterInfo(750_493, 4_500_000, "Capítulo 1"), ChapterInfo(4_500_000, 5_000_000, "")),
            cueChapters("El libro.mp3", 1, cues, durationMs = 5_000_000),
        )
        // Pistas que empiezan después del final del archivo: fuera.
        assertEquals(2, cueChapters("El libro.mp3", 1, cues, durationMs = 4_000_000).size)
    }

    @Test
    fun matchesByFileLineThenNameThenOnlyCue() {
        val cues = mapOf("otro nombre.cue" to parseCue(cue))
        // Por la línea FILE, aunque el .cue se llame distinto y haya más audio en la carpeta.
        assertEquals(3, cueChapters("el libro.MP3", 5, cues, 5_000_000).size)
        // Archivo renombrado: por el nombre base del .cue…
        assertEquals(3, cueChapters("Otro nombre.flac", 5, cues, 5_000_000).size)
        // …o el único .cue de una carpeta con un solo audio.
        assertEquals(3, cueChapters("Renombrado.mp3", 1, cues, 5_000_000).size)
        // Con más audio en la carpeta y sin nada que lo nombre: no es suyo.
        assertTrue(cueChapters("Renombrado.mp3", 2, cues, 5_000_000).isEmpty())
    }

    @Test
    fun severalFilesAndSingleTrackCues() {
        val two = parseCue(
            """
            FILE "CD1.flac" WAVE
              TRACK 01 AUDIO
                INDEX 01 00:00:00
              TRACK 02 AUDIO
                INDEX 01 10:00:00
            FILE "CD2.flac" WAVE
              TRACK 03 AUDIO
                INDEX 01 00:00:00
            """.trimIndent(),
        )
        val cues = mapOf("libro.cue" to two)
        assertEquals(listOf(0L, 600_000L), cueChapters("CD1.flac", 2, cues, 1_200_000).map { it.startMs })
        // Una sola pista no son capítulos.
        assertTrue(cueChapters("CD2.flac", 2, cues, 1_200_000).isEmpty())
    }
}
