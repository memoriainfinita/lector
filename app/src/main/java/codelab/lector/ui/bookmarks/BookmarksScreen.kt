package codelab.lector.ui.bookmarks

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.AppContainer
import codelab.lector.R
import codelab.lector.bookmarks.BookmarkGroup
import codelab.lector.bookmarks.BookmarkRow
import codelab.lector.bookmarks.bookmarksText
import codelab.lector.data.db.Book
import codelab.lector.data.db.playable
import codelab.lector.data.db.BookmarkKind
import codelab.lector.data.db.TagUse
import codelab.lector.library.displayTitle
import codelab.lector.ui.components.BookCover
import codelab.lector.ui.components.IconAction
import codelab.lector.ui.components.LocalBottomInset
import codelab.lector.ui.components.LocalUndoState
import codelab.lector.ui.components.TagChip
import codelab.lector.ui.theme.LectorTheme
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Recopilación de marcadores (design.md › Pantallas › Marcadores › B). */
class BookmarksViewModel(private val app: AppContainer) : ViewModel() {
    val store = app.bookmarks

    /** null hasta la primera lectura: sin parpadeo del estado vacío. */
    val groups: StateFlow<List<BookmarkGroup>?> = store.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val tags: StateFlow<List<TagUse>> = store.observeTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val showCovers: StateFlow<Boolean> = app.appearance.settings.map { it.showCovers }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _filter = MutableStateFlow(BookmarkFilter())
    val filter: StateFlow<BookmarkFilter> = _filter.asStateFlow()

    fun setFilter(filter: BookmarkFilter) {
        _filter.value = filter
    }

    fun toggleTag(id: Long) = _filter.value.let { f -> setFilter(f.copy(tagIds = if (id in f.tagIds) f.tagIds - id else f.tagIds + id)) }
    fun toggleUntagged() = _filter.value.let { setFilter(it.copy(untagged = !it.untagged)) }
    fun togglePause() = _filter.value.let { setFilter(it.copy(pause = !it.pause)) }

    fun coverPath(bookId: String): String? = app.covers.file(bookId).takeIf { it.exists() }?.path
}

/**
 * Sin filtro, los marcadores normales; con filtro, los que llevan alguno de sus tags, los sin tag si
 * está "sin tag" y los de pausa solo con "pausa" (design.md › Referencia: Voice).
 */
internal fun BookmarkFilter.matches(row: BookmarkRow): Boolean {
    if (row.bookmark.kind == BookmarkKind.PAUSE) return pause
    if (!active) return true
    return row.tags.any { it.tagId in tagIds } || (untagged && row.tags.isEmpty())
}

internal fun List<BookmarkGroup>.filtered(filter: BookmarkFilter) =
    map { it.copy(rows = it.rows.filter(filter::matches)) }.filter { it.rows.isNotEmpty() }

@Composable
fun BookmarksScreen(viewModel: BookmarksViewModel, onBack: () -> Unit, onSearch: () -> Unit, onManageTags: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val all by viewModel.groups.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val showCovers by viewModel.showCovers.collectAsStateWithLifecycle()
    val sheets = LocalBookmarkSheets.current
    val undo = LocalUndoState.current
    val deletedText = stringResource(R.string.bookmark_deleted)
    val pauseLabel = stringResource(R.string.settings_sleep)
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    val groups = all ?: return
    val empty = groups.none { g -> g.rows.any { it.bookmark.kind == BookmarkKind.NORMAL } } && !filter.active
    val shown = groups.filtered(filter)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(painterResource(R.drawable.ic_back), stringResource(R.string.back), onBack)
            Spacer(Modifier.size(4.dp))
            Text(stringResource(R.string.tab_bookmarks), style = t.subpageTitle, color = c.text, modifier = Modifier.weight(1f))
            if (!empty) {
                IconAction(painterResource(R.drawable.ic_search), stringResource(R.string.search_bookmarks), onSearch)
                IconAction(painterResource(R.drawable.ic_export), stringResource(R.string.export), { exporting = true })
            }
        }
        if (empty) {
            EmptyBookmarks()
            return@Column
        }
        FilterRow(tags, filter, viewModel, onAllTags = { picking = true })
        if (filter.active) FilterSummary(shown.sumOf { it.rows.size }, filter, tags) { viewModel.setFilter(BookmarkFilter()) }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = LocalBottomInset.current + 20.dp)) {
            shown.forEach { group ->
                item(key = "book:" + group.book.id) {
                    BookHeader(group, if (showCovers) viewModel.coverPath(group.book.id) else null, showCovers)
                }
                items(group.rows, key = { it.bookmark.id }) { row ->
                    BookmarkItem(
                        row,
                        onEdit = { sheets.edit(row.bookmark.id) }.takeIf { row.bookmark.kind == BookmarkKind.NORMAL },
                        onPlay = if (group.book.playable && row.bookMs != null) {
                            { viewModel.store.play(row) }
                        } else null,
                        onBackground = true,
                        highlightTags = filter.tagIds,
                        trailing = {
                            BookmarkMenu(
                                onCopy = {
                                    val text = bookmarksText(listOf(group.book.displayTitle to listOf(row)), pauseLabel)
                                    context.getSystemService(ClipboardManager::class.java)
                                        .setPrimaryClip(ClipData.newPlainText(group.book.displayTitle, text))
                                },
                                onDelete = { undo.show(deletedText, viewModel.store.delete(row.bookmark.id)) },
                            )
                        },
                    )
                }
            }
        }
    }

    if (exporting) {
        // Exporta lo que se ve: respeta el filtro activo.
        val count = shown.sumOf { it.rows.size }
        val subtitle = if (filter.active) stringResource(R.string.export_filter_note, count, filterNames(filter, tags).joinToString(", ")) else "$count"
        ExportSheet(
            subtitle,
            shown.map { it.book.displayTitle to it.rows },
            safeFileName(stringResource(R.string.bookmarks_file_name, "lector")),
            onDismiss = { exporting = false },
        )
    }

    if (picking) {
        TagFilterSheet(
            filter,
            viewModel.store,
            onApply = {
                viewModel.setFilter(it)
                picking = false
            },
            onManage = {
                picking = false
                onManageTags()
            },
            onDismiss = { picking = false },
        )
    }
}

/** Fila de filtros de una línea con desplazamiento lateral: lista de tags, todos, tags por uso, sin tag y pausa. */
@Composable
private fun FilterRow(tags: List<TagUse>, filter: BookmarkFilter, viewModel: BookmarksViewModel, onAllTags: () -> Unit) {
    val c = LectorTheme.colors
    LazyRow(
        Modifier.fillMaxWidth().padding(bottom = 6.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            Box(
                Modifier
                    .size(width = 32.dp, height = 28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .border(BorderStroke(1.dp, c.outline), RoundedCornerShape(2.dp))
                    .clickable(role = Role.Button, onClick = onAllTags),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_filter_lines), stringResource(R.string.all_tags), Modifier.size(14.dp), tint = c.text)
            }
        }
        item { TagChip(stringResource(R.string.filter_all), !filter.active, { viewModel.setFilter(BookmarkFilter()) }) }
        items(tags, key = { it.id }) { tag -> TagChip(tag.name, tag.id in filter.tagIds, { viewModel.toggleTag(tag.id) }) }
        item { TagChip(stringResource(R.string.untagged), filter.untagged, viewModel::toggleUntagged, dashed = true) }
        item { TagChip(stringResource(R.string.filter_pause), filter.pause, viewModel::togglePause, leading = painterResource(R.drawable.ic_sleep), dashed = true) }
    }
}

/** "n marcadores con idea, cita" y "Quitar filtro". */
@Composable
private fun FilterSummary(count: Int, filter: BookmarkFilter, tags: List<TagUse>, onClear: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val names = filterNames(filter, tags)
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.filtered_count, pluralStringResource(R.plurals.bookmarks_count, count, count), names.joinToString(", ")),
            style = t.secondary,
            color = c.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        codelab.lector.ui.components.TextButton(stringResource(R.string.remove_filter), onClear, color = c.accent)
    }
}

/** Nombres de lo filtrado: tags, "sin tag" y "pausa". */
@Composable
private fun filterNames(filter: BookmarkFilter, tags: List<TagUse>): List<String> =
    tags.filter { it.id in filter.tagIds }.map { it.name } +
        listOfNotNull(stringResource(R.string.untagged).takeIf { filter.untagged }, stringResource(R.string.filter_pause).takeIf { filter.pause })

/** Cabecera de libro: portada de 28, título y número de marcadores. */
@Composable
private fun BookHeader(group: BookmarkGroup, cover: String?, showCover: Boolean) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showCover) BookCover(cover, group.book.displayTitle, Modifier.size(28.dp), radius = 3.dp, titleStyle = t.label.copy(fontSize = 5.sp))
        Text(
            group.book.displayTitle,
            style = t.body.copy(fontWeight = FontWeight.SemiBold),
            color = if (group.book.playable) c.text else c.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text("${group.rows.size}", style = t.meta, color = c.textSecondary)
    }
}

/** ⋮ de un marcador: Copiar texto y Borrar (con "Deshacer", sin confirmación). */
@Composable
private fun BookmarkMenu(onCopy: () -> Unit, onDelete: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    var open by remember { mutableStateOf(false) }
    Box {
        IconAction(painterResource(R.drawable.ic_more_vert), stringResource(R.string.bookmark_options), { open = true }, tint = c.textSecondary, iconSize = 16.dp, modifier = Modifier.width(36.dp))
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            offset = DpOffset(0.dp, 4.dp),
            shape = RoundedCornerShape(8.dp),
            containerColor = c.popup,
            border = BorderStroke(1.dp, c.track),
            modifier = Modifier.width(210.dp),
        ) {
            listOf(
                Triple(R.drawable.ic_copy, R.string.copy_text, onCopy),
                Triple(R.drawable.ic_delete, R.string.delete, onDelete),
            ).forEach { (icon, label, action) ->
                val danger = label == R.string.delete
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable(role = Role.Button) {
                            open = false
                            action()
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(painterResource(icon), null, Modifier.size(16.dp), tint = if (danger) c.danger else c.iconSoft)
                    Text(stringResource(label), style = t.body, color = if (danger) c.danger else c.text)
                }
            }
        }
    }
}

/** Estado vacío (lienzo `Bookmarks-Empty`): cómo marcar. */
@Composable
private fun EmptyBookmarks() {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp).padding(bottom = LocalBottomInset.current),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(R.drawable.ic_bookmark), null, Modifier.size(40.dp), tint = c.inactive)
        Text(stringResource(R.string.no_bookmarks_title), style = t.sheetTitle, color = c.text)
        Text(stringResource(R.string.no_bookmarks_body), style = t.body.copy(lineHeight = 21.sp), color = c.textSecondary, textAlign = TextAlign.Center)
    }
}
