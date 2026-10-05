package codelab.lector.playback

import android.os.SystemClock
import android.view.KeyEvent
import codelab.lector.data.settings.PlaybackSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/** Filas de Ajustes › Botones remotos, en el orden de [PlaybackSettings.remoteButtons]. */
enum class RemoteKey {
    HEADSET_1,
    HEADSET_2,
    HEADSET_3,
    PREVIOUS,
    NEXT,
    PLAY,
}

/**
 * Pulsaciones que llegan con Ajustes › Botones remotos abierta: la pantalla resalta su fila y no
 * se ejecuta la acción. La pantalla escucha mientras está en primer plano.
 */
class RemoteKeyMonitor {
    private var listeners = 0
    private val _keys = MutableSharedFlow<RemoteKey>(extraBufferCapacity = 8)
    val keys: SharedFlow<RemoteKey> = _keys.asSharedFlow()
    val listening: Boolean get() = listeners > 0

    fun start() {
        listeners++
    }

    fun stop() {
        listeners = (listeners - 1).coerceAtLeast(0)
    }

    fun emit(key: RemoteKey) {
        _keys.tryEmit(key)
    }
}

/**
 * Botones remotos (design.md › Ajustes › C2). Play / pausa y el botón del auricular de cable se
 * cuentan (1, 2, 3 pulsaciones); anterior, siguiente y play, pausa, stop van directos a su fila.
 * Devuelve false con las teclas que no son suyas, para el manejo por defecto de Media3.
 */
class RemoteButtons(
    private val scope: CoroutineScope,
    private val prefs: () -> PlaybackSettings,
    private val monitor: RemoteKeyMonitor,
    private val run: (RemoteKey, ActionCall, keyCode: Int) -> Unit,
) {
    private var presses = 0
    private var pending: Job? = null
    /**
     * Media3 desde 1.9.2 puede entregar la misma pulsación dos veces (androidx/media#3083): misma
     * tecla y mismo downTime en menos de 100 ms. Por tiempo, porque hay emisores con downTime 0.
     */
    private var lastCode = 0
    private var lastDownTime = -1L
    private var lastAt = 0L

    fun onKey(event: KeyEvent): Boolean {
        val code = event.keyCode
        val key = when (code) {
            KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> RemoteKey.HEADSET_1
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_REWIND -> RemoteKey.PREVIOUS
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> RemoteKey.NEXT
            KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP -> RemoteKey.PLAY
            else -> return false
        }
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return true
        val now = SystemClock.uptimeMillis()
        if (code == lastCode && event.downTime == lastDownTime && now - lastAt < DuplicateMs) return true
        lastCode = code
        lastDownTime = event.downTime
        lastAt = now
        if (key == RemoteKey.HEADSET_1) count(code) else fire(key, code)
        return true
    }

    private fun count(code: Int) {
        pending?.cancel()
        presses++
        val calls = prefs().remoteButtons
        val most = when {
            calls[RemoteKey.HEADSET_3.ordinal].action != PlayerAction.NONE -> 3
            calls[RemoteKey.HEADSET_2.ordinal].action != PlayerAction.NONE -> 2
            else -> 1
        }
        if (presses >= most) {
            fireHeadset(code)
        } else {
            pending = scope.launch {
                delay(MultiPressMs)
                fireHeadset(code)
            }
        }
    }

    private fun fireHeadset(code: Int) {
        val n = presses.coerceIn(1, 3)
        presses = 0
        pending = null
        fire(RemoteKey.entries[RemoteKey.HEADSET_1.ordinal + n - 1], code)
    }

    private fun fire(key: RemoteKey, code: Int) {
        if (monitor.listening) {
            monitor.emit(key)
            return
        }
        run(key, prefs().remoteButtons[key.ordinal], code)
    }

    fun release() {
        pending?.cancel()
    }

    private companion object {
        /** Espera por otra pulsación: más que el doble toque de Android (300 ms), menos de medio segundo. */
        const val MultiPressMs = 400L
        const val DuplicateMs = 100L
    }
}
