package codelab.lector.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.content.IntentCompat
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
import androidx.media3.session.MediaButtonReceiver
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import codelab.lector.MainActivity
import codelab.lector.R
import codelab.lector.container
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
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
    private lateinit var remote: RemoteButtons
    private lateinit var sleep: SleepTimer
    /** Último libro cargado al arrancar (o nada): lo esperan los botones remotos y retomar. */
    private val restored = CompletableDeferred<Unit>()
    /** Pausa por desconectar el auricular, para reanudar si vuelve (Ajustes › Auricular). */
    private var unpluggedAt = 0L
    /** Biblioteca para el coche (design.md › Pantallas › Android Auto). */
    private lateinit var car: CarLibrary

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
        sleep = SleepTimer(this, exo, engine, app.sleep, scope)
        car = CarLibrary(this, app)
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).setAction(MainActivity.ACTION_OPEN_PLAYER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaLibrarySession.Builder(this, LectorPlayer(exo, engine), Callback())
            .setSessionActivity(openApp)
            .setMediaButtonPreferences(notificationButtons(engine.currentPrefs.notificationButtons))
            .build()

        scope.launch {
            app.playbackSettings.settings.map { it.notificationButtons }.distinctUntilChanged().collect { buttons ->
                session?.setMediaButtonPreferences(notificationButtons(buttons))
            }
        }
        scope.launch {
            app.playbackSettings.settings.map { it.pauseOnUnplug }.distinctUntilChanged().collect(exo::setHandleAudioBecomingNoisy)
        }
        scope.launch {
            app.playbackSettings.settings.map { it.remoteWhenClosed }.distinctUntilChanged().collect {
                setMediaButtonReceiverEnabled(this@PlaybackService, it)
            }
        }
        // El coche vuelve a pedir las pestañas cuando cambia lo que muestran.
        scope.launch {
            car.changes.drop(1).collect {
                car.tabIds.forEach { id -> session?.notifyChildrenChanged(id, car.children(id)?.size ?: 0, null) }
            }
        }
        // Unir, separar o cambiar la clase de carpeta pueden borrar el libro cargado: se pasa al nuevo.
        scope.launch {
            app.scanner.state.map { it.finishedAt }.filterNotNull().distinctUntilChanged().collect { engine.followRegroup() }
        }
        // Al arrancar, el último libro queda cargado y en pausa; uno quitado de la biblioteca, no.
        scope.launch {
            try {
                if (!engine.loaded) {
                    app.playbackSettings.lastBookId()
                        ?.takeIf { app.database.books().get(it)?.removed == false }
                        ?.let { engine.open(it, play = false) }
                }
            } finally {
                restored.complete(Unit)
            }
        }

        remote = RemoteButtons(scope, { engine.currentPrefs }, app.remoteKeys) { key, call, code ->
            scope.launch {
                restored.await()
                when {
                    // Play, Pausa y Stop con "play / pausa": cada tecla hace lo que dice.
                    key == RemoteKey.PLAY && call.action == PlayerAction.PLAY_PAUSE && code == KeyEvent.KEYCODE_MEDIA_PLAY -> engine.play()
                    key == RemoteKey.PLAY && call.action == PlayerAction.PLAY_PAUSE -> exo.pause()
                    else -> engine.perform(call)
                }
            }
        }

        exo.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                unpluggedAt = if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY) SystemClock.elapsedRealtime() else 0L
            }
        })
        getSystemService(AudioManager::class.java).registerAudioDeviceCallback(headsets, Handler(mainLooper))
    }

    /** Reanudar al reconectar: un auricular que vuelve antes de 10 s tras la pausa por desconectarlo. */
    private val headsets = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            if (unpluggedAt == 0L || addedDevices.none { it.isSink && it.type in HeadsetTypes }) return
            val recent = SystemClock.elapsedRealtime() - unpluggedAt < ReplugWindowMs
            unpluggedAt = 0L
            if (recent && engine.currentPrefs.resumeOnReplug && !exo.playWhenReady) engine.play()
        }
    }

    /**
     * Notificación (design.md › Reproducción): los 4 huecos de Ajustes › Botones. El 2 y el 3 van
     * junto a play; el 1 y el 4, en los huecos extra (HyperOS los pone en los extremos). Por defecto,
     * capítulo anterior, −N, +N, capítulo siguiente: junto a play, los saltos cortos, que un toque
     * sin querer corrige con otro. Las órdenes anterior y siguiente del reproductor siguen
     * disponibles para auricular, coche y teclas multimedia. "Nada" deja el hueco vacío.
     */
    private fun notificationButtons(slots: List<ActionCall>): ImmutableList<CommandButton> {
        val placed = listOf(1 to CommandButton.SLOT_BACK, 2 to CommandButton.SLOT_FORWARD, 0 to CommandButton.SLOT_OVERFLOW, 3 to CommandButton.SLOT_OVERFLOW)
        return ImmutableList.copyOf(placed.mapNotNull { (i, slot) -> slots.getOrNull(i)?.let { commandButton(it, slot) } })
    }

    private fun commandButton(call: ActionCall, slot: Int): CommandButton? {
        val icon = when (call.action) {
            PlayerAction.SKIP_BACK -> skipBackIcon(call.seconds)
            PlayerAction.SKIP_FORWARD -> skipForwardIcon(call.seconds)
            PlayerAction.PREVIOUS -> CommandButton.ICON_PREVIOUS
            PlayerAction.NEXT -> CommandButton.ICON_NEXT
            PlayerAction.PLAY_PAUSE -> CommandButton.ICON_PLAY
            PlayerAction.ADD_BOOKMARK -> CommandButton.ICON_BOOKMARK_UNFILLED
            PlayerAction.UNDO_JUMP, PlayerAction.PREVIOUS_BOOKMARK, PlayerAction.NEXT_BOOK -> CommandButton.ICON_UNDEFINED
            PlayerAction.NONE -> return null
        }
        val name = when (call.action) {
            PlayerAction.SKIP_BACK -> getString(R.string.action_skip_back, call.seconds)
            PlayerAction.SKIP_FORWARD -> getString(R.string.action_skip_forward, call.seconds)
            else -> getString(call.action.nameRes())
        }
        return CommandButton.Builder(icon)
            .apply { if (icon == CommandButton.ICON_UNDEFINED) call.action.iconRes()?.let(::setCustomIconResId) }
            .setDisplayName(name)
            .setSessionCommand(LectorCommands.action(call))
            .setSlots(slot)
            .build()
    }

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
        getSystemService(AudioManager::class.java).unregisterAudioDeviceCallback(headsets)
        remote.release()
        sleep.release()
        engine.release()
        session?.release()
        session = null
        exo.release()
        scope.cancel()
        super.onDestroy()
    }

    private inner class Callback : MediaLibrarySession.Callback {

        /** La app, los controladores de confianza (sistema, Bluetooth) y Android Auto reciben también las acciones propias. */
        override fun onConnectAsync(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<ConnectionResult> {
            if (!controller.isTrusted && controller.packageName != packageName && controller.packageName != AndroidAutoPackage) {
                return super.onConnectAsync(session, controller)
            }
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
                    // Desde el widget con el servicio parado: primero el último libro.
                    restored.await()
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
                LectorCommands.ADD_BOOKMARK -> {
                    val id = engine.addBookmark() ?: return@future SessionResult(SessionResult.RESULT_ERROR_INVALID_STATE)
                    return@future SessionResult(SessionResult.RESULT_SUCCESS, Bundle().apply { putString(LectorCommands.ARG_BOOKMARK_ID, id) })
                }
                LectorCommands.PLAY_FROM -> args.getString(LectorCommands.ARG_BOOK_ID)?.let {
                    engine.playFrom(it, args.getLong(LectorCommands.ARG_BOOK_MS))
                }
                LectorCommands.SET_SLEEP -> when (val minutes = args.getInt(LectorCommands.ARG_MINUTES)) {
                    LectorCommands.SLEEP_CHAPTER_END -> sleep.setChapterEnd()
                    0 -> sleep.cancel()
                    else -> sleep.setTimer(minutes)
                }
                else -> return@future SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
            }
            SessionResult(SessionResult.RESULT_SUCCESS)
        }

        /**
         * Botones remotos (design.md › Ajustes › C2). Los de la notificación (Android 12 y anteriores
         * los manda como teclas) siguen el camino de Media3. Media3 atribuye a la notificación también
         * las pulsaciones que arrancan el servicio con la app cerrada; las suyas llevan la sesión en
         * los datos de la intención, las del receptor no.
         */
        override fun onMediaButtonEvent(session: MediaSession, controllerInfo: MediaSession.ControllerInfo, intent: Intent): Boolean {
            if (session.isMediaNotificationController(controllerInfo) && intent.data != null) return false
            val event = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java) ?: return false
            return remote.onKey(event)
        }

        /**
         * Un botón remoto con la app cerrada arranca el servicio sin libro: el último, en su posición.
         * Media3 vuelve a poner los archivos; `LectorPlayer` lo ignora.
         */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaItemsWithStartPosition> = scope.future {
            restored.await()
            if (!engine.loaded || exo.mediaItemCount == 0) throw UnsupportedOperationException("no book to resume")
            MediaItemsWithStartPosition(
                (0 until exo.mediaItemCount).map(exo::getMediaItemAt),
                exo.currentMediaItemIndex,
                exo.currentPosition,
            )
        }

        /**
         * Tocar un libro o un marcador en el coche, o pedirlo por voz: el motor lo abre y Media3 lo
         * pone a sonar. Devuelve los archivos ya puestos, que `LectorPlayer` ignora. Los que traen
         * archivo (retomar) siguen el camino de Media3.
         */
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaItemsWithStartPosition> {
            val request = mediaItems.singleOrNull()?.takeIf { it.localConfiguration == null }
                ?: return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
            return scope.future {
                restored.await()
                val wanted = when (val target = car.target(request)) {
                    is CarLibrary.Target.Book -> target.bookId.also { engine.open(it, play = false) }
                    is CarLibrary.Target.Mark -> target.bookId.also {
                        engine.open(it, play = false)
                        if (engine.bookId == it) engine.jumpTo(target.bookMs)
                    }
                    // Por voz sin libro concreto: el cargado.
                    null -> engine.bookId
                }
                if (wanted == null || engine.bookId != wanted || exo.mediaItemCount == 0) throw UnsupportedOperationException("nothing to play")
                MediaItemsWithStartPosition(
                    (0 until exo.mediaItemCount).map(exo::getMediaItemAt),
                    exo.currentMediaItemIndex,
                    exo.currentPosition,
                )
            }
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(LibraryResult.ofItem(car.root(), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            car.children(parentId)?.let { LibraryResult.ofItemList(it, params) } ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = scope.future {
            car.item(mediaId)?.let { LibraryResult.ofItem(it, null) } ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> = scope.future {
            session.notifySearchResultChanged(browser, query, car.search(query).size, params)
            LibraryResult.ofVoid()
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            LibraryResult.ofItemList(car.search(query), params)
        }
    }
}

private const val ReplugWindowMs = 10_000L

private const val AndroidAutoPackage = "com.google.android.projection.gearhead"

private val HeadsetTypes = setOf(
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
)

/**
 * Responder con la app cerrada: el receptor de Media3 arranca el servicio con una pulsación. Apagado,
 * Android no tiene a quién entregarla con el servicio parado.
 */
fun setMediaButtonReceiverEnabled(context: Context, enabled: Boolean) {
    context.packageManager.setComponentEnabledSetting(
        ComponentName(context, MediaButtonReceiver::class.java),
        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP,
    )
}
