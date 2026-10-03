package codelab.lector.library

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.metadata.Chapter
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.inspector.MetadataRetriever
import codelab.lector.data.db.ChapterInfo
import codelab.lector.data.db.ChapterList
import codelab.lector.data.db.FileMeta
import kotlinx.coroutines.guava.await
import java.io.File

interface MetadataReader {
    suspend fun read(file: File): FileMeta
    suspend fun artwork(file: File): ByteArray?
}

/** Lee duración, etiquetas y capítulos con Media3, sin reproducir. */
@UnstableApi
class Media3MetadataReader(private val context: Context) : MetadataReader {

    /** Duración también en mp3 de tasa constante sin cabecera Xing/VBRI. */
    private val sources = DefaultMediaSourceFactory(context, DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true))

    private suspend fun <T> retrieve(file: File, block: suspend (MetadataRetriever) -> T): T =
        MetadataRetriever.Builder(context, MediaItem.fromUri(Uri.fromFile(file)))
            .setMediaSourceFactory(sources)
            .build()
            .use { block(it) }

    private suspend fun MetadataRetriever.metadata(): List<Metadata> {
        val groups = retrieveTrackGroups().await()
        return (0 until groups.length).flatMap { i ->
            val group = groups[i]
            (0 until group.length).mapNotNull { group.getFormat(it).metadata }
        }
    }

    override suspend fun read(file: File): FileMeta = retrieve(file) { r ->
        val metadata = r.metadata()
        val durationUs = r.retrieveDurationUs().await()
        val tags = MediaMetadata.Builder().populateFromMetadata(metadata).build()
        val entries = metadata.flatMap { m -> (0 until m.length()).map { m.get(it) } }
        FileMeta(
            path = file.path,
            sizeBytes = file.length(),
            modifiedAt = file.lastModified(),
            durationMs = if (durationUs == C.TIME_UNSET) 0 else durationUs / 1000,
            title = tags.title?.toString(),
            album = tags.albumTitle?.toString(),
            artist = tags.artist?.toString(),
            albumArtist = tags.albumArtist?.toString(),
            composer = tags.composer?.toString(),
            series = custom(entries, "SERIES", "MVNM"),
            seriesPart = custom(entries, "SERIES-PART", "SERIES_PART", "MVIN"),
            hasArtwork = tags.artworkData != null,
            chapters = ChapterList(
                entries.filterIsInstance<Chapter>()
                    .filterNot { it.isHidden }
                    .map { ChapterInfo(it.startTimeMs, it.endTimeMs, it.title?.value.orEmpty()) }
                    .sortedBy { it.startMs },
            ),
        )
    }

    override suspend fun artwork(file: File): ByteArray? = retrieve(file) { r ->
        MediaMetadata.Builder().populateFromMetadata(r.metadata()).build().artworkData
    }

    /** Serie y número: TXXX / MVNM / MVIN en ID3, campos "----" en MP4. */
    private fun custom(entries: List<Metadata.Entry>, vararg names: String): String? =
        entries.firstNotNullOfOrNull { e ->
            when {
                e is TextInformationFrame && e.id == "TXXX" && names.any { it.equals(e.description, true) } -> e.values.firstOrNull()
                e is TextInformationFrame && e.id in names -> e.values.firstOrNull()
                e is InternalFrame && names.any { it.equals(e.description, true) } -> e.text
                else -> null
            }?.takeIf(String::isNotBlank)
        }
}
