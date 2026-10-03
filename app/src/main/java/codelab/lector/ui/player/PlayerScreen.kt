package codelab.lector.ui.player

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.SeekBar
import codelab.lector.ui.components.TextButton
import codelab.lector.ui.formatDuration
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
    var sheet by remember { mutableStateOf<PlayerSheet?>(null) }

    Column(Modifier.fillMaxSize()) {
        Header(onMinimize)
        val np = playing
        val lost = inaccessible
        when {
            np != null -> Player(
                np = np,
                skipBack = settings.appSkipBackSec,
                skipForward = settings.appSkipForwardSec,
                bookmarks = bookmarks,
                onAct = viewModel::act,
                onJump = viewModel::jumpTo,
                onCover = { onOpenCover(np.bookId) },
                onChapters = { sheet = PlayerSheet.CHAPTERS },
                onSpeed = { sheet = PlayerSheet.SPEED },
                onMenu = { sheet = PlayerSheet.MENU },
            )
            lost != null -> Inaccessible(lost, viewModel.coverPath(lost.id), viewModel::rescan, viewModel::dismissInaccessible)
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
            onJump = {
                sheet = null
                viewModel.jumpTo(it)
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
    skipBack: Int,
    skipForward: Int,
    bookmarks: Int,
    onAct: (PlayerAction) -> Unit,
    onJump: (Long) -> Unit,
    onCover: () -> Unit,
    onChapters: () -> Unit,
    onSpeed: () -> Unit,
    onMenu: () -> Unit,
) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val mono14 = t.meta.copy(fontSize = 14.sp)
    val mono13 = t.meta.copy(fontSize = 13.sp)
    var bookDrag by remember { mutableStateOf<Float?>(null) }
    var segmentDrag by remember { mutableStateOf<Float?>(null) }

    // Marco de 358 × 411 hasta conocer la imagen; después, su proporción, sin recortar.
    var ratio by remember(np.coverPath) { mutableStateOf(CoverRatio) }
    CoverArea(Modifier.weight(1f)) {
        BookCover(
            np.coverPath,
            np.title,
            Modifier
                .aspectRatio(ratio, matchHeightConstraintsFirst = true)
                .clickable(onClickLabel = stringResource(R.string.view_cover), role = Role.Image, onClick = onCover),
            titleStyle = t.headline,
            onAspectRatio = { ratio = it },
        )
    }

    // Título, autor · narrador y porcentaje.
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(np.title, style = t.bookTitle, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val byline = listOfNotNull(np.author, np.narrator).filter { it.isNotBlank() }.distinct().joinToString(" · ")
            if (byline.isNotEmpty()) Text(byline, style = t.body, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val percent = if (np.durationMs > 0) (np.positionMs * 100 / np.durationMs).toInt() else 0
        Text("$percent%", style = mono14, color = c.textSecondary, modifier = Modifier.padding(top = 6.dp))
    }

    // Barra del libro con marcas de tramo, tiempos, tramo actual y su barra.
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp)) {
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

    // Controles: anterior, −N, play, +N, siguiente.
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val prev = stringResource(if (np.hasChapters) R.string.previous_chapter else R.string.previous_file)
        val next = stringResource(if (np.hasChapters) R.string.next_chapter else R.string.next_file)
        RoundIcon(R.drawable.ic_skip_previous, prev, 52.dp, 26.dp) { onAct(PlayerAction.PREVIOUS) }
        SkipButton(R.drawable.ic_replay, skipBack, stringResource(R.string.skip_back_seconds, skipBack)) { onAct(PlayerAction.SKIP_BACK) }
        val playing = np.playWhenReady
        Box(
            Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(c.accent)
                .clickable(role = Role.Button) { onAct(PlayerAction.PLAY_PAUSE) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                stringResource(if (playing) R.string.pause else R.string.play),
                Modifier.size(30.dp),
                tint = c.onAccent,
            )
        }
        SkipButton(R.drawable.ic_forward, skipForward, stringResource(R.string.skip_forward_seconds, skipForward)) { onAct(PlayerAction.SKIP_FORWARD) }
        RoundIcon(R.drawable.ic_skip_next, next, 52.dp, 26.dp) { onAct(PlayerAction.NEXT) }
    }

    // Fila bajo los controles: pausa diferida, marcar, velocidad, marcadores del libro y ⋯ (su menú sale por abajo).
    Row(
        Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 6.dp),
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
        SmallAction(
            R.drawable.ic_list, "$bookmarks", stringResource(R.string.book_bookmarks_count, bookmarks), c.inactive, enabled = false,
        ) {}
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

/** Salto con el número de segundos dentro de la flecha circular. */
@Composable
private fun SkipButton(@DrawableRes icon: Int, seconds: Int, description: String, onClick: () -> Unit) {
    val c = LectorTheme.colors
    Box(
        Modifier.size(56.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), description, Modifier.size(40.dp), tint = c.text)
        Text(
            "$seconds",
            style = LectorTheme.type.meta.copy(fontWeight = FontWeight.SemiBold, fontFamily = LectorTheme.type.body.fontFamily),
            color = c.text,
            modifier = Modifier.offset(y = 2.dp),
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
private fun ColumnScope.Inaccessible(book: Book, coverPath: String?, onRescan: (String) -> Unit, onRemove: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val title = book.customName ?: book.title
    CoverArea(Modifier.weight(1f)) {
        var ratio by remember(coverPath) { mutableStateOf(CoverRatio) }
        BookCover(
            coverPath,
            title,
            Modifier.aspectRatio(ratio, matchHeightConstraintsFirst = true).alpha(0.35f),
            titleStyle = t.headline,
            onAspectRatio = { ratio = it },
        )
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = t.bookTitle, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val byline = listOfNotNull(book.author, book.narrator).filter { it.isNotBlank() }.distinct().joinToString(" · ")
        if (byline.isNotEmpty()) Text(byline, style = t.body, color = c.textSecondary)
    }
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 20.dp)
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
