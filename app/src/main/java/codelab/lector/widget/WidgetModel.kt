package codelab.lector.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import codelab.lector.AppContainer
import codelab.lector.playback.ActionCall
import codelab.lector.playback.FilePosition
import codelab.lector.playback.NowPlaying
import codelab.lector.playback.bookTimeline
import codelab.lector.ui.theme.accentColor
import codelab.lector.ui.theme.isDarkNow
import kotlinx.coroutines.flow.first
import java.time.LocalTime

/** Libro del widget. Posiciones en ms: del libro y del tramo (capítulo, o archivo sin capítulos). */
data class WidgetBook(
    val bookId: String,
    val title: String,
    val author: String?,
    val coverPath: String?,
    val positionMs: Long,
    val durationMs: Long,
    val segmentTitle: String,
    val segmentPositionMs: Long,
    val segmentDurationMs: Long,
    val playing: Boolean,
)

data class WidgetModel(
    /** Sin libro: "Elige un libro". */
    val book: WidgetBook?,
    val dark: Boolean,
    val accent: Color,
    val showCovers: Boolean,
    /** Ajustes › Botones: izquierda y derecha de play. */
    val buttons: List<ActionCall>,
    /** Con más ancho, los huecos de los extremos del reproductor (anterior y siguiente por defecto). */
    val extraButtons: List<ActionCall>,
)

fun NowPlaying.toWidgetBook() = WidgetBook(
    bookId = bookId,
    title = title,
    author = author,
    coverPath = coverPath,
    positionMs = positionMs,
    durationMs = durationMs,
    segmentTitle = segmentTitle,
    segmentPositionMs = (positionMs - segmentStartMs).coerceAtLeast(0),
    segmentDurationMs = (segmentEndMs - segmentStartMs).coerceAtLeast(0),
    playing = playWhenReady,
)

/**
 * Lo que muestra el widget: el estado del servicio si está en marcha; si no (tras reiniciar el
 * móvil o cerrar la app), el último libro escuchado desde la base de datos, en pausa.
 */
suspend fun loadWidgetModel(context: Context, app: AppContainer): WidgetModel {
    val appearance = app.appearance.settings.first()
    val systemDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val dark = isDarkNow(appearance.mode, appearance.schedule, LocalTime.now(), systemDark)
    val playback = app.playbackSettings.current()
    return WidgetModel(
        book = app.nowPlaying.state.value?.toWidgetBook() ?: lastBook(app),
        dark = dark,
        accent = accentColor(appearance.accent, dark, context),
        showCovers = appearance.showCovers,
        buttons = playback.widgetButtons,
        extraButtons = listOf(playback.playerButtons.first(), playback.playerButtons.last()),
    )
}

private suspend fun lastBook(app: AppContainer): WidgetBook? {
    val id = app.playbackSettings.lastBookId() ?: return null
    val book = app.database.books().get(id)?.takeIf { !it.removed } ?: return null
    val timeline = app.database.bookTimeline(id)
    val position = book.positionFile?.let(timeline::indexOfFile)?.takeIf { it >= 0 }
        ?.let { timeline.toBook(FilePosition(it, book.positionMs)) } ?: 0L
    val segment = timeline.segments.getOrNull(timeline.segmentIndexAt(position))
    return WidgetBook(
        bookId = book.id,
        title = book.customName ?: book.title,
        author = book.author,
        coverPath = app.covers.file(book.id).takeIf { it.exists() }?.path,
        positionMs = position,
        durationMs = timeline.totalMs,
        segmentTitle = segment?.title.orEmpty(),
        segmentPositionMs = segment?.let { (position - it.startMs).coerceAtLeast(0) } ?: 0,
        segmentDurationMs = segment?.let { it.endMs - it.startMs } ?: 0,
        playing = false,
    )
}
