package codelab.lector.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import codelab.lector.data.settings.InterruptedPlayback
import codelab.lector.data.settings.PlaybackSettingsRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/**
 * Conexión de las pantallas con el servicio: órdenes por la sesión (MediaController) y estado
 * desde [PlaybackStateHolder], que el motor publica en el mismo proceso.
 */
class PlaybackConnection(
    private val context: Context,
    private val holder: PlaybackStateHolder,
    private val settings: PlaybackSettingsRepository,
    private val scope: CoroutineScope,
) {
    val state: StateFlow<NowPlaying?> get() = holder.state
    val error: StateFlow<PlaybackError?> get() = holder.error

    private var controller: ListenableFuture<MediaController>? = null

    /** MediaController se crea y se usa en el hilo principal. */
    private suspend fun controller(): MediaController {
        val future = controller ?: MediaController.Builder(
            context, SessionToken(context, ComponentName(context, PlaybackService::class.java)),
        ).buildAsync().also { controller = it }
        return future.await()
    }

    /** Arranca la sesión: el servicio carga el último libro en pausa. Las pantallas lo llaman al abrirse. */
    fun connect() {
        scope.launch(Dispatchers.Main.immediate) { controller() }
    }

    fun open(bookId: String, play: Boolean = true) = send(
        LectorCommands.OPEN_BOOK,
        Bundle().apply {
            putString(LectorCommands.ARG_BOOK_ID, bookId)
            putBoolean(LectorCommands.ARG_PLAY, play)
        },
    )

    fun act(call: ActionCall) = send(LectorCommands.ACTION, LectorCommands.action(call).customExtras)

    fun act(action: PlayerAction) = act(ActionCall(action))

    /** Salto grande a una posición del libro (barra, capítulo, marcador). */
    fun jumpTo(bookMs: Long) = send(LectorCommands.JUMP_TO, Bundle().apply { putLong(LectorCommands.ARG_BOOK_MS, bookMs) })

    /** Ir a un tramo de la lista de capítulos: retoma su posición guardada. */
    fun jumpToSegment(index: Int) = send(LectorCommands.JUMP_TO_SEGMENT, Bundle().apply { putInt(LectorCommands.ARG_SEGMENT, index) })

    /** Velocidad del libro actual (0.5x–3.5x); se guarda en el libro. */
    fun setSpeed(speed: Float) {
        scope.launch(Dispatchers.Main.immediate) { controller().setPlaybackSpeed(speed) }
    }

    fun setSkipSilence(enabled: Boolean) =
        send(LectorCommands.SET_SKIP_SILENCE, Bundle().apply { putBoolean(LectorCommands.ARG_ENABLED, enabled) })

    /** Sonido propio del libro actual: activarlo copia el global. */
    fun setOwnSound(enabled: Boolean) =
        send(LectorCommands.SET_OWN_SOUND, Bundle().apply { putBoolean(LectorCommands.ARG_ENABLED, enabled) })

    /** Sonido propio del libro actual (hoja Sonido con el interruptor activado). */
    fun setBookSound(sound: SoundSettings) = send(LectorCommands.SET_BOOK_SOUND, LectorCommands.soundArgs(sound))

    /** Sonido global (Ajustes, o la hoja Sonido sin sonido propio). El motor lo aplica al cambiar. */
    val globalSound: Flow<SoundSettings> get() = settings.globalSound

    fun setGlobalSound(sound: SoundSettings) {
        scope.launch { settings.setGlobalSound(sound) }
    }

    /** Menú del libro: solo actúan si [bookId] es el libro cargado. */
    fun reset(bookId: String) = send(LectorCommands.RESET_BOOK, bookArgs(bookId))

    fun refresh(bookId: String) = send(LectorCommands.REFRESH_BOOK, bookArgs(bookId))

    fun unload(bookId: String) = send(LectorCommands.UNLOAD_BOOK, bookArgs(bookId))

    /** Pausa diferida: minutos, 0 para apagar o [LectorCommands.SLEEP_CHAPTER_END]. */
    fun setSleep(minutes: Int) = send(LectorCommands.SET_SLEEP, Bundle().apply { putInt(LectorCommands.ARG_MINUTES, minutes) })

    private fun bookArgs(bookId: String) = Bundle().apply { putString(LectorCommands.ARG_BOOK_ID, bookId) }

    fun clearError() = holder.clearError()

    /**
     * Libro que sonaba cuando el sistema cerró la app con la pantalla apagada. Solo se comprueba
     * antes de que el servicio publique estado: con el proceso vivo, la marca es de la sesión en curso.
     */
    suspend fun takeInterrupted(): InterruptedPlayback? =
        if (holder.state.value != null) null else settings.takeInterrupted()

    private fun send(action: String, args: Bundle) {
        scope.launch(Dispatchers.Main.immediate) {
            controller().sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args)
        }
    }
}
