package codelab.lector.widget

import android.content.Context
import android.os.SystemClock
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import codelab.lector.AppContainer
import codelab.lector.data.settings.AppearanceSettings
import codelab.lector.playback.ActionCall
import codelab.lector.playback.NowPlaying
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlin.math.abs

/**
 * Redibuja los widgets cuando cambia lo que muestran: libro, tramo, play / pausa, tema, acento o
 * botones al momento; la posición, al saltar o en pausa, y mientras suena una vez por minuto, para
 * no gastar batería. Sin widgets puestos no hace nada.
 *
 * Un widget activo no vuelve a cargar sus datos al actualizarse, solo se redibuja: los lee de
 * [frame], que se publica aquí.
 */
class WidgetUpdates(private val context: Context, private val app: AppContainer) {

    private val _frame = MutableStateFlow<WidgetFrame?>(null)
    val frame: StateFlow<WidgetFrame?> = _frame.asStateFlow()

    /** Lo que se dibujó por última vez, sin la posición. */
    private data class Shown(
        val book: WidgetBook?,
        val appearance: AppearanceSettings,
        val buttons: List<ActionCall>,
    )

    private var shown: Shown? = null
    private var shownPositionMs = 0L
    private var shownSpeed = 1f
    private var shownAt = 0L

    suspend fun run() {
        val buttons = app.playbackSettings.settings.map { it.widgetButtons + it.playerButtons }
        combine(app.nowPlaying.state, app.appearance.settings, buttons, ::Triple).conflate().collect { (np, look, calls) ->
            val book = np?.toWidgetBook()
            val next = Shown(book?.copy(positionMs = 0, segmentPositionMs = 0), look, calls)
            val now = SystemClock.elapsedRealtime()
            val due = next != shown || (np != null && positionDue(np, now))
            if (!due) return@collect
            if (GlanceAppWidgetManager(context).getGlanceIds(LectorWidget::class.java).isNotEmpty()) {
                _frame.value = loadWidgetFrame(context, app)
                LectorWidget().updateAll(context)
            } else {
                // Sin widgets no se sigue el estado: uno nuevo carga el suyo.
                _frame.value = null
            }
            shown = next
            shownPositionMs = book?.positionMs ?: 0
            shownSpeed = np?.speed ?: 1f
            shownAt = now
        }
    }

    /**
     * Parado (en pausa o cargando), cualquier cambio de posición; sonando, un salto o un minuto
     * desde la última vez.
     */
    private fun positionDue(np: NowPlaying, now: Long): Boolean {
        if (!np.isPlaying) return abs(np.positionMs - shownPositionMs) >= 1_000
        if (now - shownAt >= PlayingIntervalMs) return true
        val expected = shownPositionMs + ((now - shownAt) * shownSpeed).toLong()
        return abs(np.positionMs - expected) > JumpToleranceMs || np.speed != shownSpeed
    }

    private companion object {
        const val PlayingIntervalMs = 60_000L
        const val JumpToleranceMs = 3_000L
    }
}
