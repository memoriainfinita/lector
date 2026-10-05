package codelab.lector.ui.player

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.R
import codelab.lector.data.db.Book
import codelab.lector.playback.NowPlaying
import codelab.lector.playback.PlayerAction
import codelab.lector.ui.components.BookCover
import codelab.lector.ui.components.IconAction
import codelab.lector.ui.components.LocalUndoState
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.SeekBar
import codelab.lector.ui.components.SegmentedControl
import codelab.lector.ui.components.TextButton
import codelab.lector.ui.formatDuration
import kotlin.math.roundToInt
import codelab.lector.ui.formatSpeed
import codelab.lector.ui.theme.LectorTheme

private enum class PlayerSheet { MENU, SPEED, SOUND, CHAPTERS }

/** Marco de la portada del lienzo (358 × 411) mientras no hay imagen. */
private const val CoverRatio = 358f / 411f

/** Escuchando (design.md › Decisiones de diseño: reproductor A). */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onMinimize: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCover: (String) -> Unit,
    onShowFolder: (String) -> Unit,
) {
    val playing by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val inaccessible by viewModel.inaccessibleBook.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarkCount.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val showCover = appearance.showCovers
    var sheet by remember { mutableStateOf<PlayerSheet?>(null) }
    val undo = LocalUndoState.current
    val removedText = stringResource(R.string.removed_from_library)

    // Horizontal (más ancha que alta): doble panel, sin cabecera.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val np = playing
        val lost = inaccessible
        val removeLost = { book: Book ->
            undo.show(removedText, viewModel.removeInaccessible(book.id))
            onMinimize()
        }
        if (landscape) {
            when {
                np != null -> LandscapePlayer(
                    np = np,
                    showCover = showCover,
                    skipBack = settings.appSkipBackSec,
                    skipForward = settings.appSkipForwardSec,
                    onAct = viewModel::act,
                    onJump = viewModel::jumpTo,
                    onSegment = viewModel::jumpToSegment,
                    onCover = { onOpenCover(np.bookId) },
                    onMinimize = onMinimize,
                    onChapters = { sheet = PlayerSheet.CHAPTERS },
                    onSpeed = { sheet = PlayerSheet.SPEED },
                    onMenu = { sheet = PlayerSheet.MENU },
                )
                lost != null -> InaccessibleLandscape(lost, viewModel.coverPath(lost.id), showCover, viewModel::rescan, { removeLost(lost) }, onMinimize)
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Header(onMinimize)
                when {
                    np != null -> Player(
                        np = np,
                        showCover = showCover,
                        skipBack = settings.appSkipBackSec,
                        skipForward = settings.appSkipForwardSec,
                        bookmarks = bookmarks,
                        onAct = viewModel::act,
                        onJump = viewModel::jumpTo,
                        onCover = { onOpenCover(np.bookId) },
                        onMinimize = onMinimize,
                        onChapters = { sheet = PlayerSheet.CHAPTERS },
                        onSpeed = { sheet = PlayerSheet.SPEED },
                        onMenu = { sheet = PlayerSheet.MENU },
                    )
                    lost != null -> Inaccessible(lost, viewModel.coverPath(lost.id), showCover, viewModel::rescan, { removeLost(lost) }, onMinimize)
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
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
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
    skipBack: Int,
    skipForward: Int,
    bookmarks: Int,
    onAct: (PlayerAction) -> Unit,
    onJump: (Long) -> Unit,
    onCover: () -> Unit,
    onMinimize: () -> Unit,
    onChapters: () -> Unit,
    onSpeed: () -> Unit,
    onMenu: () -> Unit,
) {
    if (showCover) {
        CoverArea(Modifier.weight(1f)) { PlayerCover(np, onCover, onMinimize) }
        TitleBlock(np, Modifier.fillMaxWidth().padding(horizontal = 24.dp))
    } else {
        // Lienzo "Reproductor sin portadas": el hueco queda en medio; deslizarlo hacia abajo hace Atrás.
        Column(Modifier.weight(1f).fillMaxWidth().swipeDown(onMinimize)) {
            NoCoverTitle(np, Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 28.dp))
        }
    }
    Bars(np, onJump, onChapters, Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp))

    // Controles: anterior, −N, play, +N, siguiente.
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIcon(R.drawable.ic_skip_previous, previousLabel(np), 52.dp, 26.dp) { onAct(PlayerAction.PREVIOUS) }
        SkipButton(R.drawable.ic_replay, 1.dp, skipBack, stringResource(R.string.skip_back_seconds, skipBack)) { onAct(PlayerAction.SKIP_BACK) }
        PlayButton(np, 76.dp, 30.dp, onAct)
        SkipButton(R.drawable.ic_forward, (-1).dp, skipForward, stringResource(R.string.skip_forward_seconds, skipForward)) { onAct(PlayerAction.SKIP_FORWARD) }
        RoundIcon(R.drawable.ic_skip_next, nextLabel(np), 52.dp, 26.dp) { onAct(PlayerAction.NEXT) }
    }

    ActionRow(np, bookmarks, onAct, onSpeed, onMenu, Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 6.dp))
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
    skipBack: Int,
    skipForward: Int,
    onAct: (PlayerAction) -> Unit,
    onJump: (Long) -> Unit,
    onSegment: (Int) -> Unit,
    onCover: () -> Unit,
    onMinimize: () -> Unit,
    onChapters: () -> Unit,
    onSpeed: () -> Unit,
    onMenu: () -> Unit,
) {
    val mono14 = LectorTheme.type.meta.copy(fontSize = 14.sp)
    Row(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().padding(start = 24.dp, top = 20.dp, end = 20.dp, bottom = 20.dp)) {
            val coverMax = (maxWidth - LandscapeGap - LandscapeColumnMin).coerceAtMost(300.dp)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(LandscapeGap)) {
                if (showCover && coverMax >= 96.dp) {
                    Box(Modifier.fillMaxHeight().widthIn(max = coverMax), contentAlignment = Alignment.Center) {
                        PlayerCover(np, onCover, onMinimize)
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    TitleBlock(np, Modifier.fillMaxWidth())
                    Spacer(Modifier.weight(1f))
                    Bars(np, onJump, onChapters, Modifier.fillMaxWidth())
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RoundIcon(R.drawable.ic_skip_previous, previousLabel(np), 44.dp, 22.dp) { onAct(PlayerAction.PREVIOUS) }
                        TextAction("−$skipBack", stringResource(R.string.skip_back_seconds, skipBack), mono14) { onAct(PlayerAction.SKIP_BACK) }
                        PlayButton(np, 56.dp, 24.dp, onAct)
                        TextAction("+$skipForward", stringResource(R.string.skip_forward_seconds, skipForward), mono14) { onAct(PlayerAction.SKIP_FORWARD) }
                        RoundIcon(R.drawable.ic_skip_next, nextLabel(np), 44.dp, 22.dp) { onAct(PlayerAction.NEXT) }
                    }
                    // Sin marcadores del libro: el panel de la derecha los tiene.
                    ActionRow(np, null, onAct, onSpeed, onMenu, Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(LectorTheme.colors.divider))
        SidePanel(np, onSegment, Modifier.width(280.dp).fillMaxHeight())
    }
}

/**
 * Panel derecho en horizontal: pestañas Capítulos (o Archivos) y Marcadores, esta inactiva hasta
 * su función. Los tramos con las filas de la hoja de capítulos; sigue al tramo en curso.
 */
@Composable
private fun SidePanel(np: NowPlaying, onSegment: (Int) -> Unit, modifier: Modifier) {
    val c = LectorTheme.colors
    Column(modifier.padding(top = 16.dp)) {
        SegmentedControl(
            listOf(stringResource(if (np.hasChapters) R.string.chapters else R.string.files), stringResource(R.string.tab_bookmarks)),
            selected = 0,
            onSelect = {},
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            disabled = setOf(1),
        )
        val list = rememberLazyListState(initialFirstVisibleItemIndex = (np.segmentIndex - 2).coerceAtLeast(0))
        // Al cambiar de tramo, si el nuevo no se ve, se desplaza hasta él.
        LaunchedEffect(np.bookId, np.segmentIndex) {
            if (list.layoutInfo.visibleItemsInfo.none { it.index == np.segmentIndex }) {
                list.animateScrollToItem((np.segmentIndex - 2).coerceAtLeast(0))
            }
        }
        LazyColumn(Modifier.weight(1f), state = list) {
            itemsIndexed(np.segments) { i, _ -> SegmentRow(np, i, onSegment, base = c.background, highlight = c.surface) }
        }
    }
}

/** Portada: marco de 358 × 411 hasta conocer la imagen; después, su proporción, sin recortar. */
@Composable
private fun PlayerCover(np: NowPlaying, onCover: () -> Unit, onMinimize: () -> Unit) {
    var ratio by remember(np.coverPath) { mutableStateOf(CoverRatio) }
    BookCover(
        np.coverPath,
        np.title,
        author = np.author,
        modifier = Modifier
            .aspectRatio(ratio, matchHeightConstraintsFirst = true)
            .swipeDown(onMinimize)
            .clickable(onClickLabel = stringResource(R.string.view_cover), role = Role.Image, onClick = onCover),
        titleStyle = LectorTheme.type.headline,
        onAspectRatio = { ratio = it },
    )
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

/** Título, autor · narrador y porcentaje. */
@Composable
private fun TitleBlock(np: NowPlaying, modifier: Modifier) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val mono14 = t.meta.copy(fontSize = 14.sp)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(np.title, style = t.bookTitle, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val byline = listOfNotNull(np.author, np.narrator).filter { it.isNotBlank() }.distinct().joinToString(" · ")
            if (byline.isNotEmpty()) Text(byline, style = t.body, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val percent = if (np.durationMs > 0) (np.positionMs * 100.0 / np.durationMs).roundToInt() else 0
        Text("$percent%", style = mono14, color = c.textSecondary, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Barra del libro con marcas de tramo, tiempos, tramo actual y su barra. */
@Composable
private fun Bars(np: NowPlaying, onJump: (Long) -> Unit, onChapters: () -> Unit, modifier: Modifier) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val mono13 = t.meta.copy(fontSize = 13.sp)
    var bookDrag by remember { mutableStateOf<Float?>(null) }
    var segmentDrag by remember { mutableStateOf<Float?>(null) }
    Column(modifier) {
        val total = np.durationMs.coerceAtLeast(1)
        val bookPos = bookDrag?.let { (it * total).toLong() } ?: np.positionMs
        val marks = remember(np.segments, total) {
            np.segments.drop(1).map { it.startMs.toFloat() / total }
        }
        SeekBar(
            fraction = np.positionMs.toFloat() / total,
            onSeek = { onJump((it * total).toLong()) },
            marks = marks,
            onDrag = { bookDrag = it },
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatDuration(bookPos), style = mono13, color = c.textSecondary, modifier = Modifier.weight(1f))
            Text("−" + formatDuration(np.durationMs - bookPos), style = mono13, color = c.textSecondary)
        }

        val segLength = (np.segmentEndMs - np.segmentStartMs).coerceAtLeast(1)
        val segPos = segmentDrag?.let { (it * segLength).toLong() } ?: (np.positionMs - np.segmentStartMs).coerceIn(0, segLength)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .heightIn(min = 32.dp)
                .let { if (np.hasChapters) it.clickable(role = Role.Button, onClick = onChapters) else it },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (np.hasChapters) {
                    Text(stringResource(R.string.chapter_short, np.segmentIndex + 1), style = t.secondary, color = c.accent)
                    Text(np.segmentTitle, style = t.secondary, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Icon(painterResource(R.drawable.ic_chevron_down), stringResource(R.string.chapters), Modifier.size(14.dp), tint = c.textSecondary)
                } else {
                    Text(np.segmentTitle, style = t.secondary, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text("${formatDuration(segPos)} / ${formatDuration(segLength)}", style = mono13, color = c.textSecondary)
        }
        SeekBar(
            fraction = (np.positionMs - np.segmentStartMs).toFloat() / segLength,
            onSeek = { onJump(np.segmentStartMs + (it * segLength).toLong()) },
            thickness = 3.dp,
            color = c.textSecondary,
            onDrag = { segmentDrag = it },
        )
    }
}

@Composable
private fun previousLabel(np: NowPlaying) =
    stringResource(if (np.hasChapters) R.string.previous_chapter else R.string.previous_file)

@Composable
private fun nextLabel(np: NowPlaying) =
    stringResource(if (np.hasChapters) R.string.next_chapter else R.string.next_file)

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
        // Pausa diferida y marcadores del libro: llegan con sus funciones.
        SmallAction(R.drawable.ic_sleep, null, stringResource(R.string.settings_sleep), c.inactive, enabled = false) {}
        RoundIcon(R.drawable.ic_bookmark, stringResource(R.string.add_bookmark), 44.dp, 20.dp, tint = c.accent) {
            onAct(PlayerAction.ADD_BOOKMARK)
        }
        val speed = formatSpeed(np.speed)
        SmallAction(null, speed, stringResource(R.string.speed_value, speed), c.iconSoft, onClick = onSpeed)
        if (bookmarks != null) {
            SmallAction(
                R.drawable.ic_list, "$bookmarks", stringResource(R.string.book_bookmarks_count, bookmarks), c.inactive, enabled = false,
            ) {}
        }
        SmallAction(R.drawable.ic_more, null, stringResource(R.string.more_options), c.iconSoft, onClick = onMenu)
    }
}

/** Zona de la portada: ocupa el alto libre y la portada se ajusta sin deformarse. */
@Composable
private fun CoverArea(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
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
 * Salto con el número de segundos dentro de la flecha circular. El círculo de cada icono está 1 dp
 * a un lado del centro (la flecha ocupa el otro): [centerX] lleva el número a su centro. Cifras de
 * ancho fijo para que "10" no quede descompensado por el 1 estrecho.
 */
@Composable
private fun SkipButton(@DrawableRes icon: Int, centerX: Dp, seconds: Int, description: String, onClick: () -> Unit) {
    val c = LectorTheme.colors
    Box(
        Modifier.size(56.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), description, Modifier.size(40.dp), tint = c.text)
        Text(
            "$seconds",
            style = LectorTheme.type.meta.copy(fontWeight = FontWeight.SemiBold, fontFamily = LectorTheme.type.body.fontFamily, fontFeatureSettings = "tnum"),
            color = c.text,
            modifier = Modifier.offset(x = centerX, y = 0.5.dp),
        )
    }
}

/** Salto en texto (−30, +30) de Escuchando en horizontal: 44 de lado. */
@Composable
private fun TextAction(text: String, description: String, style: TextStyle, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = style, color = LectorTheme.colors.text)
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
private fun ColumnScope.Inaccessible(book: Book, coverPath: String?, showCover: Boolean, onRescan: (String) -> Unit, onRemove: () -> Unit, onMinimize: () -> Unit) {
    if (showCover) {
        CoverArea(Modifier.weight(1f)) { InaccessibleCover(book, coverPath, onMinimize) }
        InaccessibleHeading(book, Modifier.fillMaxWidth().padding(horizontal = 24.dp))
    } else {
        Column(Modifier.weight(1f).fillMaxWidth().swipeDown(onMinimize)) {
            InaccessibleHeading(book, Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 28.dp))
        }
    }
    NotFoundCard(book, onRescan, onRemove, Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 20.dp))
}

/** Libro sin acceso en horizontal: portada atenuada a la izquierda; título y tarjeta a la derecha. */
@Composable
private fun InaccessibleLandscape(book: Book, coverPath: String?, showCover: Boolean, onRescan: (String) -> Unit, onRemove: () -> Unit, onMinimize: () -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (showCover) {
            Box(Modifier.fillMaxHeight().widthIn(max = 300.dp), contentAlignment = Alignment.Center) {
                InaccessibleCover(book, coverPath, onMinimize)
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            InaccessibleHeading(book, Modifier.fillMaxWidth())
            NotFoundCard(book, onRescan, onRemove, Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun InaccessibleCover(book: Book, coverPath: String?, onMinimize: () -> Unit) {
    var ratio by remember(coverPath) { mutableStateOf(CoverRatio) }
    BookCover(
        coverPath,
        book.customName ?: book.title,
        author = book.author,
        modifier = Modifier.aspectRatio(ratio, matchHeightConstraintsFirst = true).swipeDown(onMinimize).alpha(0.35f),
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

/**
 * Deslizar la portada hacia abajo hace lo mismo que la flecha de la cabecera, al soltar pasado el
 * umbral (el del visor de portada). La portada no se mueve: sola, parecería que se despega.
 */
@Composable
private fun Modifier.swipeDown(onSwipe: () -> Unit): Modifier {
    val action by rememberUpdatedState(onSwipe)
    var drag by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { 120.dp.toPx() }
    return pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragEnd = {
                    if (drag > threshold) action()
                    drag = 0f
                },
                onDragCancel = { drag = 0f },
            ) { change, dy ->
                change.consume()
                drag = (drag + dy).coerceAtLeast(0f)
            }
        }
}
