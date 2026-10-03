package codelab.lector.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado del reproductor para las pantallas y los widgets. Posiciones en ms del libro salvo indicación. */
data class NowPlaying(
    val bookId: String,
    val title: String,
    val author: String?,
    val narrator: String?,
    val coverPath: String?,
    val positionMs: Long,
    val durationMs: Long,
    val fileIndex: Int,
    val fileCount: Int,
    /** Tramo actual: capítulo si el libro tiene capítulos, si no el archivo. */
    val segmentTitle: String,
    val segmentIndex: Int,
    val segmentCount: Int,
    val segmentStartMs: Long,
    val segmentEndMs: Long,
    val hasChapters: Boolean,
    /** Todos los tramos del libro: marcas de la barra y lista de capítulos. */
    val segments: List<Segment>,
    val isPlaying: Boolean,
    /** Sonará en cuanto pueda (el botón de play no parpadea mientras carga). */
    val playWhenReady: Boolean,
    val speed: Float,
    val skipSilence: Boolean,
    /** Sonido propio del libro; si no, usa el global. */
    val ownSound: Boolean,
    /** Sonido que se está aplicando (propio o global). */
    val sound: SoundSettings,
    /** Hasta cuándo se ofrece "Deshacer" tras un salto grande (reloj del sistema), o null. */
    val undoUntil: Long?,
    /** Posición previa al primer salto de la cadena, para "Saltado desde…". */
    val undoFromMs: Long?,
)

sealed interface PlaybackError {
    data class Inaccessible(val bookId: String) : PlaybackError
    data class Failed(val message: String) : PlaybackError
}

/** Punto único que publica el motor y leen las pantallas (design.md › Reproducción). */
class PlaybackStateHolder {
    private val _state = MutableStateFlow<NowPlaying?>(null)
    val state: StateFlow<NowPlaying?> = _state.asStateFlow()

    private val _error = MutableStateFlow<PlaybackError?>(null)
    val error: StateFlow<PlaybackError?> = _error.asStateFlow()

    internal fun publish(value: NowPlaying?) {
        _state.value = value
    }

    internal fun fail(error: PlaybackError?) {
        _error.value = error
    }

    fun clearError() {
        _error.value = null
    }
}
