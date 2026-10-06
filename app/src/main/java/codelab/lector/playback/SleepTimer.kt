package codelab.lector.playback

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import codelab.lector.data.settings.PlaybackSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.sqrt

/** Estado de la pausa diferida para las pantallas. */
data class SleepState(
    val active: Boolean = false,
    /** Minutos del temporizador; null si pausa al terminar el capítulo. */
    val minutes: Int? = null,
    /** Pausa al final del tramo: elegido así, o el tiempo se cumplió con "Alargar". */
    val atChapterEnd: Boolean = false,
    /** Tiempo real hasta la pausa (con el temporizador, el que queda si sigue sonando). */
    val remainingMs: Long = 0,
    /** Arrancada por el horario automático. */
    val auto: Boolean = false,
)

/** Lo publica el temporizador del servicio; lo leen Escuchando y Ajustes. */
class SleepStateHolder {
    private val _state = MutableStateFlow(SleepState())
    val state: StateFlow<SleepState> = _state.asStateFlow()

    internal fun publish(value: SleepState) {
        _state.value = value
    }
}

/**
 * Pausa diferida (design.md › Pausa diferida). Cuenta solo mientras suena; una pausa devuelve el
 * temporizador a la duración completa. Fundido del volumen del reproductor en los últimos 10 s.
 */
class SleepTimer(
    context: Context,
    private val exo: ExoPlayer,
    private val engine: BookEngine,
    private val holder: SleepStateHolder,
    private val scope: CoroutineScope,
) : Player.Listener {

    private enum class Mode { TIMER, CHAPTER_END }

    private val prefs: PlaybackSettings get() = engine.currentPrefs
    private var mode: Mode? = null
    private var durationMs = 0L
    private var remainingMs = 0L
    private var auto = false
    /** Fin del tramo en el que hay que pausar, en ms del libro: se fija al empezar a esperarlo. */
    private var chapterEnd: Long? = null
    private var loop: Job? = null
    private var lastTick = 0L
    private var pausing = false
    private val motion = MotionResume(context)

    init {
        exo.addListener(this)
    }

    fun setTimer(minutes: Int) {
        arm(Mode.TIMER, minutes * 60_000L, auto = false)
    }

    fun setChapterEnd() {
        arm(Mode.CHAPTER_END, 0, auto = false)
    }

    fun cancel() {
        mode = null
        chapterEnd = null
        motion.stop()
        exo.volume = 1f
        update()
    }

    private fun arm(newMode: Mode, duration: Long, auto: Boolean) {
        motion.stop()
        mode = newMode
        durationMs = duration
        remainingMs = duration
        this.auto = auto
        chapterEnd = if (newMode == Mode.CHAPTER_END) engine.currentSegmentEnd() else null
        exo.volume = 1f
        update()
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) {
            motion.stop()
            if (mode == null && prefs.sleepAuto && inWindow(LocalTime.now(), prefs.sleepFrom, prefs.sleepTo)) {
                arm(Mode.TIMER, prefs.sleepAutoMinutes * 60_000L, auto = true)
            }
        } else if (!pausing && mode == Mode.TIMER) {
            // Interrumpida: vuelve a contar desde el principio, también si ya esperaba el final del capítulo.
            remainingMs = durationMs
            chapterEnd = null
            exo.volume = 1f
        }
        update()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) = update()

    /**
     * Archivo terminado sonando mientras se espera el final del tramo: pausa ya. El motor salta
     * después a la posición guardada del siguiente ("Siguiente archivo desde su posición"), y ese
     * salto no debe confundirse con uno del usuario.
     */
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (chapterEnd != null && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) fire()
    }

    /** Un salto cambia de tramo: se espera el final del nuevo. */
    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (chapterEnd != null && reason == Player.DISCONTINUITY_REASON_SEEK) chapterEnd = engine.currentSegmentEnd()
    }

    private fun update() {
        val run = mode != null && exo.isPlaying
        if (run && loop == null) {
            lastTick = SystemClock.elapsedRealtime()
            loop = scope.launch {
                while (isActive) {
                    delay(TickMs)
                    tick()
                }
            }
        } else if (!run) {
            loop?.cancel()
            loop = null
        }
        publish()
    }

    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        val dt = now - lastTick
        lastTick = now
        val left = when {
            chapterEnd != null -> toChapterEnd()
            mode == Mode.TIMER -> {
                remainingMs -= dt
                if (remainingMs <= 0 && prefs.sleepExtend) {
                    chapterEnd = engine.currentSegmentEnd()
                    toChapterEnd()
                } else {
                    remainingMs
                }
            }
            else -> return
        }
        if (left <= 0) return fire()
        // Con "Alargar", cumplir el tiempo no pausa: el fundido es el del final del capítulo.
        val willPause = !(mode == Mode.TIMER && chapterEnd == null && prefs.sleepExtend)
        exo.volume = if (willPause) (left.toFloat() / FadeMs).coerceIn(0f, 1f) else 1f
        publish()
    }

    /** Tiempo real hasta el final del tramo esperado, según la velocidad. */
    private fun toChapterEnd(): Long {
        val end = chapterEnd ?: return 0
        val speed = exo.playbackParameters.speed.takeIf { it > 0f } ?: 1f
        return ((end - engine.position()) / speed).toLong()
    }

    private fun fire() {
        val repeat = mode
        val repeatDuration = durationMs
        val wasAuto = auto
        mode = null
        chapterEnd = null
        pausing = true
        exo.pause()
        pausing = false
        exo.volume = 1f
        update()
        if (prefs.sleepMarkPause) scope.launch { engine.addPauseBookmark() }
        if (prefs.sleepMotionResume && repeat != null) {
            motion.start {
                arm(repeat, repeatDuration, wasAuto)
                engine.play()
            }
        }
    }

    private fun publish() {
        val m = mode
        holder.publish(
            if (m == null) {
                SleepState()
            } else {
                SleepState(
                    active = true,
                    minutes = if (m == Mode.TIMER) (durationMs / 60_000).toInt() else null,
                    atChapterEnd = chapterEnd != null,
                    remainingMs = if (chapterEnd != null) toChapterEnd().coerceAtLeast(0) else remainingMs.coerceAtLeast(0),
                    auto = auto,
                )
            },
        )
    }

    fun release() {
        loop?.cancel()
        motion.stop()
        exo.removeListener(this)
        holder.publish(SleepState())
    }

    private companion object {
        const val TickMs = 200L
        const val FadeMs = 10_000f
    }
}

/** Dentro de la franja [from, to); si [to] es anterior a [from], la franja cruza la medianoche. */
fun inWindow(now: LocalTime, from: LocalTime, to: LocalTime): Boolean =
    if (from <= to) now >= from && now < to else now >= from || now < to

/**
 * Seguir si muevo el móvil: 30 s tras la pausa diferida, con el acelerómetro. Un bloqueo de
 * activación parcial mantiene la CPU despierta para recibirlo con la pantalla apagada.
 */
private class MotionResume(context: Context) : SensorEventListener {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val power = context.getSystemService(PowerManager::class.java)
    private var wake: PowerManager.WakeLock? = null
    private var onMove: (() -> Unit)? = null
    private var until = 0L
    private var rest: FloatArray? = null

    fun start(onMove: () -> Unit) {
        stop()
        val accel = sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        this.onMove = onMove
        until = SystemClock.elapsedRealtime() + WindowMs
        wake = power?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "lector:sleep-motion")?.apply { acquire(WindowMs) }
        sensors.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensors?.unregisterListener(this)
        wake?.takeIf { it.isHeld }?.release()
        wake = null
        onMove = null
        rest = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (SystemClock.elapsedRealtime() > until) return stop()
        val v = event.values
        val r = rest ?: return run { rest = v.copyOf(3) }
        val dx = v[0] - r[0]
        val dy = v[1] - r[1]
        val dz = v[2] - r[2]
        if (sqrt(dx * dx + dy * dy + dz * dz) > Threshold) {
            val action = onMove
            stop()
            action?.invoke()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val WindowMs = 30_000L
        /** m/s² respecto a la primera lectura: un movimiento de la mano, no la vibración de la mesa. */
        const val Threshold = 2.5f
    }
}
