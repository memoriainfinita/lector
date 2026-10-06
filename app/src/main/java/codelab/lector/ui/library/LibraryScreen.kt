package codelab.lector.ui.library

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import codelab.lector.library.searchHighlights
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import codelab.lector.ui.components.CheckBox
import java.io.File
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.R
import codelab.lector.ui.bookmarks.LocalBookmarkSheets
import codelab.lector.data.db.LibraryItem
import codelab.lector.library.FolderKind
import codelab.lector.library.LibraryEntry
import codelab.lector.library.LibraryFilter
import codelab.lector.library.LibrarySort
import codelab.lector.library.displayTitle
import codelab.lector.library.folderContent
import codelab.lector.library.folderToShow
import codelab.lector.library.progress
import codelab.lector.library.remainingMs
import codelab.lector.library.status
import codelab.lector.ui.components.BookCover
import codelab.lector.ui.components.toneKeyOf
import codelab.lector.ui.components.ListDivider
import codelab.lector.ui.components.HeroButton
import codelab.lector.ui.components.IconAction
import codelab.lector.ui.components.LocalBottomInset
import codelab.lector.ui.components.IconSegmentedControl
import codelab.lector.ui.components.TagChip
import codelab.lector.ui.formatDuration
import codelab.lector.ui.theme.LectorTheme
import kotlin.math.roundToInt

/** Ocupa también los márgenes laterales del contenedor ([horizontal] a cada lado). */
private fun Modifier.bleed(horizontal: androidx.compose.ui.unit.Dp) = layout { measurable, constraints ->
    val extra = (horizontal * 2).roundToPx()
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

/**
 * Pellizco con dos dedos: un nivel por gesto al pasar de un umbral. Mira los eventos antes que la
 * cuadrícula y solo los consume con dos dedos, así el scroll con uno sigue funcionando.
 */
private fun Modifier.pinchToZoom(onZoom: (bigger: Boolean) -> Unit) = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var zoom = 1f
        var done = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } >= 2) {
                zoom *= event.calculateZoom()
                if (!done && (zoom > 1.25f || zoom < 0.8f)) {
                    onZoom(zoom > 1f)
                    done = true
                }
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

/**
 * Biblioteca: cabecera, Libros / Carpetas, "Seguir escuchando", filtros, ordenar, cuadrícula, vista
 * Carpetas, menú ⋮ del libro y búsqueda, que filtra la cuadrícula sin pantalla aparte.
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    pendingFolder: String?,
    onBookmarks: () -> Unit,
    onSettings: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onAddFolder: () -> Unit,
    onOpenCover: (String) -> Unit,
    onSplit: (String) -> Unit,
    onMerge: (String) -> Unit,
    /** Sin sesión de escucha en curso: con ella, el minirreproductor ya enseña el libro. */
    showContinue: Boolean,
    /** La carpeta pedida por "Ir a la carpeta" ya se ha abierto. */
    onFolderShown: () -> Unit,
    storage: StorageRoots,
    /** Ya hay datos que mostrar (libros o biblioteca vacía): quita la pantalla de inicio. */
    onLoaded: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.hasFolders) { if (state.hasFolders != null) onLoaded() }
    val c = LectorTheme.colors
    val t = LectorTheme.type
    var segment by rememberSaveable { mutableIntStateOf(0) }
    // Carpeta abierta en la vista Carpetas; null = raíz.
    var folder by rememberSaveable { mutableStateOf<String?>(null) }
    var classSheetFor by remember { mutableStateOf<String?>(null) }
    // Menú del libro y sus diálogos, por id: muestran siempre el libro al día.
    var menuFor by remember { mutableStateOf<String?>(null) }
    var renameFor by remember { mutableStateOf<String?>(null) }
    var deleteFor by remember { mutableStateOf<String?>(null) }
    // Buscando, el campo suelta el foco al abrir el menú: si no, al cerrar un diálogo (Renombrar)
    // Android se lo devuelve y reabre el teclado.
    val focusManager = LocalFocusManager.current
    fun openMenu(bookId: String) {
        focusManager.clearFocus()
        menuFor = bookId
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val openWithTitle = stringResource(R.string.open_with)
    val deleteFailed = stringResource(R.string.delete_failed)
    val menuActions = BookMenuActions(
        onCover = onOpenCover,
        onFolder = onOpenFolder,
        onSplit = onSplit,
        onMerge = onMerge,
        onFolderClass = { classSheetFor = it },
        onRename = { renameFor = it },
        onOpenWith = { book -> scope.launch { viewModel.fileToOpen(book)?.let { openWith(context, it, openWithTitle) } } },
        onDelete = { deleteFor = it },
    )
    LaunchedEffect(pendingFolder, state.hasFolders) {
        if (pendingFolder != null && state.hasFolders == true) {
            folder = folderToShow(pendingFolder, state.items)
            segment = 1
            onFolderShown()
        }
    }

    // Búsqueda: la cabecera se vuelve campo y la cuadrícula se filtra. Al cerrar, todo como estaba.
    var searching by rememberSaveable { mutableStateOf(false) }
    val query by viewModel.query.collectAsStateWithLifecycle()
    fun closeSearch() {
        searching = false
        viewModel.setQuery("")
    }
    BackHandler(enabled = searching) { closeSearch() }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val headerHeight = if (landscape) 52.dp else 60.dp
    val header = @Composable {
        if (searching) {
            SearchHeader(query, viewModel::setQuery, ::closeSearch, headerHeight)
            if (query.isNotBlank()) SearchCount(state.entries.size)
        } else {
            LibraryHeader(
                withSelector = state.hasFolders == true,
                segment = segment,
                onSegment = { segment = it },
                onSearch = { searching = true },
                onBookmarks = onBookmarks,
                onSettings = onSettings,
                height = headerHeight,
            )
        }
    }

    // Título, selector y buscar en una fila. En horizontal la altura escasea: la cabecera se va
    // con el scroll de la lista para dejar sitio a las portadas.
    // Escaneo discreto (el rápido al abrir): solo la línea, sobre el borde de la cabecera, sin
    // ocupar sitio; así no desplaza la cuadrícula al terminar. El completo, con texto.
    val fullScan = state.scan.running && !state.scan.quiet
    val headerWithScan = @Composable {
        Box {
            Column { header() }
            if (state.scan.running && state.scan.quiet) {
                ScanLine(Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp))
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        val scrollingHeader = landscape && state.hasFolders == true
        if (!scrollingHeader) headerWithScan()
        if (state.hasFolders == false) {
            EmptyLibrary(onAddFolder)
            return@Column
        }
        if (fullScan && !scrollingHeader) ScanProgress(state.scan.found, state.scanFolder)
        val top: (@Composable () -> Unit)? = if (scrollingHeader) {
            {
                headerWithScan()
                if (fullScan) ScanProgress(state.scan.found, state.scanFolder)
            }
        } else null
        when {
            state.hasFolders == null -> Unit
            // Los quitados no tienen sitio en Carpetas: su carpeta puede no existir ya. Buscando, la cuadrícula.
            segment == 1 && !searching -> FoldersView(
                content = folderContent(folder, state.items.filterNot { it.book.removed }, state.roots, state.rules),
                roots = state.roots,
                storage = storage,
                loadedBookId = state.loadedBookId,
                covers = state.covers,
                showCovers = state.showCovers,
                onFolder = { folder = it },
                onBook = { item ->
                    viewModel.open(item.book.id)
                    onOpenPlayer()
                },
                onFolderOptions = { classSheetFor = it },
                onBookOptions = { openMenu(it.book.id) },
                top = top,
            )
            // Una cuadrícula nueva al abrir y al cerrar la búsqueda: las dos empiezan arriba del todo.
            else -> key(searching) {
                BookGrid(state, viewModel, onOpenPlayer, onOpenFolder, { classSheetFor = it }, ::openMenu, landscape, showContinue && !searching, top = top)
            }
        }
    }
    classSheetFor?.let { path -> FolderClassSheet(path, viewModel, state.rules, onDismiss = { classSheetFor = null }) }
    fun itemOf(id: String?) = id?.let { state.items.firstOrNull { item -> item.book.id == it } }
    itemOf(menuFor)?.let { item ->
        BookMenuSheet(item, state.covers[item.book.id], viewModel, menuActions, onDismiss = { menuFor = null }, showCover = state.showCovers)
    }
    itemOf(renameFor)?.let { item ->
        RenameDialog(item.book, onSave = { viewModel.rename(item.book, it) }, onDismiss = { renameFor = null })
    }
    itemOf(deleteFor)?.let { item ->
        DeleteBookDialog(
            item.book,
            path = viewModel.relativePath(item.book.path),
            viewModel = viewModel,
            onConfirm = {
                deleteFor = null
                scope.launch {
                    if (!viewModel.deleteFromPhone(item.book)) Toast.makeText(context, deleteFailed, Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { deleteFor = null },
        )
    }
}

/** "Abrir con…": el archivo por FileProvider, con el selector de Android. */
private fun openWith(context: Context, file: File, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "audio/*"
    val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, type).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(Intent.createChooser(view, title)) }
}

/** Cabecera buscando: flecha atrás, campo y ×, con el teclado abierto (design.md › Biblioteca › D). */
@Composable
private fun SearchHeader(query: String, onQuery: (String) -> Unit, onClose: () -> Unit, height: androidx.compose.ui.unit.Dp) {
    val c = LectorTheme.colors
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val label = stringResource(R.string.search_library)
    Row(
        Modifier.fillMaxWidth().height(height).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconAction(painterResource(R.drawable.ic_back), stringResource(R.string.close_search), onClose)
        Row(
            Modifier
                .weight(1f)
                .height(40.dp)
                .background(c.background, RoundedCornerShape(6.dp))
                .border(1.dp, c.track, RoundedCornerShape(6.dp))
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = LectorTheme.type.row.copy(color = c.text),
                cursorBrush = SolidColor(c.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.weight(1f).focusRequester(focus).semantics { contentDescription = label },
            )
            if (query.isNotEmpty()) {
                Box(
                    Modifier.size(32.dp).clickable(role = Role.Button, onClickLabel = stringResource(R.string.clear_text)) { onQuery("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.clear_text), Modifier.size(16.dp), tint = c.textSecondary)
                }
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** "4 libros · título, autor o carpeta", bajo el campo. */
@Composable
private fun SearchCount(count: Int) {
    Text(
        pluralStringResource(R.plurals.folder_books, count, count) + " · " + stringResource(R.string.search_fields),
        style = LectorTheme.type.body.copy(fontSize = 12.sp),
        color = LectorTheme.colors.textSecondary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 4.dp),
    )
}

/** Lo encontrado en acento. */
@Composable
private fun highlighted(text: String, terms: List<String>): AnnotatedString {
    val accent = LectorTheme.colors.accent
    return buildAnnotatedString {
        append(text)
        searchHighlights(text, terms).forEach { addStyle(SpanStyle(color = accent), it.first, it.last + 1) }
    }
}

@Composable
private fun LibraryHeader(
    withSelector: Boolean,
    segment: Int,
    onSegment: (Int) -> Unit,
    onSearch: () -> Unit,
    onBookmarks: () -> Unit,
    onSettings: () -> Unit,
    height: androidx.compose.ui.unit.Dp,
) {
    val c = LectorTheme.colors
    Row(
        Modifier.fillMaxWidth().height(height).padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.tab_library), style = LectorTheme.type.tabTitle, color = c.text, maxLines = 1)
        Box(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp), contentAlignment = Alignment.CenterEnd) {
            if (withSelector) {
                IconSegmentedControl(
                    listOf(painterResource(R.drawable.ic_nav_library), painterResource(R.drawable.ic_folder)),
                    listOf(stringResource(R.string.library_books), stringResource(R.string.library_folders)),
                    segment,
                    onSegment,
                )
            }
        }
        // Sin menú inferior: Marcadores y Ajustes se abren desde aquí.
        Row {
            if (withSelector) IconAction(painterResource(R.drawable.ic_search), stringResource(R.string.search_library), onSearch)
            IconAction(painterResource(R.drawable.ic_bookmark), stringResource(R.string.tab_bookmarks), onBookmarks, iconSize = 20.dp)
            IconAction(painterResource(R.drawable.ic_settings), stringResource(R.string.settings), onSettings, iconSize = 20.dp)
        }
    }
}

@Composable
private fun BookGrid(
    state: LibraryUiState,
    viewModel: LibraryViewModel,
    onOpenPlayer: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onFolderOptions: (String) -> Unit,
    onBookOptions: (String) -> Unit,
    landscape: Boolean,
    showContinue: Boolean,
    /** Cabecera que se desplaza con la cuadrícula (horizontal), con la línea de búsqueda debajo. */
    top: (@Composable () -> Unit)?,
) {
    val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    val bookmarkSheets = LocalBookmarkSheets.current
    // Con el teclado abierto (buscando), las últimas tarjetas tienen que poder subir por encima de él.
    // La raíz ya deja la barra de navegación; el teclado la tapa, así que solo cuenta lo que sobresale.
    // El minirreproductor queda bajo el teclado: se toma el mayor de los dos huecos, no la suma.
    val density = LocalDensity.current
    val keyboard = with(density) {
        (WindowInsets.ime.getBottom(this) - WindowInsets.navigationBars.getBottom(this)).coerceAtLeast(0).toDp()
    }
    val bottom = maxOf(LocalBottomInset.current, keyboard)
    // Sin portadas (Ajustes › Mostrar portadas): lista, como Simple ABP; sin pellizco.
    val list = !state.showCovers
    // Columnas del pellizco: 1, 2 o 3 en vertical; en horizontal, el doble (mismo tamaño de tarjeta).
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (list) 1 else if (landscape) state.columns * 2 else state.columns),
        modifier = Modifier.fillMaxSize().then(if (list) Modifier else Modifier.pinchToZoom(viewModel::zoom)),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottom + 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(if (list) 0.dp else 18.dp),
    ) {
        if (top != null) {
            item(key = "header", span = full) {
                // A todo el ancho, saltando los márgenes de la cuadrícula: la misma cabecera que fija.
                Column(Modifier.bleed(20.dp)) { top() }
            }
        }
        val continueItem = state.continueItem?.takeIf { showContinue }
        continueItem?.let { item ->
            item(key = "continue", span = full) {
                ContinueCard(
                    item,
                    showCover = state.showCovers,
                    cover = state.covers[item.book.id],
                    playing = state.playing && item.book.id == state.loadedBookId,
                    onOpen = {
                        viewModel.open(item.book.id)
                        onOpenPlayer()
                    },
                    onPlayPause = { viewModel.playPause(item.book.id) },
                    modifier = Modifier.padding(top = if (top != null) 0.dp else 16.dp),
                )
            }
        }
        item(key = "filters", span = full) {
            FilterRow(
                state.filters,
                state.sort,
                state.showUnavailable,
                viewModel::toggleFilter,
                viewModel::setSort,
                viewModel::setShowUnavailable,
                Modifier.padding(top = if (continueItem == null) 10.dp else 2.dp, bottom = if (list) 8.dp else 0.dp),
            )
        }
        items(state.entries, key = { it.key }) { entry ->
            if (list) {
                // A todo el ancho, con su propio margen: las filas llevan separador de lado a lado.
                Column(Modifier.bleed(20.dp)) {
                    when (entry) {
                        is LibraryEntry.BookEntry -> BookListRow(
                            entry.item,
                            loaded = entry.item.book.id == state.loadedBookId,
                            onOpen = if (entry.item.book.removed) {
                                { bookmarkSheets.showBook(entry.item.book.id) }
                            } else {
                                {
                                    viewModel.open(entry.item.book.id)
                                    onOpenPlayer()
                                }
                            },
                            onOptions = { onBookOptions(entry.item.book.id) },
                            terms = state.searchTerms,
                        )
                        is LibraryEntry.FolderEntry -> FolderListRow(
                            entry,
                            onOpen = { onOpenFolder(entry.path) },
                            onOptions = { onFolderOptions(entry.path) },
                            terms = state.searchTerms,
                        )
                    }
                }
                return@items
            }
            when (entry) {
                is LibraryEntry.BookEntry -> BookCard(
                    entry.item,
                    cover = state.covers[entry.item.book.id].takeIf { state.showCovers },
                    loaded = entry.item.book.id == state.loadedBookId,
                    // Quitado: nada que reproducir; abre sus marcadores.
                    onOpen = if (entry.item.book.removed) {
                        { bookmarkSheets.showBook(entry.item.book.id) }
                    } else {
                        {
                            viewModel.open(entry.item.book.id)
                            onOpenPlayer()
                        }
                    },
                    onOptions = { onBookOptions(entry.item.book.id) },
                    terms = state.searchTerms,
                    columns = state.columns,
                )
                is LibraryEntry.FolderEntry -> FolderCard(
                    entry,
                    cover = entry.items.firstNotNullOfOrNull { state.covers[it.book.id] }.takeIf { state.showCovers },
                    onOpen = { onOpenFolder(entry.path) },
                    onOptions = { onFolderOptions(entry.path) },
                    terms = state.searchTerms,
                    columns = state.columns,
                )
            }
        }
        // Mientras busca, dos huecos al final: los libros van apareciendo.
        if (state.scan.running && !state.scan.quiet && !list) items(2, key = { "skeleton$it" }) { SkeletonCard() }
    }
}

@Composable
private fun ContinueCard(
    item: LibraryItem,
    showCover: Boolean,
    cover: String?,
    playing: Boolean,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val title = item.book.displayTitle
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.open_player), onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showCover) {
            BookCover(cover, title, Modifier.size(60.dp), radius = 4.dp, titleStyle = t.label.copy(fontSize = 9.sp), toneKey = toneKeyOf(item.book.author, title))
        }
        Column(Modifier.weight(1f).padding(start = if (showCover) 0.dp else 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.continue_listening).uppercase(), style = t.section, color = c.accent)
            Text(title, style = t.row.copy(fontWeight = FontWeight.SemiBold), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Sin portada, el autor ocupa su sitio.
            val author = item.book.author?.takeIf { it.isNotBlank() }
            if (!showCover && author != null) {
                Text(author, style = t.secondary, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            ProgressBar(item.progress, c.accent)
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(c.accent)
                .clickable(role = Role.Button, onClickLabel = stringResource(if (playing) R.string.pause else R.string.play), onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play), null, Modifier.size(18.dp), tint = c.onAccent)
        }
    }
}

@Composable
private fun FilterRow(
    filters: Set<LibraryFilter>,
    sort: LibrarySort,
    showUnavailable: Boolean,
    onToggle: (LibraryFilter) -> Unit,
    onSort: (LibrarySort) -> Unit,
    onShowUnavailable: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = mapOf(
        LibraryFilter.IN_PROGRESS to R.string.filter_in_progress,
        LibraryFilter.NOT_STARTED to R.string.filter_not_started,
        LibraryFilter.FINISHED to R.string.filter_finished,
    )
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEach { (filter, label) -> TagChip(stringResource(label), filter in filters, { onToggle(filter) }) }
        Spacer(Modifier.weight(1f))
        SortButton(sort, showUnavailable, onSort, onShowUnavailable)
    }
}

/** Popup "Ordenar por" y, debajo, la casilla "No disponibles" (libros quitados). */
@Composable
private fun SortButton(sort: LibrarySort, showUnavailable: Boolean, onSort: (LibrarySort) -> Unit, onShowUnavailable: (Boolean) -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    var open by remember { mutableStateOf(false) }
    val options = listOf(
        LibrarySort.RECENT to R.string.sort_recent,
        LibrarySort.ADDED to R.string.sort_added,
        LibrarySort.TITLE to R.string.sort_title,
        LibrarySort.AUTHOR to R.string.sort_author,
        LibrarySort.REMAINING to R.string.sort_remaining,
    )
    Box {
        IconAction(painterResource(R.drawable.ic_sort), stringResource(R.string.sort), { open = true }, iconSize = 18.dp, modifier = Modifier.offset(x = 12.dp))
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            offset = DpOffset(0.dp, 4.dp),
            shape = RoundedCornerShape(8.dp),
            containerColor = c.popup,
            border = androidx.compose.foundation.BorderStroke(1.dp, c.track),
            modifier = Modifier.width(240.dp),
        ) {
            Text(
                stringResource(R.string.sort_by).uppercase(),
                style = t.section,
                color = c.textSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            )
            options.forEach { (option, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable(role = Role.RadioButton) {
                            open = false
                            onSort(option)
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(label), style = t.body, color = c.text, modifier = Modifier.weight(1f))
                    if (option == sort) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp), tint = c.accent)
                }
            }
            Box(Modifier.padding(vertical = 4.dp).fillMaxWidth().height(1.dp).background(c.track))
            // Casilla: se queda abierto el popup, para ver el cambio sin volver a abrirlo.
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .toggleable(showUnavailable, role = Role.Checkbox, onValueChange = onShowUnavailable)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.unavailable), style = t.body, color = c.text, modifier = Modifier.weight(1f))
                CheckBox(showUnavailable)
            }
        }
    }
}

@Composable
private fun BookCard(item: LibraryItem, cover: String?, loaded: Boolean, onOpen: (() -> Unit)?, onOptions: () -> Unit, terms: List<String>, columns: Int) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val book = item.book
    val title = book.displayTitle
    val status = item.status
    val unavailable = book.inaccessible || book.removed
    val meta = cardMeta(item)
    val accent = c.accent
    Column(
        Modifier
            .alpha(if (unavailable) 0.4f else 1f)
            .then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.listen_to, title), onClick = onOpen) else Modifier),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .drawBehind {
                    // Contorno del libro cargado, 2 por fuera de la portada.
                    if (loaded) {
                        val gap = 4.dp.toPx()
                        val stroke = 2.dp.toPx()
                        drawRoundRect(
                            accent,
                            topLeft = Offset(-gap + stroke / 2, -gap + stroke / 2),
                            size = Size(size.width + 2 * gap - stroke, size.height + 2 * gap - stroke),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                            style = Stroke(stroke),
                        )
                    }
                },
        ) {
            BookCover(cover, title, Modifier.fillMaxSize(), titleStyle = coverTitleStyle(columns), author = book.author)
        }
        if (status == LibraryFilter.IN_PROGRESS && !unavailable) ProgressBar(item.progress, if (loaded) c.accent else c.textSecondary)
        else Spacer(Modifier.height(3.dp))
        CardFooter(highlighted(title, terms), meta, optionsLabel = stringResource(R.string.book_options, title), onOptions = onOptions)
    }
}

/** Línea mono de la tarjeta y de la búsqueda: "44% · quedan 3:52:09", "sin empezar · 6:56:54"… */
@Composable
internal fun cardMeta(item: LibraryItem): String {
    val book = item.book
    return when {
        book.removed -> unavailableMeta(item)
        book.inaccessible -> stringResource(R.string.book_missing)
        item.status == LibraryFilter.FINISHED -> stringResource(R.string.book_finished, formatDuration(book.totalDurationMs))
        item.status == LibraryFilter.NOT_STARTED -> stringResource(R.string.book_not_started, formatDuration(book.totalDurationMs))
        else -> stringResource(R.string.book_progress, (item.progress * 100).roundToInt(), formatDuration(item.remainingMs))
    }
}

/** Libro quitado: "no disponible · 3 marcadores", o el porcentaje si no tiene. */
@Composable
internal fun unavailableMeta(item: LibraryItem): String = stringResource(
    R.string.book_unavailable,
    if (item.bookmarkCount > 0) pluralStringResource(R.plurals.bookmarks_count, item.bookmarkCount, item.bookmarkCount)
    else "${(item.progress * 100).roundToInt()}%",
)

@Composable
private fun FolderCard(entry: LibraryEntry.FolderEntry, cover: String?, onOpen: () -> Unit, onOptions: () -> Unit, terms: List<String>, columns: Int) {
    val c = LectorTheme.colors
    val count = entry.items.size
    val meta = when (entry.kind) {
        FolderKind.SESSIONS -> pluralStringResource(R.plurals.folder_sessions, count, count)
        FolderKind.EPISODES -> pluralStringResource(R.plurals.folder_episodes, count, count)
    }
    Column(
        Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.open_folder, entry.name), onClick = onOpen),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Portada apilada: dos cantos por encima y la portada 8 más abajo, en el mismo cuadrado.
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val top = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
            Box(Modifier.padding(horizontal = 8.dp).fillMaxWidth().height(12.dp).background(c.track, top))
            Box(Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp).fillMaxWidth().height(12.dp).background(c.outline, top))
            BookCover(
                cover,
                entry.name,
                Modifier.padding(top = 8.dp).fillMaxSize(),
                titleStyle = coverTitleStyle(columns),
                author = stringResource(folderKindName(entry.kind)),
                toneKey = entry.name,
            )
        }
        Spacer(Modifier.height(3.dp))
        CardFooter(highlighted(entry.name, terms), meta, optionsLabel = stringResource(R.string.folder_options, entry.name), onOptions = onOptions)
    }
}

/** Título de la portada tipográfica según el tamaño de la tarjeta (columnas del pellizco). */
@Composable
private fun coverTitleStyle(columns: Int): TextStyle {
    val t = LectorTheme.type
    return when (columns) {
        1 -> t.bookTitle.copy(fontSize = 26.sp)
        2 -> t.row.copy(fontSize = 18.sp)
        else -> t.body
    }
}

private fun folderKindName(kind: FolderKind) = when (kind) {
    FolderKind.SESSIONS -> R.string.folder_class_sessions
    FolderKind.EPISODES -> R.string.folder_class_episodes
}

/**
 * Fila de la lista sin portadas (lienzo "Biblioteca sin portadas: lista"): título, autor, línea
 * mono y barra si está en curso; ⋮ a la derecha. Cargado: título en acento.
 */
@Composable
private fun BookListRow(item: LibraryItem, loaded: Boolean, onOpen: (() -> Unit)?, onOptions: () -> Unit, terms: List<String>) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val book = item.book
    val title = book.displayTitle
    val unavailable = book.inaccessible || book.removed
    val author = book.author?.takeIf { it.isNotBlank() }
    ListDivider()
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (unavailable) 0.4f else 1f)
            .then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.listen_to, title), onClick = onOpen) else Modifier)
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                highlighted(title, terms),
                style = t.row.copy(fontWeight = FontWeight.Medium),
                color = if (loaded) c.accent else c.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (author != null) Text(highlighted(author, terms), style = t.secondary, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(cardMeta(item), style = t.meta, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.status == LibraryFilter.IN_PROGRESS && !unavailable) {
                Box(Modifier.padding(top = 4.dp)) { ProgressBar(item.progress, if (loaded) c.accent else c.textSecondary) }
            }
        }
        IconAction(painterResource(R.drawable.ic_more_vert), stringResource(R.string.book_options, title), onOptions, tint = c.textSecondary, iconSize = 18.dp)
    }
}

/** Carpeta de Sesiones o Episodios en la lista: icono de carpeta, nombre, "Sesiones · 12" y ⋮. */
@Composable
private fun FolderListRow(entry: LibraryEntry.FolderEntry, onOpen: () -> Unit, onOptions: () -> Unit, terms: List<String>) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    ListDivider()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.open_folder, entry.name), onClick = onOpen)
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_folder), null, Modifier.size(20.dp), tint = c.textSecondary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(highlighted(entry.name, terms), style = t.row.copy(fontWeight = FontWeight.Medium), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.folder_subtitle_class, stringResource(folderKindName(entry.kind)), entry.items.size), style = t.meta, color = c.textSecondary)
        }
        IconAction(painterResource(R.drawable.ic_more_vert), stringResource(R.string.folder_options, entry.name), onOptions, tint = c.textSecondary, iconSize = 18.dp)
    }
}

/** Título, línea mono y ⋮. Sin [onOptions] el ⋮ queda inactivo. */
@Composable
private fun CardFooter(title: AnnotatedString, meta: String, optionsLabel: String, onOptions: (() -> Unit)? = null) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = t.body.copy(fontWeight = FontWeight.Medium), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(meta, style = t.meta, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier
                .width(28.dp)
                .height(40.dp)
                .offset(x = 8.dp)
                .then(if (onOptions != null) Modifier.clickable(role = Role.Button, onClickLabel = optionsLabel, onClick = onOptions) else Modifier),
            contentAlignment = Alignment.TopCenter,
        ) {
            Icon(painterResource(R.drawable.ic_more_vert), optionsLabel, Modifier.padding(top = 2.dp).size(16.dp), tint = if (onOptions != null) c.textSecondary else c.inactive)
        }
    }
}

@Composable
private fun SkeletonCard() {
    val c = LectorTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(c.surface, RoundedCornerShape(6.dp)))
        Box(Modifier.fillMaxWidth(0.7f).height(14.dp).background(c.surface, RoundedCornerShape(2.dp)))
        Box(Modifier.fillMaxWidth(0.5f).height(12.dp).background(c.surface, RoundedCornerShape(2.dp)))
    }
}

@Composable
internal fun ProgressBar(fraction: Float, color: androidx.compose.ui.graphics.Color) {
    val c = LectorTheme.colors
    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(c.track)) {
        Box(Modifier.fillMaxWidth(fraction).height(3.dp).background(color))
    }
}

/** Línea de progreso indeterminada de 2 dp. */
@Composable
internal fun ScanLine(modifier: Modifier = Modifier) {
    val c = LectorTheme.colors
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth().height(2.dp),
        color = c.accent,
        trackColor = c.track,
        gapSize = 0.dp,
    )
}

/** Línea fina de progreso bajo el selector, con el recuento y la carpeta que se recorre. */
@Composable
internal fun ScanProgress(found: Int, folder: String?) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScanLine()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val text = stringResource(R.string.scanning_found, found)
            val number = found.toString()
            val at = text.indexOf(number)
            Text(
                buildAnnotatedString {
                    if (at < 0) append(text) else {
                        append(text.substring(0, at))
                        withStyle(SpanStyle(color = c.text)) { append(number) }
                        append(text.substring(at + number.length))
                    }
                },
                style = t.meta,
                color = c.textSecondary,
                maxLines = 1,
            )
            if (folder != null) {
                Text(folder, style = t.meta, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.StartEllipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onAddFolder: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp).padding(bottom = LocalBottomInset.current),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(R.drawable.ic_folder), null, Modifier.size(40.dp), tint = c.inactive)
        Text(stringResource(R.string.library_empty_title), style = t.sheetTitle, color = c.text)
        Text(stringResource(R.string.library_empty_body), style = t.body.copy(lineHeight = 21.sp), color = c.textSecondary, textAlign = TextAlign.Center)
        HeroButton(stringResource(R.string.add_folder), onAddFolder, Modifier.padding(top = 8.dp))
    }
}
