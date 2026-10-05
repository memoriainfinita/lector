package codelab.lector.playback

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Lo que ve la sesión (notificación, pantalla de bloqueo, Bluetooth, coche): anterior / siguiente
 * pasan por los tramos del libro, los saltos por los segundos de los ajustes, y reanudar repite el tramo.
 */
@OptIn(UnstableApi::class)
class LectorPlayer(player: Player, private val engine: BookEngine) : ForwardingSimpleBasePlayer(player) {

    override fun getState(): State {
        val state = super.getState()
        if (!engine.loaded) return state
        val commands = state.availableCommands.buildUpon()
            .addAll(
                COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_NEXT,
                COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                COMMAND_SEEK_BACK, COMMAND_SEEK_FORWARD,
            )
            .build()
        return state.buildUpon().setAvailableCommands(commands).build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) engine.beforePlay()
        return super.handleSetPlayWhenReady(playWhenReady)
    }

    /**
     * Los archivos los pone el motor. Retomar con la app cerrada (`onPlaybackResumption`) devuelve
     * los del libro ya abierto y Media3 los vuelve a poner: se ignoran para no reiniciar el reproductor.
     */
    override fun handleSetMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> =
        if (engine.loaded) Futures.immediateVoidFuture() else super.handleSetMediaItems(mediaItems, startIndex, startPositionMs)

    /** Velocidad desde la app, la notificación o el coche: se guarda en el libro. */
    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
        engine.setSpeed(playbackParameters.speed)
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        val prefs = engine.currentPrefs
        when (seekCommand) {
            COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> engine.previous()
            COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> engine.next()
            COMMAND_SEEK_BACK -> engine.skip(-prefs.appSkipBackSec)
            COMMAND_SEEK_FORWARD -> engine.skip(prefs.appSkipForwardSec)
            else -> {
                // Barra de la notificación u otro salto a una posición: salto grande.
                engine.recordJump()
                return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
            }
        }
        return Futures.immediateVoidFuture()
    }
}
