package codelab.lector.ui.player

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.R
import codelab.lector.data.db.Book
import codelab.lector.playback.ActionCall
import codelab.lector.playback.NowPlaying
import codelab.lector.playback.iconRes
import codelab.lector.playback.nameRes
import codelab.lector.playback.PlayerAction
import codelab.lector.ui.bookmarks.BookBookmarksPanel
import codelab.lector.ui.bookmarks.LocalBookmarkSheets
import codelab.lector.ui.components.BookCover
import codelab.lector.ui.components.IconAction
import codelab.lector.ui.components.LocalUndoState
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.SeekBar
import codelab.lector.ui.components.SegmentedControl
import codelab.lector.ui.components.TextButton
import codelab.lector.ui.formatDuration
import kotlin.math.abs
import kotlin.math.roundToInt
import codelab.lector.ui.formatSpeed
import codelab.lector.ui.theme.LectorTheme

private enum class PlayerSheet { MENU, SPEED, SOUND, CHAPTERS, SLEEP }

/** Marco de la portada del lienzo (358 × 411) mientras no hay imagen. */
private const val CoverRatio = 358f / 411f

/** "Explicit liber." solo en el final: la posición guardada puede quedar un poco antes del total. */
private const val ExplicitMarginMs = 1_500L

/** Escuchando (design.md › Decisiones de diseño: reproductor A). */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    slide: PlayerSlide,
    onOpenSettings: () -> Unit,
    onOpenSleepSettings: () -> Unit,
    onOpenCover: (String) -> Unit,
    onShowFolder: (String) -> Unit,
    onShown: () -> Unit = {},
) {
    val playing by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val inaccessible by viewModel.inaccessibleBook.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarkCount.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val sleep by viewModel.sleep.collectAsStateWithLifecycle()
    val showCover = appearance.showCovers
    var sheet by remember { mutableStateOf<PlayerSheet?>(null) }
    val undo = LocalUndoState.current
    val bookmarkSheets = LocalBookmarkSheets.current
    val removedText = stringResource(R.string.removed_from_library)

    // Sube solo si se acaba de abrir: no al volver de Ajustes ni al girar la pantalla.
    // [onShown] cuando ya está arriba del todo.
    LaunchedEffect(slide) {
        slide.onShown()
        snapshotFlow { slide.isOpen }.first { it }
        onShown()
    }
    BackHandler { slide.close() }

    // Horizontal (más ancha que alta): doble panel, sin cabecera. Recortada a su área: al bajar
    // no asoma por detrás de la barra de navegación del sistema.
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val close: () -> Unit = slide::close
        val landscape = maxWidth > maxHeight
        val np = playing
        val lost = inaccessible
        val removeLost = { book: Book ->
            undo.show(removedText, viewModel.removeInaccessible(book.id))
            close()
        }
        // Opaca: debajo está la pantalla anterior, que se ve al bajarla.
        CompositionLocalProvider(LocalPlayerSlide provides slide) {
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = slide.offset }.background(LectorTheme.colors.background)) {
        if (landscape) {
            when {
                np != null -> LandscapePlayer(
                    np = np,
                    showCover = showCover,
                    buttons = settings.playerButtons,
                    onAct = viewModel::act,
                    onCall = viewModel::act,
                    onJump = viewModel::jumpTo,
                    onSegment = viewModel::jumpToSegment,
                    onCover = { onOpenCover(np.bookId) },
                    onChapters = { sheet = PlayerSheet.CHAPTERS },
                    onSpeed = { sheet = PlayerSheet.SPEED },
                    onMenu = { sheet = PlayerSheet.MENU },
                    sleepActive = sleep.active,
                    onSleep = { sheet = PlayerSheet.SLEEP },
                )
                lost != null -> InaccessibleLandscape(lost, viewModel.coverPath(lost.id), showCover, viewModel::rescan, { removeLost(lost) })
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                // Con portada, una raya como la de las hojas en lugar de la cabecera: más alto para la imagen.
                if (np == null || !showCover) Header(close) else MinimizeHandle(close)
                when {
                    np != null -> Player(
                        np = np,
                        showCover = showCover,
                        buttons = settings.playerButtons,
                        onCall = viewModel::act,
                        bookmarks = bookmarks,
                        onBookmarks = { bookmarkSheets.showBook(np.bookId) },
                        onAct = viewModel::act,
                        onJump = viewModel::jumpTo,
                        onCover = { onOpenCover(np.bookId) },
                        onChapters = { sheet = PlayerSheet.CHAPTERS },
                        onSpeed = { sheet = PlayerSheet.SPEED },
                        onMenu = { sheet = PlayerSheet.MENU },
                        sleepActive = sleep.active,
                        onSleep = { sheet = PlayerSheet.SLEEP },
                    )
                    lost != null -> Inaccessible(lost, viewModel.coverPath(lost.id), showCover, viewModel::rescan, { removeLost(lost) })
                }
            }
        }
        }
        }
    }

    val np = playing ?: return
    when (sheet) {
        PlayerSheet.MENU -> PlayerMenuSheet(
            np = np,
            viewModel = viewModel,
            onDismiss = { sheet = null },
            onSound = { sheet = PlayerSheet.SOUND },
            onSpeed = { sheet = PlayerSheet.SPEED },
            onSleep = { sheet = PlayerSheet.SLEEP },
            onFolder = {
                sheet = null
                viewModel.bookFolder(onShowFolder)
            },
            onSettings = {
                sheet = null
                onOpenSettings()
            },
        )
        PlayerSheet.SPEED -> SpeedSheet(np, settings.newBookSpeed, viewModel, onDismiss = { sheet = null })
        PlayerSheet.SOUND -> SoundSheet(np, viewModel, onDismiss = { sheet = null })
        PlayerSheet.SLEEP -> SleepSheet(
            state = sleep,
            extend = settings.sleepExtend,
            lastMinutes = settings.sleepLastMinutes,
            onSet = viewModel::setSleep,
            onExtend = viewModel::setSleepExtend,
            onOther = viewModel::setSleepOther,
            onMore = {
                sheet = null
                onOpenSleepSettings()
            },
            onDismiss = { sheet = null },
        )
        PlayerSheet.CHAPTERS -> ChaptersSheet(
            np,
            onSegment = {
                sheet = null
                viewModel.jumpToSegment(it)
            },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

@Composable
private fun Header(onMinimize: () -> Unit) {
    val c = LectorTheme.colors
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAction(painterResource(R.drawable.ic_chevron_down), stringResource(R.string.minimize_player), onMinimize)
        Text(
            stringResource(R.string.tab_listening).uppercase(),
            style = LectorTheme.type.secondary.copy(letterSpacing = 1.sp),
            color = c.textSecondary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(44.dp))
    }
}

@Composable
private fun ColumnScope.Player(
    np: NowPlaying,
    showCover: Boolean,
    buttons: List<ActionCall>,
    onCall: (ActionCall) -> Unit,
    bookmarks: Int,
    onBookmarks: () -> Unit,
    onAct: (PlayerAction) -> Unit,
    onJump: (Long) -> Unit,
    onCover: () -> Unit,
    onChapters: () -> Unit,
    onSpeed: () -> Unit,
    onMenu: () -> Unit,
    sleepActive: Boolean,
    onSleep: () -> Unit,
) {
    var scrub by remember { mutableStateOf<Scrub?>(null) }
    if (showCover) {
        CoverArea(Modifier.weight(1f), horizontal = 8.dp) {
            PlayerCover(np, onCover)
            scrub?.let { ScrubLabel(np, it) }
        }
        TitleBlock(np, Modifier.fillMaxWidth().padding(horizontal = 24.dp))
    } else {
        // Lienzo "Reproductor sin portadas": el hueco queda en medio; deslizarlo hacia abajo hace Atrás.
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxSize().slideDown()) {
                NoCoverTitle(np, Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 28.dp))
            }
            scrub?.let { ScrubLabel(np, it) }
        }
    }
    Bars(np, onJump, onChapters, { scrub = it }, Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp))

    // Controles: los huecos 1 y 2, play, los huecos 3 y 4 (Ajustes › Botones).
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SlotButton(buttons[0], np, compact = false, onCall)
        SlotButton(buttons[1], np, compact = false, onCall)
        PlayButton(np, 68.dp, 28.dp, onAct)
        SlotButton(buttons[2], np, compact = false, onCall)
        SlotButton(buttons[3], np, compact = false, onCall)
    }

    ActionRow(np, bookmarks, onBookmarks, sleepActive, onSleep, onAct, onSpeed, onMenu, Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 4.dp, bottom = 2.dp))
}

/** Separación entre la portada y la columna central en horizontal. */
private val LandscapeGap = 18.dp

/** Ancho mínimo de la columna central en horizontal: lo que ocupan los cinco controles. */
private val LandscapeColumnMin = 236.dp

/**
 * Escuchando en horizontal (lienzo: "Horizontal a doble panel"): portada, columna con título,
 * barras y controles, y panel de 280 con los tramos. La portada se queda con el ancho que deja la
 * columna central, hasta 300.
 */
@Composable
private fun LandscapePlayer(
    np: NowPlaying,
    showCover: Boolean,
    buttons: List<ActionCall>,
    onCall: (ActionCall) -> Unit,
    onAct: (PlayerAction) -> Unit,
    onJump: (Long) -> Unit,
    onSegment: (Int) -> Unit,
    onCover: () -> Unit,
    onChapters: () -> Unit,
    onSpeed: () -> Unit,
    onMenu: () -> Unit,
    sleepActive: Boolean,
    onSleep: () -> Unit,
) {
    var scrub by remember { mutableStateOf<Scrub?>(null) }
    Row(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().padding(start = 24.dp, top = 20.dp, end = 20.dp, bottom = 20.dp)) {
            val coverMax = (maxWidth - LandscapeGap - LandscapeColumnMin).coerceAtMost(300.dp)
            val withCover = showCover && coverMax >= 96.dp
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(LandscapeGap)) {
                if (withCover) {
                    Box(Modifier.fillMaxHeight().widthIn(max = coverMax), contentAlignment = Alignment.Center) {
                        PlayerCover(np, onCover)
                        scrub?.let { ScrubLabel(np, it) }
                    }
                }
                // Sin portada, el tiempo grande va arriba de la columna, sobre el título.
                Box(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize()) {
                    TitleBlock(np, Modifier.fillMaxWidth())
                    Spacer(Modifier.weight(1f))
                    Bars(np, onJump, onChapters, { scrub = it }, Modifier.fillMaxWidth())
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SlotButton(buttons[0], np, compact = true, onCall)
                        SlotButton(buttons[1], np, compact = true, onCall)
                        PlayButton(np, 56.dp, 24.dp, onAct)
                        SlotButton(buttons[2], np, compact = true, onCall)
                        SlotButton(buttons[3], np, compact = true, onCall)
                    }
                    // Sin marcadores del libro: el panel de la derecha los tiene.
                    ActionRow(np, null, {}, sleepActive, onSleep, onAct, onSpeed, onMenu, Modifier.fillMaxWidth().padding(top = 6.dp))
                }
                if (!withCover) scrub?.let { ScrubLabel(np, it, Modifier.align(Alignment.TopCenter)) }
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(LectorTheme.colors.divider))
        SidePanel(np, onSegment, Modifier.width(280.dp).fillMaxHeight())
    }
}

/**
 * Panel derecho en horizontal: pestañas Capítulos (o Archivos) y Marcadores. Los tramos con las
 * filas de la hoja de capítulos; sigue al tramo en curso. Marcadores, como la hoja del libro.
 */
@Composable
private fun SidePanel(np: NowPlaying, onSegment: (Int) -> Unit, modifier: Modifier) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.padding(top = 16.dp)) {
        SegmentedControl(
            listOf(stringResource(if (np.hasChapters) R.string.chapters else R.string.files), stringResource(R.string.tab_bookmarks)),
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        )
        if (tab == 0) SegmentsPanel(np, onSegment, Modifier.weight(1f))
        else BookBookmarksPanel(np.bookId, np.positionMs, Modifier.weight(1f))
    }
}

@Composable
private fun SegmentsPanel(np: NowPlaying, onSegment: (Int) -> Unit, modifier: Modifier) {
    val c = LectorTheme.colors
    run {
        val list = rememberLazyListState(initialFirstVisibleItemIndex = (np.segmentIndex - 2).coerceAtLeast(0))
        // Al cambiar de tramo, si el nuevo no se ve, se desplaza hasta él.
        LaunchedEffect(np.bookId, np.segmentIndex) {
            if (list.layoutInfo.visibleItemsInfo.none { it.index == np.segmentIndex }) {
                list.animateScrollToItem((np.segmentIndex - 2).coerceAtLeast(0))
            }
        }
        LazyColumn(modifier, state = list) {
            itemsIndexed(np.segments) { i, _ -> SegmentRow(np, i, onSegment, base = c.background, highlight = c.surface) }
        }
    }
}

/** Portada: marco de 358 × 411 hasta conocer la imagen; después, su proporción, sin recortar. */
@Composable
private fun PlayerCover(np: NowPlaying, onCover: () -> Unit) {
    var ratio by remember(np.coverPath) { mutableStateOf(CoverRatio) }
    BookCover(
        np.coverPath,
        np.title,
        author = np.author,
        modifier = Modifier
            .aspectRatio(ratio, matchHeightConstraintsFirst = true)
            .slideDown()
            .clickable(onClickLabel = stringResource(R.string.view_cover), role = Role.Image, onClick = onCover),
        titleStyle = LectorTheme.type.headline,
        onAspectRatio = { ratio = it },
    )
}

/** En vertical con portada: raya de las hojas (40 × 4) en una franja de 24. Deslizar o tocar minimiza. */
@Composable
private fun MinimizeHandle(onMinimize: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(24.dp)
            .slideDown()
            .clickable(onClickLabel = stringResource(R.string.minimize_player), role = Role.Button, onClick = onMinimize),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(width = 40.dp, height = 4.dp).background(LectorTheme.colors.outline, RoundedCornerShape(2.dp)))
    }
}

/** Sin portadas: autor en mayúsculas, título de 28, narrador y "6% · quedan 24:22:14". */
@Composable
private fun NoCoverTitle(np: NowPlaying, modifier: Modifier) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        np.author?.takeIf { it.isNotBlank() }?.let {
            Text(it.uppercase(), style = t.secondary.copy(letterSpacing = 1.5.sp), color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(np.title, style = t.headline.copy(lineHeight = 32.sp), color = c.text, maxLines = 4, overflow = TextOverflow.Ellipsis)
        np.narrator?.takeIf { it.isNotBlank() && it != np.author }?.let {
            Text(stringResource(R.string.narrated_by, it), style = t.body, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val percent = if (np.durationMs > 0) (np.positionMs * 100.0 / np.durationMs).roundToInt() else 0
        Text(
            stringResource(R.string.book_progress, percent, formatDuration((np.durationMs - np.positionMs).coerceAtLeast(0))),
            style = t.meta.copy(fontSize = 14.sp),
            color = c.textSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Título y autor · narrador, a todo el ancho: el porcentaje va entre los tiempos del libro. */
@Composable
private fun TitleBlock(np: NowPlaying, modifier: Modifier) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(np.title, style = t.bookTitle, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val byline = listOfNotNull(np.author, np.narrator).filter { it.isNotBlank() }.distinct().joinToString(" · ")
        if (byline.isNotEmpty()) Text(byline, style = t.body, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Arrastre de una barra: adónde se va, desde dónde (al empezar) y el divisor del arrastre fino. */
private class Scrub(val targetMs: Long, val fromMs: Long, val fine: Int)

/**
 * Tiempo grande mientras se arrastra una barra, sobre la portada para que el dedo no lo tape:
 * adónde se va, cuánto se mueve, el capítulo en el que cae y el arrastre fino si lo hay.
 */
@Composable
private fun ScrubLabel(np: NowPlaying, scrub: Scrub, modifier: Modifier = Modifier) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val delta = scrub.targetMs - scrub.fromMs
    val chapter = if (np.hasChapters) np.segments.indexOfLast { it.startMs <= scrub.targetMs }.takeIf { it >= 0 } else null
    Column(
        modifier
            .background(c.popup.copy(alpha = 0.94f), RoundedCornerShape(12.dp))
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(formatDuration(scrub.targetMs), style = t.value, color = c.text)
        Text((if (delta < 0) "−" else "+") + formatDuration(abs(delta)), style = t.meta.copy(fontSize = 15.sp), color = c.textSecondary)
        if (chapter != null) {
            Text(
                stringResource(R.string.chapter_short, chapter + 1) + " · " + np.segments[chapter].title,
                style = t.secondary,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 260.dp),
            )
        }
        if (scrub.fine > 1) Text(stringResource(R.string.fine_scrub, scrub.fine), style = t.secondary, color = c.accent)
    }
}

/** Barra del libro con marcas de tramo, tiempos, tramo actual y su barra. */
@Composable
private fun Bars(np: NowPlaying, onJump: (Long) -> Unit, onChapters: () -> Unit, onScrub: (Scrub?) -> Unit, modifier: Modifier) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val mono13 = t.meta.copy(fontSize = 13.sp)
    var bookDrag by remember { mutableStateOf<Float?>(null) }
    var segmentDrag by remember { mutableStateOf<Float?>(null) }
    // Posición al empezar a arrastrar: el desplazamiento se cuenta desde ahí aunque siga sonando.
    var dragFrom by remember { mutableStateOf<Long?>(null) }
    fun scrubTo(targetMs: Long?, fine: Int) {
        if (targetMs == null) {
            dragFrom = null
            onScrub(null)
            return
        }
        val from = dragFrom ?: np.positionMs.also { dragFrom = it }
        onScrub(Scrub(targetMs, from, fine))
    }
    Column(modifier) {
        val total = np.durationMs.coerceAtLeast(1)
        val bookPos = bookDrag?.let { (it * total).toLong() } ?: np.positionMs
        val marks = remember(np.segments, total) {
            np.segments.drop(1).map { it.startMs.toFloat() / total }
        }
        // Cada barra con sus números encima: los del libro no se mezclan con los del tramo.
        // Porcentaje en medio: mismo ancho a cada lado para que quede centrado.
        Row(Modifier.fillMaxWidth()) {
            Text(formatDuration(bookPos), style = mono13, color = c.text, modifier = Modifier.weight(1f))
            val percent = (bookPos * 100.0 / total).roundToInt()
            Text("$percent%", style = mono13, color = c.textSecondary)
            Text("−" + formatDuration(np.durationMs - bookPos), style = mono13, color = c.textSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        SeekBar(
            fraction = np.positionMs.toFloat() / total,
            onSeek = { onJump((it * total).toLong()) },
            marks = marks,
            onDrag = { f, fine ->
                bookDrag = f
                scrubTo(f?.let { (it * total).toLong() }, fine)
            },
        )

        val segLength = (np.segmentEndMs - np.segmentStartMs).coerceAtLeast(1)
        // Lista de tramos: capítulos o, sin ellos, archivos si hay más de uno.
        val hasList = np.hasChapters || np.segments.size > 1
        val segPos = segmentDrag?.let { (it * segLength).toLong() } ?: (np.positionMs - np.segmentStartMs).coerceIn(0, segLength)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .heightIn(min = 26.dp)
                .let { if (hasList) it.clickable(role = Role.Button, onClick = onChapters) else it },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Colofón de copista al terminar escuchando hasta el final, en el color de la rúbrica.
                if (np.finished && segmentDrag == null && np.positionMs >= np.durationMs - ExplicitMarginMs) {
                    Text("Explicit liber.", style = t.secondary.copy(fontStyle = FontStyle.Italic), color = c.accent, maxLines = 1)
                } else if (np.hasChapters) {
                    Text(stringResource(R.string.chapter_short, np.segmentIndex + 1), style = t.secondary, color = c.accent)
                    Text(np.segmentTitle, style = t.secondary, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Icon(painterResource(R.drawable.ic_chevron_down), stringResource(R.string.chapters), Modifier.size(14.dp), tint = c.textSecondary)
                } else {
                    // Sin capítulos, los archivos: tocar abre la misma hoja, como "Archivos".
                    Text(np.segmentTitle, style = t.secondary, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (hasList) Icon(painterResource(R.drawable.ic_chevron_down), stringResource(R.string.files), Modifier.size(14.dp), tint = c.textSecondary)
                }
            }
            Text("${formatDuration(segPos)} / ${formatDuration(segLength)}", style = mono13, color = c.textSecondary)
        }
        // Pegada a su fila: la zona táctil de 24 deja aire de sobra encima de una barra de 3.
        SeekBar(
            modifier = Modifier.offset(y = (-4).dp),
            fraction = (np.positionMs - np.segmentStartMs).toFloat() / segLength,
            onSeek = { onJump(np.segmentStartMs + (it * segLength).toLong()) },
            thickness = 3.dp,
            color = c.textSecondary,
            onDrag = { f, fine ->
                segmentDrag = f
                scrubTo(f?.let { np.segmentStartMs + (it * segLength).toLong() }, fine)
            },
        )
    }
}

@Composable
private fun previousLabel(np: NowPlaying) =
    stringResource(if (np.hasChapters) R.string.previous_chapter else R.string.previous_file)

@Composable
private fun nextLabel(np: NowPlaying) =
    stringResource(if (np.hasChapters) R.string.next_chapter else R.string.next_file)

/**
 * Hueco de Ajustes › Botones: saltos con el número dentro de la flecha, el resto con su icono.
 * Vertical, de 56 / 52; [compact] (horizontal), de 44. "Nada" deja el hueco vacío.
 */
@Composable
private fun SlotButton(call: ActionCall, np: NowPlaying, compact: Boolean, onCall: (ActionCall) -> Unit) {
    val size = if (compact) 44.dp else 52.dp
    val iconSize = if (compact) 22.dp else 26.dp
    val onClick = { onCall(call) }
    when (call.action) {
        PlayerAction.NONE -> Spacer(Modifier.size(size))
        PlayerAction.SKIP_BACK, PlayerAction.SKIP_FORWARD -> {
            val back = call.action == PlayerAction.SKIP_BACK
            val description = stringResource(if (back) R.string.skip_back_seconds else R.string.skip_forward_seconds, call.seconds)
            SkipButton(if (back) R.drawable.ic_replay else R.drawable.ic_forward, back, call.seconds, description, compact, onClick)
        }
        PlayerAction.PREVIOUS -> RoundIcon(R.drawable.ic_skip_previous, previousLabel(np), size, iconSize, onClick = onClick)
        PlayerAction.NEXT -> RoundIcon(R.drawable.ic_skip_next, nextLabel(np), size, iconSize, onClick = onClick)
        PlayerAction.PLAY_PAUSE -> {
            val playing = np.playWhenReady
            RoundIcon(if (playing) R.drawable.ic_pause else R.drawable.ic_play, stringResource(if (playing) R.string.pause else R.string.play), size, iconSize, onClick = onClick)
        }
        PlayerAction.ADD_BOOKMARK ->
            RoundIcon(R.drawable.ic_bookmark, stringResource(R.string.add_bookmark), size, iconSize - 2.dp, tint = LectorTheme.colors.accent, onClick = onClick)
        else -> RoundIcon(call.action.iconRes() ?: R.drawable.ic_more, stringResource(call.action.nameRes()), size, iconSize - 2.dp, onClick = onClick)
    }
}

/** Play / pausa en acento, según "va a sonar". */
@Composable
private fun PlayButton(np: NowPlaying, size: Dp, iconSize: Dp, onAct: (PlayerAction) -> Unit) {
    val c = LectorTheme.colors
    val playing = np.playWhenReady
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(c.accent)
            .clickable(role = Role.Button) { onAct(PlayerAction.PLAY_PAUSE) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
            stringResource(if (playing) R.string.pause else R.string.play),
            Modifier.size(iconSize),
            tint = c.onAccent,
        )
    }
}

/**
 * Fila bajo los controles: pausa diferida, marcar, velocidad, marcadores del libro ([bookmarks];
 * null la quita) y ⋯ (su menú sale por abajo).
 */
@Composable
private fun ActionRow(
    np: NowPlaying,
    bookmarks: Int?,
    onBookmarks: () -> Unit,
    sleepActive: Boolean,
    onSleep: () -> Unit,
    onAct: (PlayerAction) -> Unit,
    onSpeed: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier,
) {
    val c = LectorTheme.colors
    Row(
        modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // La luna, en acento mientras la pausa diferida está activa.
        SmallAction(R.drawable.ic_sleep, null, stringResource(R.string.settings_sleep), if (sleepActive) c.accent else c.iconSoft, onClick = onSleep)
        RoundIcon(R.drawable.ic_bookmark, stringResource(R.string.add_bookmark), 44.dp, 20.dp, tint = c.accent) {
            onAct(PlayerAction.ADD_BOOKMARK)
        }
        val speed = formatSpeed(np.speed)
        SmallAction(null, speed, stringResource(R.string.speed_value, speed), c.iconSoft, onClick = onSpeed)
        if (bookmarks != null) {
            SmallAction(R.drawable.ic_list, "$bookmarks", stringResource(R.string.book_bookmarks_count, bookmarks), c.iconSoft, onClick = onBookmarks)
        }
        SmallAction(R.drawable.ic_more, null, stringResource(R.string.more_options), c.iconSoft, onClick = onMenu)
    }
}

/** Zona de la portada: ocupa el alto libre y la portada se ajusta sin deformarse. */
@Composable
private fun CoverArea(modifier: Modifier, horizontal: Dp = 16.dp, content: @Composable () -> Unit) {
    Box(
        modifier.fillMaxWidth().padding(start = horizontal, end = horizontal, bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun RoundIcon(
    @DrawableRes icon: Int,
    description: String,
    size: Dp,
    iconSize: Dp,
    tint: Color = LectorTheme.colors.text,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(size).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), description, Modifier.size(iconSize), tint = tint)
    }
}

/**
 * Salto con el número de segundos dentro de la flecha circular: 56 con icono de 40, o 44 con icono
 * de 32 si [compact]. El círculo de cada icono está a un lado del centro (la flecha ocupa el otro):
 * el número se desplaza a su centro. Cifras de ancho fijo para que "10" no quede descompensado por
 * el 1 estrecho.
 */
@Composable
private fun SkipButton(@DrawableRes icon: Int, back: Boolean, seconds: Int, description: String, compact: Boolean, onClick: () -> Unit) {
    val c = LectorTheme.colors
    val scale = if (compact) 0.8f else 1f
    Box(
        Modifier.size(if (compact) 44.dp else 56.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), description, Modifier.size(40.dp * scale), tint = c.text)
        Text(
            "$seconds",
            style = LectorTheme.type.meta.copy(
                fontSize = 12.sp * scale,
                fontWeight = FontWeight.SemiBold,
                fontFamily = LectorTheme.type.body.fontFamily,
                fontFeatureSettings = "tnum",
            ),
            color = c.text,
            modifier = Modifier.offset(x = (if (back) 1.dp else (-1).dp) * scale, y = 0.5.dp * scale),
        )
    }
}

/** Botón pequeño de la fila inferior: icono 18 y texto mínimo en mono 13. */
@Composable
private fun SmallAction(
    @DrawableRes icon: Int?,
    text: String?,
    description: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .widthIn(min = 44.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(painterResource(icon), null, Modifier.size(18.dp), tint = tint)
        if (text != null) Text(text, style = LectorTheme.type.meta.copy(fontSize = 13.sp), color = tint)
    }
}

/** Libro sin acceso: portada atenuada y tarjeta con "Volver a buscar" y "Quitar". */
@Composable
private fun ColumnScope.Inaccessible(book: Book, coverPath: String?, showCover: Boolean, onRescan: (String) -> Unit, onRemove: () -> Unit) {
    if (showCover) {
        CoverArea(Modifier.weight(1f)) { InaccessibleCover(book, coverPath) }
        InaccessibleHeading(book, Modifier.fillMaxWidth().padding(horizontal = 24.dp))
    } else {
        Column(Modifier.weight(1f).fillMaxWidth().slideDown()) {
            InaccessibleHeading(book, Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 28.dp))
        }
    }
    NotFoundCard(book, onRescan, onRemove, Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 20.dp))
}

/** Libro sin acceso en horizontal: portada atenuada a la izquierda; título y tarjeta a la derecha. */
@Composable
private fun InaccessibleLandscape(book: Book, coverPath: String?, showCover: Boolean, onRescan: (String) -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (showCover) {
            Box(Modifier.fillMaxHeight().widthIn(max = 300.dp), contentAlignment = Alignment.Center) {
                InaccessibleCover(book, coverPath)
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            InaccessibleHeading(book, Modifier.fillMaxWidth())
            NotFoundCard(book, onRescan, onRemove, Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun InaccessibleCover(book: Book, coverPath: String?) {
    var ratio by remember(coverPath) { mutableStateOf(CoverRatio) }
    BookCover(
        coverPath,
        book.customName ?: book.title,
        author = book.author,
        modifier = Modifier.aspectRatio(ratio, matchHeightConstraintsFirst = true).slideDown().alpha(0.35f),
        titleStyle = LectorTheme.type.headline,
        onAspectRatio = { ratio = it },
    )
}

@Composable
private fun InaccessibleHeading(book: Book, modifier: Modifier) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(book.customName ?: book.title, style = t.bookTitle, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val byline = listOfNotNull(book.author, book.narrator).filter { it.isNotBlank() }.distinct().joinToString(" · ")
        if (byline.isNotEmpty()) Text(byline, style = t.body, color = c.textSecondary)
    }
}

/** Tarjeta "No se encuentra el libro" con "Volver a buscar" y "Quitar". */
@Composable
private fun NotFoundCard(book: Book, onRescan: (String) -> Unit, onRemove: () -> Unit, modifier: Modifier) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(R.drawable.ic_alert), null, Modifier.size(20.dp), tint = c.danger)
            Text(stringResource(R.string.book_not_found), style = t.row.copy(fontWeight = FontWeight.SemiBold), color = c.text)
        }
        Text(stringResource(R.string.book_not_found_body), style = t.body.copy(lineHeight = 21.sp), color = c.textSecondary)
        Text(book.path, style = t.meta, color = c.textTertiary)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            TextButton(stringResource(R.string.remove), onRemove)
            PrimaryButton(stringResource(R.string.rescan), { onRescan(book.id) })
        }
    }
}

private val LocalPlayerSlide = staticCompositionLocalOf<PlayerSlide?> { null }

/**
 * Arrastrar hacia abajo la raya, la portada o el hueco sin portada mueve Escuchando entera; solo
 * hacia abajo, a los lados no hay gesto.
 */
@Composable
private fun Modifier.slideDown(): Modifier {
    val slide = LocalPlayerSlide.current ?: return this
    val density = LocalDensity.current
    val distance = with(density) { SlideDistance.toPx() }
    val fling = with(density) { SlideVelocity.toPx() }
    // El dedo, en coordenadas de la raíz: las de la zona se mueven con Escuchando y restarían
    // lo que ya ha bajado.
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    return onGloballyPositioned { coords[0] = it }.pointerInput(slide) {
        val tracker = VelocityTracker()
        var lastY = Float.NaN
        detectVerticalDragGestures(
            onDragStart = {
                tracker.resetTracking()
                lastY = Float.NaN
                slide.startDrag()
            },
            onDragEnd = { slide.release(tracker.calculateVelocity().y, distance, fling) },
            onDragCancel = { slide.release(0f, distance, fling) },
        ) { change, dy ->
            change.consume()
            val y = coords[0]?.takeIf { it.isAttached }?.localToRoot(change.position)?.y ?: return@detectVerticalDragGestures
            slide.drag(if (lastY.isNaN()) dy else y - lastY)
            lastY = y
            tracker.addPosition(change.uptimeMillis, Offset(0f, y))
        }
    }
}
