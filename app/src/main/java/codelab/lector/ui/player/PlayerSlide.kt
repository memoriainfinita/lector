package codelab.lector.ui.player

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Al soltar, se cierra o se abre pasado este desplazamiento (el umbral del visor de portada)... */
internal val SlideDistance = 120.dp

/** ...o con un gesto en esa dirección más rápido que esto, por cada segundo. */
internal val SlideVelocity = 400.dp

/**
 * Movimiento sin rebote para subir y bajar, como el de las hojas. Termina a 1 px: por debajo ya no
 * se ve moverse y la cola del muelle retrasaría el final.
 */
private val SlideSpec = spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 1f)

/** Vuelta arriba si no se cierra, con un rebote. */
private val SettleSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 1f)

@Composable
fun rememberPlayerSlide(onClosed: () -> Unit): PlayerSlide {
    val scope = rememberCoroutineScope()
    return remember { PlayerSlide(scope, onClosed) }
}

/**
 * Escuchando entera desplazada hacia abajo [offset] px (design.md › Escuchando): sube al abrirse,
 * baja al cerrarse y sigue al dedo desde la raya o la portada, y desde el minirreproductor hacia
 * arriba. Vive en la pantalla principal: el minirreproductor y Escuchando la comparten.
 * [onClosed] saca Escuchando de la pila cuando ha terminado de bajar.
 */
@Stable
class PlayerSlide internal constructor(private val scope: CoroutineScope, private val onClosed: () -> Unit) {
    var offset by mutableFloatStateOf(0f)
        private set

    /** Alto del área de las pantallas: lo que baja para quedar fuera. */
    var height = 0f

    /** Escuchando sigue al dedo desde el minirreproductor. */
    var fromMini by mutableStateOf(false)
        private set

    /** Abierta, quieta y arriba del todo. */
    val isOpen: Boolean get() = !fromMini && !closing && !animating && offset == 0f

    private var closing by mutableStateOf(false)
    private var animating by mutableStateOf(false)
    private var pendingOpen = false
    private var job: Job? = null
    private var runs = 0
    private var target = 0f

    private fun run(to: Float, spec: AnimationSpec<Float>, velocity: Float, then: () -> Unit = {}) {
        job?.cancel()
        animating = true
        // Una animación cancelada por otra no apaga [animating]: ya es de la nueva.
        val run = ++runs
        job = scope.launch {
            try {
                animate(offset, to, velocity, spec) { value, _ -> offset = value }
            } finally {
                if (runs == run) animating = false
            }
            then()
        }
    }

    /** Se va a abrir Escuchando: empieza abajo y sube cuando aparece ([onShown]). */
    fun prepareOpen() {
        if (fromMini) return
        job?.cancel()
        closing = false
        offset = height
        pendingOpen = true
    }

    /** Escuchando ya está en la pantalla: si se acaba de abrir, sube. */
    fun onShown() {
        if (!pendingOpen) return
        pendingOpen = false
        run(0f, SlideSpec, 0f)
    }

    fun close() = close(0f)

    private fun close(velocity: Float) {
        if (closing) return
        closing = true
        pendingOpen = false
        run(height, SlideSpec, velocity) { onClosed() }
    }

    fun startDrag() {
        job?.cancel()
        target = offset
    }

    fun drag(dy: Float) {
        if (closing) return
        target = (target + dy).coerceIn(0f, height)
        offset = target
    }

    /** Fin del arrastre hacia abajo: baja del todo o vuelve arriba con un rebote. */
    fun release(velocity: Float, distance: Float, fling: Float) {
        if (closing) return
        if (offset > distance || velocity > fling) close(velocity) else run(0f, SettleSpec, velocity)
    }

    /** El minirreproductor empieza a subir: Escuchando entra desde abajo siguiendo al dedo. */
    internal fun startFromMini(open: () -> Unit) {
        job?.cancel()
        closing = false
        pendingOpen = false
        fromMini = true
        offset = height
        target = height
        open()
    }

    /** Fin del arrastre desde el minirreproductor: termina de subir o vuelve a bajar y se cierra. */
    internal fun releaseFromMini(velocity: Float, distance: Float, fling: Float) {
        fromMini = false
        if (height - offset > distance || velocity < -fling) run(0f, SlideSpec, velocity) else close(velocity)
    }
}

/**
 * Arrastrar el minirreproductor hacia arriba abre Escuchando siguiendo al dedo. Solo hacia arriba:
 * hacia abajo no hace nada. [open] mete Escuchando en la pila.
 */
@Composable
fun Modifier.slideUpFromMini(slide: PlayerSlide, open: () -> Unit): Modifier {
    val density = LocalDensity.current
    val distance = with(density) { SlideDistance.toPx() }
    val fling = with(density) { SlideVelocity.toPx() }
    val started = remember { booleanArrayOf(false) }
    return draggable(
        state = rememberDraggableState { dy ->
            if (!started[0]) {
                if (dy >= 0f) return@rememberDraggableState
                started[0] = true
                slide.startFromMini(open)
            }
            slide.drag(dy)
        },
        orientation = Orientation.Vertical,
        onDragStarted = { started[0] = false },
        onDragStopped = { velocity ->
            if (started[0]) {
                started[0] = false
                slide.releaseFromMini(velocity, distance, fling)
            }
        },
    )
}
