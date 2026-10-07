package codelab.lector.library

import codelab.lector.data.db.ChapterInfo
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** Pista de un `.cue`: título (puede faltar) e inicio en ms dentro de su archivo. */
data class CueTrack(val title: String, val startMs: Long)

/** Un FILE del `.cue`, con sus pistas en orden. */
data class CueFile(val name: String, val tracks: List<CueTrack>)

/** Texto de un `.cue`: UTF-8; si no lo es, Windows-1252, lo habitual en los antiguos. Sin BOM. */
fun decodeCue(bytes: ByteArray): String {
    val utf8 = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    val text = try {
        utf8.decode(ByteBuffer.wrap(bytes)).toString()
    } catch (_: CharacterCodingException) {
        String(bytes, Charset.forName("windows-1252"))
    }
    return text.removePrefix("﻿")
}

private val Quoted = Regex("^\"(.*)\"")
private val Index = Regex("""(\d+):(\d{1,2}):(\d{1,2})""")

/** Valor de una línea: entre comillas, o la primera palabra. */
private fun valueOf(rest: String): String = Quoted.find(rest)?.groupValues?.get(1) ?: rest.substringBefore(' ')

/**
 * FILE, TRACK, TITLE e INDEX 01 (mm:ss:ff, 75 tramas por segundo). Lo demás se ignora. Una pista sin
 * INDEX 01 no cuenta.
 */
fun parseCue(text: String): List<CueFile> {
    val files = mutableListOf<CueFile>()
    var name: String? = null
    var tracks = mutableListOf<CueTrack>()
    var title = ""
    var inTrack = false
    fun closeFile() {
        name?.let { files += CueFile(it, tracks) }
        tracks = mutableListOf()
    }
    for (raw in text.lineSequence()) {
        val line = raw.trim()
        val keyword = line.substringBefore(' ').uppercase()
        val rest = line.substringAfter(' ', "").trim()
        when (keyword) {
            "FILE" -> {
                closeFile()
                name = valueOf(rest)
                inTrack = false
            }
            "TRACK" -> {
                inTrack = true
                title = ""
            }
            "TITLE" -> if (inTrack) title = valueOf(rest)
            "INDEX" -> if (inTrack && rest.startsWith("01")) {
                Index.find(rest.removePrefix("01"))?.destructured?.let { (m, s, f) ->
                    tracks += CueTrack(title, m.toLong() * 60_000 + s.toLong() * 1_000 + f.toLong() * 1_000 / 75)
                }
            }
        }
    }
    closeFile()
    return files
}

/**
 * Capítulos de [audioName] desde los `.cue` de su carpeta ([cues]: nombre del `.cue` → su contenido):
 * el FILE con su nombre; si no lo hay, el `.cue` con su nombre base ("Libro.cue" o "Libro.mp3.cue") o el
 * único de una carpeta con un solo audio, si tiene un solo FILE (archivo renombrado). Dos pistas como mínimo;
 * cada capítulo termina donde empieza el siguiente y el último, en [durationMs].
 */
fun cueChapters(audioName: String, audioCount: Int, cues: Map<String, List<CueFile>>, durationMs: Long): List<ChapterInfo> {
    val stem = stemOf(audioName)
    val byFile = cues.values.flatten().firstOrNull { f -> f.name.substringAfterLast('/').substringAfterLast('\\').equals(audioName, ignoreCase = true) }
    val byName = cues.entries.firstOrNull { (cue, _) -> stemOf(cue).equals(stem, true) || stemOf(cue).equals(audioName, true) }?.value
    val only = cues.values.singleOrNull()?.takeIf { audioCount == 1 }
    val tracks = (byFile ?: (byName ?: only)?.singleOrNull())?.tracks.orEmpty()
        .filter { durationMs <= 0 || it.startMs < durationMs }
        .sortedBy { it.startMs }
    if (tracks.size < 2) return emptyList()
    return tracks.mapIndexed { i, t ->
        val end = tracks.getOrNull(i + 1)?.startMs ?: durationMs.takeIf { it > 0 } ?: t.startMs
        ChapterInfo(t.startMs, end, t.title)
    }
}
