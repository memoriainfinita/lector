package codelab.lector.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import codelab.lector.MainActivity
import codelab.lector.R
import codelab.lector.container
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch

/** Servicio de reproducción. MediaLibraryService desde el principio: lo exige Android Auto. */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var exo: ExoPlayer
    private lateinit var engine: BookEngine
    private var session: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        val app = container
        val sound = SoundProcessor()
        exo = ExoPlayer.Builder(this, SoundRenderersFactory(this, sound))
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        engine = BookEngine(exo, app.database, app.covers, app.playbackSettings, app.nowPlaying, sound, scope, app.appScope)
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).setAction(MainActivity.ACTION_OPEN_PLAYER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaLibrarySession.Builder(this, LectorPlayer(exo, engine), Callback())
            .setSessionActivity(openApp)
            .setMediaButtonPreferences(notificationButtons(engine.currentPrefs.appSkipBackSec, engine.currentPrefs.appSkipForwardSec))
            .build()

        scope.launch {
            app.playbackSettings.settings.map { it.appSkipBackSec to it.appSkipForwardSec }.distinctUntilChanged().collect { (back, forward) ->
                session?.setMediaButtonPreferences(notificationButtons(back, forward))
            }
        }
        // Al arrancar, el último libro queda cargado y en pausa; uno quitado de la biblioteca, no.
        scope.launch {
            if (!engine.loaded) {
                app.playbackSettings.lastBookId()
                    ?.takeIf { app.database.books().get(it)?.removed == false }
                    ?.let { engine.open(it, play = false) }
            }
        }
    }

    /**
     * Notificación: anterior, −N, play, +N, siguiente (design.md › Reproducción). Junto a play, los
     * saltos cortos: un toque sin querer se corrige con otro. Capítulo anterior y siguiente, como
     * botones propios en los huecos extra (HyperOS los pone en los extremos). Las órdenes anterior y
     * siguiente del reproductor siguen disponibles para auricular, coche y teclas multimedia.
     */
    private fun notificationButtons(backSec: Int, forwardSec: Int): ImmutableList<CommandButton> = ImmutableList.of(
        CommandButton.Builder(skipBackIcon(backSec))
            .setDisplayName(getString(R.string.action_skip_back, backSec))
            .setSessionCommand(LectorCommands.action(ActionCall(PlayerAction.SKIP_BACK, backSec)))
            .setSlots(CommandButton.SLOT_BACK)
            .build(),
        CommandButton.Builder(skipForwardIcon(forwardSec))
            .setDisplayName(getString(R.string.action_skip_forward, forwardSec))
            .setSessionCommand(LectorCommands.action(ActionCall(PlayerAction.SKIP_FORWARD, forwardSec)))
            .setSlots(CommandButton.SLOT_FORWARD)
            .build(),
        CommandButton.Builder(CommandButton.ICON_PREVIOUS)
            .setDisplayName(getString(R.string.previous_chapter))
            .setSessionCommand(LectorCommands.action(ActionCall(PlayerAction.PREVIOUS)))
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build(),
        CommandButton.Builder(CommandButton.ICON_NEXT)
            .setDisplayName(getString(R.string.next_chapter))
            .setSessionCommand(LectorCommands.action(ActionCall(PlayerAction.NEXT)))
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build(),
    )

    private fun skipBackIcon(seconds: Int) = when (seconds) {
        5 -> CommandButton.ICON_SKIP_BACK_5
        10 -> CommandButton.ICON_SKIP_BACK_10
        15 -> CommandButton.ICON_SKIP_BACK_15
        30 -> CommandButton.ICON_SKIP_BACK_30
        else -> CommandButton.ICON_SKIP_BACK
    }

    private fun skipForwardIcon(seconds: Int) = when (seconds) {
        5 -> CommandButton.ICON_SKIP_FORWARD_5
        10 -> CommandButton.ICON_SKIP_FORWARD_10
        15 -> CommandButton.ICON_SKIP_FORWARD_15
        30 -> CommandButton.ICON_SKIP_FORWARD_30
        else -> CommandButton.ICON_SKIP_FORWARD
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    /** Quitar la app de recientes sin reproducir cierra el servicio; reproduciendo, sigue. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!exo.playWhenReady || exo.mediaItemCount == 0) {
            engine.save()
            stopSelf()
        }
    }

    override fun onDestroy() {
        engine.release()
        session?.release()
        session = null
        exo.release()
        scope.cancel()
        super.onDestroy()
    }

    private inner class Callback : MediaLibrarySession.Callback {

        /** La app y los controladores de confianza (sistema, Bluetooth) reciben también las acciones propias. */
        override fun onConnectAsync(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<ConnectionResult> {
            if (!controller.isTrusted && controller.packageName != packageName) return super.onConnectAsync(session, controller)
            val commands = ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .apply { LectorCommands.all.forEach(::add) }
                .build()
            return Futures.immediateFuture(
                ConnectionResult.AcceptedResultBuilder(session).setAvailableSessionCommands(commands).build(),
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> = scope.future {
            when (customCommand.customAction) {
                LectorCommands.ACTION -> {
                    val call = LectorCommands.readAction(args) ?: LectorCommands.readAction(customCommand.customExtras)
                    call?.let { engine.perform(it) }
                }
                LectorCommands.OPEN_BOOK -> args.getString(LectorCommands.ARG_BOOK_ID)?.let {
                    engine.open(it, args.getBoolean(LectorCommands.ARG_PLAY))
                }
                LectorCommands.JUMP_TO -> engine.jumpTo(args.getLong(LectorCommands.ARG_BOOK_MS))
                LectorCommands.JUMP_TO_SEGMENT -> engine.jumpToSegment(args.getInt(LectorCommands.ARG_SEGMENT))
                LectorCommands.SET_SKIP_SILENCE -> engine.setSkipSilence(args.getBoolean(LectorCommands.ARG_ENABLED))
                LectorCommands.SET_OWN_SOUND -> engine.setOwnSound(args.getBoolean(LectorCommands.ARG_ENABLED))
                LectorCommands.SET_BOOK_SOUND -> engine.setBookSound(LectorCommands.readSound(args))
                LectorCommands.RESET_BOOK -> args.getString(LectorCommands.ARG_BOOK_ID)?.let(engine::reset)
                LectorCommands.REFRESH_BOOK -> args.getString(LectorCommands.ARG_BOOK_ID)?.let { engine.refresh(it) }
                LectorCommands.UNLOAD_BOOK -> args.getString(LectorCommands.ARG_BOOK_ID)?.let { engine.unload(it) }
                else -> return@future SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
            }
            SessionResult(SessionResult.RESULT_SUCCESS)
        }

        /** Raíz vacía: la navegación de la biblioteca (Android Auto) llega más adelante. */
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(
            LibraryResult.ofItem(
                MediaItem.Builder()
                    .setMediaId("root")
                    .setMediaMetadata(MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(false).build())
                    .build(),
                params,
            ),
        )
    }
}
