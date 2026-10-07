package codelab.lector.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codelab.lector.R
import codelab.lector.data.db.LibraryItem
import codelab.lector.data.db.playable
import codelab.lector.library.FolderClass
import codelab.lector.library.FolderContent
import codelab.lector.library.FolderRow
import codelab.lector.library.FolderSubtitle
import codelab.lector.library.LibraryFilter
import codelab.lector.library.classOf
import codelab.lector.library.displayTitle
import codelab.lector.library.parentOf
import codelab.lector.library.progress
import codelab.lector.library.ruleFor
import codelab.lector.library.status
import codelab.lector.ui.components.BookCover
import codelab.lector.ui.components.toneKeyOf
import codelab.lector.ui.components.IconAction
import codelab.lector.ui.components.LectorSheet
import codelab.lector.ui.components.LocalBottomInset
import codelab.lector.ui.components.SheetDivider
import codelab.lector.ui.formatDuration
import codelab.lector.ui.theme.LectorTheme
import kotlin.math.roundToInt

/*
 * Vista Carpetas de la Biblioteca (design.md › Pantallas › Biblioteca › B).
 */

/** Almacenamiento al que pertenece una carpeta, para la ruta de arriba. */
class StorageRoots(val primary: String, val all: List<String>)

/**
 * Contenido de una carpeta con la ruta arriba. Atrás y la flecha suben un nivel, sin pasar de la
 * carpeta de la biblioteca (o de la lista de carpetas, si hay varias).
 */
@Composable
fun FoldersView(
    content: FolderContent,
    roots: List<String>,
    storage: StorageRoots,
    loadedBookId: String?,
    covers: Map<String, String>,
    showCovers: Boolean,
    onFolder: (String?) -> Unit,
    onBook: (LibraryItem) -> Unit,
    onFolderOptions: (String) -> Unit,
    onBookOptions: (LibraryItem) -> Unit,
    /** Cabecera que se desplaza con la lista (horizontal). */
    top: (@Composable () -> Unit)?,
) {
    val path = content.path
    val up: String? = when {
        path == null -> null
        path in roots -> if (roots.size > 1) "" else null
        else -> parentOf(path)
    }
    // "" = la lista de carpetas de la biblioteca (raíz con varias).
    BackHandler(enabled = up != null) { onFolder(up?.ifEmpty { null }) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalBottomInset.current + 20.dp)) {
        if (top != null) item(key = "header") { top() }
        if (path != null) item(key = "path") { PathBar(path, storage, canGoUp = up != null) { onFolder(up?.ifEmpty { null }) } }
        else item(key = "space") { Spacer(Modifier.height(12.dp)) }
        items(content.folders, key = { "f:" + it.path }) { row ->
            FolderRowItem(row, onOpen = { onFolder(row.path) }, onOptions = { onFolderOptions(row.path) })
        }
        items(content.books, key = { "b:" + it.book.id }) { item ->
            BookRowItem(
                item,
                cover = covers[item.book.id],
                showCover = showCovers,
                loaded = item.book.id == loadedBookId,
                onOpen = { onBook(item) },
                onOptions = { onBookOptions(item) },
            )
        }
    }
}

@Composable
private fun PathBar(path: String, storage: StorageRoots, canGoUp: Boolean, onUp: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val root = storage.all.filter { path == it || path.startsWith("$it/") }.maxByOrNull { it.length }
    val rootLabel = stringResource(if (root == null || root == storage.primary) R.string.storage_primary else R.string.storage_sd)
    val segments = (if (root == null) path else path.removePrefix(root)).split('/').filter { it.isNotEmpty() }
    Row(
        Modifier.fillMaxWidth().padding(start = if (canGoUp) 12.dp else 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp).height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (canGoUp) IconAction(painterResource(R.drawable.ic_back), stringResource(R.string.go_up), onUp, iconSize = 20.dp)
        Text(
            buildAnnotatedString {
                append((listOf(rootLabel) + segments.dropLast(1)).joinToString(" / ") + " / ")
                withStyle(SpanStyle(color = c.text, fontWeight = FontWeight.Medium)) { append(segments.lastOrNull().orEmpty()) }
            },
            style = t.body,
            color = c.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.StartEllipsis,
        )
    }
}

@Composable
private fun FolderRowItem(row: FolderRow, onOpen: () -> Unit, onOptions: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val subtitle = when (val s = row.subtitle) {
        FolderSubtitle.Author -> stringResource(R.string.folder_author)
        is FolderSubtitle.Books -> pluralStringResource(R.plurals.folder_books, s.count, s.count)
        is FolderSubtitle.Class -> stringResource(R.string.folder_subtitle_class, stringResource(className(s.folderClass)), s.works)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(48.dp).background(c.surface, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_folder), null, Modifier.size(22.dp), tint = c.textSecondary)
        }
        Column(Modifier.weight(1f)) {
            Text(row.name, style = t.row.copy(fontWeight = FontWeight.Medium), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = t.meta.copy(fontFamily = t.secondary.fontFamily), color = c.textSecondary)
        }
        IconAction(painterResource(R.drawable.ic_more_vert), stringResource(R.string.folder_options, row.name), onOptions, tint = c.textSecondary, iconSize = 20.dp)
    }
    RowDivider()
}

@Composable
private fun BookRowItem(item: LibraryItem, cover: String?, showCover: Boolean, loaded: Boolean, onOpen: () -> Unit, onOptions: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val book = item.book
    val title = book.displayTitle
    val total = formatDuration(book.totalDurationMs)
    val status = item.status
    val meta = when {
        book.inaccessible -> stringResource(R.string.book_missing)
        book.unsupportedFormat != null -> stringResource(R.string.book_unsupported, book.unsupportedFormat)
        status == LibraryFilter.FINISHED -> stringResource(R.string.book_row_finished, total)
        status == LibraryFilter.NOT_STARTED -> stringResource(R.string.book_row_not_started, total)
        item.bookmarkCount > 0 -> stringResource(
            R.string.book_row_marks, formatDuration(item.positionInBookMs), total,
            pluralStringResource(R.plurals.bookmarks_count, item.bookmarkCount, item.bookmarkCount),
        )
        else -> stringResource(R.string.book_row_progress, formatDuration(item.positionInBookMs), total, (item.progress * 100).roundToInt())
    }
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (loaded) Modifier.background(c.surface.copy(alpha = 0.6f)) else Modifier)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.listen_to, title), onClick = onOpen)
            .alpha(if (book.inaccessible || book.unsupportedFormat != null) 0.4f else 1f)
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showCover) BookCover(cover, title, Modifier.size(48.dp), radius = 4.dp, titleStyle = t.label.copy(fontSize = 9.sp), toneKey = toneKeyOf(book.author, title))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = t.row.copy(fontWeight = FontWeight.Medium),
                color = if (loaded) c.accent else c.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(meta, style = t.meta, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (status == LibraryFilter.IN_PROGRESS && book.playable) ProgressBar(item.progress, if (loaded) c.accent else c.textSecondary)
        }
        IconAction(painterResource(R.drawable.ic_more_vert), stringResource(R.string.book_options, title), onOptions, tint = c.textSecondary, iconSize = 20.dp)
    }
    RowDivider()
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(LectorTheme.colors.divider))
}

fun className(folderClass: FolderClass): Int = when (folderClass) {
    FolderClass.BOOKS -> R.string.folder_class_books
    FolderClass.EPISODES -> R.string.folder_class_episodes
    FolderClass.ALBUMS -> R.string.folder_class_albums
    FolderClass.SESSIONS -> R.string.folder_class_sessions
}

private fun classDescription(folderClass: FolderClass): Int = when (folderClass) {
    FolderClass.BOOKS -> R.string.folder_class_books_desc
    FolderClass.EPISODES -> R.string.folder_class_episodes_desc
    FolderClass.ALBUMS -> R.string.folder_class_albums_desc
    FolderClass.SESSIONS -> R.string.folder_class_sessions_desc
}

/**
 * Hoja "Clase de carpeta": cabecera con nombre, ruta y archivos, cuatro opciones y la nota. Se
 * aplica al tocar, sin Listo.
 */
@Composable
fun FolderClassSheet(
    path: String,
    viewModel: LibraryViewModel,
    rules: List<codelab.lector.data.db.FolderRule>,
    onDismiss: () -> Unit,
) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    var files by remember(path) { mutableStateOf<Int?>(null) }
    LaunchedEffect(path) { files = viewModel.fileCount(path) }
    val current = classOf(ruleFor(path, rules))
    LectorSheet(onDismiss) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(44.dp).background(c.background, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_folder), null, Modifier.size(22.dp), tint = c.textSecondary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(path.substringAfterLast('/'), style = t.sheetTitle, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val count = files
                val where = viewModel.relativePath(path)
                Text(
                    if (count == null) where else where + " · " + pluralStringResource(R.plurals.files_count, count, count),
                    style = t.meta,
                    color = c.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SheetDivider()
        Text(
            stringResource(R.string.folder_class).uppercase(),
            style = t.section,
            color = c.textSecondary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        )
        FolderClass.entries.forEach { option ->
            val selected = option == current
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .selectable(selected, role = Role.RadioButton) {
                        viewModel.setFolderClass(path, option)
                        onDismiss()
                    }
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(20.dp).border(2.dp, if (selected) c.accent else c.inactive, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Box(Modifier.size(10.dp).background(c.accent, CircleShape))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(className(option)), style = t.row, color = c.text)
                    Text(stringResource(classDescription(option)), style = t.secondary, color = c.textSecondary)
                }
            }
        }
        Text(
            stringResource(R.string.folder_class_note),
            style = t.secondary,
            color = c.textSecondary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 18.dp),
        )
    }
}
