package codelab.lector.library

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
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

/** Sube cuando se lee algo nuevo de cada archivo: la caché anterior se vuelve a leer una vez. */
const val MetaReadVersion = 1

/** Nombre corto de un formato de audio para "formato no admitido · ALAC". */
fun codecName(mime: String): String = when (mime) {
    MimeTypes.AUDIO_ALAC -> "ALAC"
    MimeTypes.AUDIO_AC3 -> "AC-3"
    MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "E-AC-3"
    MimeTypes.AUDIO_AC4 -> "AC-4"
    MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_HD, MimeTypes.AUDIO_DTS_EXPRESS -> "DTS"
    MimeTypes.AUDIO_TRUEHD -> "TrueHD"
    else -> mime.substringAfter('/').uppercase()
}

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

    private suspend fun MetadataRetriever.formats(): List<Format> {
        val groups = retrieveTrackGroups().await()
        return (0 until groups.length).flatMap { i ->
            val group = groups[i]
            (0 until group.length).map { group.getFormat(it) }
        }
    }

    private suspend fun MetadataRetriever.metadata(): List<Metadata> = formats().mapNotNull { it.metadata }

    override suspend fun read(file: File): FileMeta = retrieve(file) { r ->
        val formats = r.formats()
        val metadata = formats.mapNotNull { it.metadata }
        val audio = formats.firstOrNull { MimeTypes.isAudio(it.sampleMimeType) }?.sampleMimeType
        // Las imágenes de capítulo de un m4b van en una pista "jpeg": no es vídeo.
        val video = formats.any { MimeTypes.isVideo(it.sampleMimeType) && it.sampleMimeType != MimeTypes.VIDEO_MJPEG }
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
            trackNumber = tags.trackNumber,
            discNumber = tags.discNumber,
            unsupported = audio?.takeUnless(::decodable)?.let(::codecName),
            hasVideo = video,
            readVersion = MetaReadVersion,
        )
    }

    /** Audio sin comprimir (wav) o con un decodificador en este móvil, como lo busca el reproductor. */
    private fun decodable(mime: String): Boolean =
        mime == MimeTypes.AUDIO_RAW || runCatching { MediaCodecUtil.getDecoderInfos(mime, false, false).isNotEmpty() }.getOrDefault(true)

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
