package codelab.lector.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import codelab.lector.AppContainer
import codelab.lector.MainActivity
import codelab.lector.R
import codelab.lector.container
import codelab.lector.playback.ActionCall
import codelab.lector.playback.PlayerAction
import codelab.lector.playback.iconRes
import codelab.lector.playback.nameRes
import codelab.lector.ui.components.coverTone
import codelab.lector.ui.components.toneKeyOf
import codelab.lector.ui.formatDuration
import codelab.lector.ui.theme.LectorColors
import codelab.lector.ui.theme.darkColors
import codelab.lector.ui.theme.lightColors
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Widget del libro en curso (design.md › Pantallas › Widget; lienzo `Widget`). Uno solo, que se
 * adapta a su tamaño ([WidgetContent]). Junto a play, los huecos del widget de Ajustes › Botones; con
 * más ancho, los extremos del reproductor y marcar.
 *
 * Una versión por tamaño de referencia, y Android muestra la mayor que cabe en el tamaño real. No
 * `SizeMode.Exact`: da solo una versión vertical y otra horizontal, que Android elige por la
 * orientación del móvil, y con el móvil de lado y el launcher en vertical salía la horizontal.
 */
class LectorWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(ReferenceSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.container
        val initial = app.widgets.frame.value ?: loadWidgetFrame(context, app)
        provideContent {
            val frame = app.widgets.frame.collectAsState().value ?: initial
            WidgetContent(frame.model, frame.cover)
        }
    }
}

/**
 * Tamaños de referencia, en los umbrales del diseño; Android admite 16 como mucho. Anchos: caben 1,
 * 3, 5 y 6 botones (100, 164, 260, 308). Altos: una fila (40), con portada al lado (116, 220) y en
 * vertical también con los anchos mayores (400: más alto que 1,2 veces 308).
 */
private val ReferenceSizes = listOf(100, 164, 260, 308).flatMap { w ->
    listOf(40, 116, 220, 400).map { h -> DpSize(w.dp, h.dp) }
}.toSet()

class LectorWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LectorWidget()
}

/** Botón del widget: la acción va al servicio por la sesión, como las del reproductor. */
class WidgetActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val action = parameters[ActionKey]?.let { runCatching { PlayerAction.valueOf(it) }.getOrNull() } ?: return
        withTimeoutOrNull(ActionTimeoutMs) {
            context.container.playback.actAndWait(ActionCall(action, parameters[SecondsKey] ?: 0))
        }
    }
}

private val ActionKey = ActionParameters.Key<String>("action")
private val SecondsKey = ActionParameters.Key<Int>("seconds")
private const val ActionTimeoutMs = 8_000L

/** Lo que dibuja el widget: el modelo y la portada ya reducida. */
class WidgetFrame(val model: WidgetModel, val cover: Bitmap?)

suspend fun loadWidgetFrame(context: Context, app: AppContainer): WidgetFrame {
    val model = loadWidgetModel(context, app)
    return WidgetFrame(model, model.book?.coverPath?.takeIf { model.showCovers }?.let(::decodeCover))
}

/** Lado mayor de la portada en píxeles: Android limita la memoria de las imágenes de un widget. */
private const val CoverMaxPx = 640

private fun decodeCover(path: String): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= CoverMaxPx) sample *= 2
    val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val scale = CoverMaxPx.toFloat() / maxOf(bitmap.width, bitmap.height)
    val sized = if (scale >= 1f) bitmap
    else Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
    return sized.rounded()
}

/**
 * Esquinas redondeadas en la propia imagen: ajustada sin recortar en un hueco mayor, el redondeo del
 * hueco no la alcanza.
 */
private fun Bitmap.rounded(): Bitmap {
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(this@rounded, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
    val radius = minOf(width, height) * CoverCornerRatio
    Canvas(out).drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint)
    return out
}

/** Como los 6 de radio de una portada de unos 170. */
private const val CoverCornerRatio = 0.035f

private fun color(c: Color) = ColorProvider(c)

/**
 * Un solo diseño que se adapta al tamaño (el de referencia; la portada vertical, al real):
 * - Bajo (menos de [RowMaxHeight]): una fila con portada, título y barra del libro, y botones.
 * - Estrecho o más alto que ancho: en vertical, portada arriba, textos y botones abajo.
 * - Ancho: portada a la izquierda con los textos, y los botones a todo lo ancho debajo.
 * Los textos entran según la altura (título siempre; tramo, barra y tiempos si caben) y los
 * botones según el ancho ([buttonsFor]).
 */
@Composable
private fun WidgetContent(model: WidgetModel, cover: Bitmap?) {
    val c = if (model.dark) darkColors(model.accent) else lightColors(model.accent)
    val size = LocalSize.current
    val root = GlanceModifier.fillMaxSize().appWidgetBackground().cornerRadius(22.dp).background(c.surface)
    val book = model.book
    when {
        book == null -> Empty(c, root)
        size.height < RowMaxHeight -> RowLayout(model, book, cover, c, root, size)
        size.width < WideMinWidth || size.height >= size.width * TallRatio -> TallLayout(model, book, cover, c, root, size)
        else -> WideLayout(model, book, cover, c, root, size)
    }
}

private val RowMaxHeight = 116.dp
private val WideMinWidth = 200.dp
private const val TallRatio = 1.2f
private val Pad = 12.dp
private val ButtonSize = 44.dp
private val ButtonGap = 4.dp
private val ButtonRowHeight = 52.dp

/** Alturas aproximadas de los textos: Glance no mide, se reserva lo que ocupan. */
private val TitleHeight = 22.dp
private val SegmentHeight = 20.dp
private val BarHeight = 12.dp
private val BarWithTimesHeight = 30.dp

private fun buttonsWidth(n: Int) = ButtonSize * n + ButtonGap * (n - 1).coerceAtLeast(0)

/**
 * Botones que caben en [width], por prioridad: play; los dos huecos del widget a sus lados; los
 * extremos del reproductor (anterior y siguiente); marcar. Sin repetir acciones ni "Nada".
 */
private fun buttonsFor(width: Dp, model: WidgetModel): List<ActionCall> {
    val play = ActionCall(PlayerAction.PLAY_PAUSE)
    val (left, right) = model.buttons
    val (first, last) = model.extraButtons
    val steps = listOf(
        listOf(play),
        listOf(left, play, right),
        listOf(first, left, play, right, last),
        listOf(first, left, play, right, last, ActionCall(PlayerAction.ADD_BOOKMARK)),
    ).map { step -> step.filter { it.action != PlayerAction.NONE }.distinctBy { it.action to it.seconds } }
    return steps.lastOrNull { buttonsWidth(it.size) <= width } ?: steps.first()
}

/** Sin libro: logo y "Elige un libro"; abre la Biblioteca. */
@Composable
private fun Empty(c: LectorColors, modifier: GlanceModifier) {
    val context = LocalContext.current
    Row(
        modifier.padding(horizontal = 16.dp).clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(ImageProvider(R.drawable.ic_lector_logo), null, GlanceModifier.size(36.dp))
        Spacer(GlanceModifier.width(12.dp))
        Column {
            Text("lector", style = TextStyle(color = color(c.text), fontSize = 15.sp, fontWeight = FontWeight.Medium))
            Text(context.getString(R.string.widget_empty), style = TextStyle(color = color(c.textSecondary), fontSize = 12.sp))
        }
    }
}

/** Una fila: portada, título y barra del libro (al menos [RowTitleMinWidth]) y los botones que quepan. */
@Composable
private fun RowLayout(model: WidgetModel, book: WidgetBook, cover: Bitmap?, c: LectorColors, modifier: GlanceModifier, size: DpSize) {
    val open = openPlayer()
    val coverSize = (size.height - 16.dp).coerceIn(32.dp, 64.dp)
    val showCover = model.showCovers && size.width >= RowCoverMinWidth
    val coverWidth = if (showCover) coverSize + Pad else 0.dp
    val buttons = buttonsFor(size.width - Pad * 2 - coverWidth - RowTitleMinWidth - ButtonGap, model)
    Row(modifier.padding(horizontal = Pad), verticalAlignment = Alignment.CenterVertically) {
        if (showCover) {
            Cover(book, cover, model.dark, coverSize, GlanceModifier.clickable(open))
            Spacer(GlanceModifier.width(Pad))
        }
        Column(GlanceModifier.defaultWeight().clickable(open)) {
            Title(book, c)
            Spacer(GlanceModifier.height(7.dp))
            Bar(book.positionMs, book.durationMs, c)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            buttons.forEach { call ->
                Box(GlanceModifier.padding(start = ButtonGap)) { Button(call, book, c) }
            }
        }
    }
}

private val RowTitleMinWidth = 80.dp
private val RowCoverMinWidth = 240.dp

/**
 * En vertical: portada arriba (lo que deje el resto, hasta todo el ancho), título, tramo y barra
 * con tiempos si caben, y la fila de botones abajo.
 */
@Composable
private fun TallLayout(model: WidgetModel, book: WidgetBook, cover: Bitmap?, c: LectorColors, modifier: GlanceModifier, size: DpSize) {
    val open = openPlayer()
    val inner = DpSize(size.width - Pad * 2, size.height - Pad * 2)
    val forText = inner.height - ButtonRowHeight - 8.dp
    // De más a menos: todos los textos, sin el tramo, solo título y barra, solo título.
    val options = listOf(
        TextSet(segment = true, times = true),
        TextSet(segment = false, times = true),
        TextSet(segment = false, times = false),
        TextSet(segment = false, times = false, bar = false),
    )
    val coverFor = { t: TextSet -> minOf(inner.width, forText - t.height - 8.dp) }
    val text = options.firstOrNull { !model.showCovers || coverFor(it) >= TallCoverMinSize } ?: options.last()
    val coverSize = coverFor(text).takeIf { model.showCovers && it >= TallCoverMinSize }
    // La portada con imagen ocupa el hueco real (puede ser mayor que el tamaño de referencia),
    // ajustada sin recortar; la tipográfica, el tamaño calculado.
    val flexibleCover = coverSize != null && cover != null
    Column(modifier.padding(Pad)) {
        if (coverSize != null) {
            if (cover != null) {
                Image(ImageProvider(cover), null, GlanceModifier.fillMaxWidth().defaultWeight().clickable(open), contentScale = ContentScale.Fit)
            } else {
                Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Cover(book, null, model.dark, coverSize, GlanceModifier.clickable(open))
                }
            }
            Spacer(GlanceModifier.height(8.dp))
        }
        Info(book, text, c, GlanceModifier.fillMaxWidth().clickable(open))
        Spacer(if (flexibleCover) GlanceModifier.height(4.dp) else GlanceModifier.defaultWeight())
        ButtonRow(buttonsFor(inner.width, model), book, c)
    }
}

private val TallCoverMinSize = 48.dp

/** Ancho: portada a la izquierda con los textos a su derecha, y la fila de botones debajo, a todo lo ancho. */
@Composable
private fun WideLayout(model: WidgetModel, book: WidgetBook, cover: Bitmap?, c: LectorColors, modifier: GlanceModifier, size: DpSize) {
    val open = openPlayer()
    val inner = DpSize(size.width - Pad * 2, size.height - Pad * 2)
    val top = inner.height - ButtonRowHeight - 8.dp
    val text = listOf(
        TextSet(segment = true, times = true),
        TextSet(segment = false, times = true),
        TextSet(segment = false, times = false),
    ).firstOrNull { it.height <= top } ?: TextSet(segment = false, times = false, bar = false)
    val coverSize = minOf(top, inner.width * 0.45f, WideCoverMaxSize)
    Column(modifier.padding(Pad)) {
        Row(GlanceModifier.fillMaxWidth().height(top)) {
            if (model.showCovers) {
                Cover(book, cover, model.dark, coverSize, GlanceModifier.clickable(open))
                Spacer(GlanceModifier.width(Pad))
            }
            Info(book, text, c, GlanceModifier.defaultWeight().fillMaxSize().clickable(open), barAtBottom = true)
        }
        Spacer(GlanceModifier.defaultWeight())
        ButtonRow(buttonsFor(inner.width, model), book, c)
    }
}

private val WideCoverMaxSize = 240.dp

/** Qué textos entran bajo el título. */
private data class TextSet(val segment: Boolean, val times: Boolean, val bar: Boolean = true) {
    val height: Dp get() = TitleHeight +
        (if (segment) SegmentHeight else 0.dp) +
        (if (!bar) 0.dp else if (times) BarWithTimesHeight else BarHeight)
}

/** Título, tramo y barra del tramo con sus tiempos, según [text]. [barAtBottom]: la barra al pie del hueco. */
@Composable
private fun Info(book: WidgetBook, text: TextSet, c: LectorColors, modifier: GlanceModifier, barAtBottom: Boolean = false) {
    val meta = TextStyle(color = color(c.textSecondary), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    Column(modifier) {
        Title(book, c)
        if (text.segment && book.segmentTitle.isNotBlank()) {
            Spacer(GlanceModifier.height(4.dp))
            Text(book.segmentTitle, style = TextStyle(color = color(c.textSecondary), fontSize = 12.sp), maxLines = 1)
        }
        if (text.bar) {
            if (barAtBottom) Spacer(GlanceModifier.defaultWeight()) else Spacer(GlanceModifier.height(9.dp))
            Bar(book.segmentPositionMs, book.segmentDurationMs, c)
            if (text.times) {
                Spacer(GlanceModifier.height(6.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    Text(formatDuration(book.segmentPositionMs), style = meta)
                    Spacer(GlanceModifier.defaultWeight())
                    Text(formatDuration(book.segmentDurationMs), style = meta)
                }
            }
        }
    }
}

@Composable
private fun Title(book: WidgetBook, c: LectorColors) {
    Text(book.title, style = TextStyle(color = color(c.text), fontSize = 15.sp, fontWeight = FontWeight.Medium), maxLines = 1)
}

/** Botones repartidos a todo lo ancho, cada uno en una celda igual (una fila de Glance admite 10 elementos). */
@Composable
private fun ButtonRow(buttons: List<ActionCall>, book: WidgetBook, c: LectorColors) {
    Row(GlanceModifier.fillMaxWidth().height(ButtonRowHeight), verticalAlignment = Alignment.CenterVertically) {
        buttons.forEach { call ->
            Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) { Button(call, book, c) }
        }
    }
}

@Composable
private fun Button(call: ActionCall, book: WidgetBook, c: LectorColors) {
    if (call.action == PlayerAction.PLAY_PAUSE) PlayButton(book.playing, c) else Slot(call, c)
}

@Composable
private fun openPlayer() = actionStartActivity(
    Intent(LocalContext.current, MainActivity::class.java).setAction(MainActivity.ACTION_OPEN_PLAYER),
)

/** Portada, o la tipográfica con el tono del autor si el libro no tiene. */
@Composable
private fun Cover(book: WidgetBook, cover: Bitmap?, dark: Boolean, size: Dp, modifier: GlanceModifier) {
    if (cover != null) {
        Image(ImageProvider(cover), null, modifier.size(size).cornerRadius(6.dp), contentScale = ContentScale.Crop)
        return
    }
    val (background, ink) = coverTone(toneKeyOf(book.author, book.title), dark)
    Box(modifier.size(size).cornerRadius(6.dp).background(background).padding(6.dp), contentAlignment = Alignment.BottomStart) {
        Text(
            book.title,
            style = TextStyle(color = color(ink), fontSize = if (size > 80.dp) 16.sp else 10.sp, fontWeight = FontWeight.Medium),
            maxLines = 3,
        )
    }
}

@Composable
private fun Bar(positionMs: Long, durationMs: Long, c: LectorColors) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    LinearProgressIndicator(
        progress = progress,
        modifier = GlanceModifier.fillMaxWidth().height(3.dp),
        color = color(c.accent),
        backgroundColor = color(c.track),
    )
}

@Composable
private fun PlayButton(playing: Boolean, c: LectorColors) {
    val context = LocalContext.current
    Box(
        GlanceModifier.size(44.dp).cornerRadius(22.dp).background(c.accent).clickable(actionFor(ActionCall(PlayerAction.PLAY_PAUSE))),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            ImageProvider(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
            context.getString(if (playing) R.string.pause else R.string.play),
            GlanceModifier.size(20.dp),
            colorFilter = ColorFilter.tint(color(c.onAccent)),
        )
    }
}

/** Hueco: saltos con su número dentro de la flecha, el resto con su icono, "Nada" vacío. */
@Composable
private fun Slot(call: ActionCall, c: LectorColors) {
    val context = LocalContext.current
    val box = GlanceModifier.size(width = 44.dp, height = 44.dp)
    when (call.action) {
        PlayerAction.NONE -> Spacer(box)
        PlayerAction.SKIP_BACK, PlayerAction.SKIP_FORWARD -> {
            val back = call.action == PlayerAction.SKIP_BACK
            Box(box.clickable(actionFor(call)), contentAlignment = Alignment.Center) {
                Image(
                    ImageProvider(if (back) R.drawable.ic_replay else R.drawable.ic_forward),
                    context.getString(if (back) R.string.skip_back_seconds else R.string.skip_forward_seconds, call.seconds),
                    GlanceModifier.size(32.dp),
                    colorFilter = ColorFilter.tint(color(c.text)),
                )
                Text(
                    "${call.seconds}",
                    style = TextStyle(color = color(c.text), fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(start = if (back) 1.dp else 0.dp, end = if (back) 0.dp else 1.dp),
                )
            }
        }
        else -> Box(box.clickable(actionFor(call)), contentAlignment = Alignment.Center) {
            Image(
                ImageProvider(call.action.iconRes() ?: R.drawable.ic_more),
                context.getString(call.action.nameRes()),
                GlanceModifier.size(22.dp),
                colorFilter = ColorFilter.tint(color(if (call.action == PlayerAction.ADD_BOOKMARK) c.accent else c.text)),
            )
        }
    }
}

private fun actionFor(call: ActionCall) = actionRunCallback<WidgetActionCallback>(
    actionParametersOf(ActionKey to call.action.name, SecondsKey to call.seconds),
)
