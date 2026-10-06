package codelab.lector.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import codelab.lector.AppContainer
import codelab.lector.R
import codelab.lector.data.db.BookFile
import codelab.lector.data.db.LibraryItem
import codelab.lector.library.NaturalOrder
import codelab.lector.library.bookFolders
import codelab.lector.library.displayPath
import codelab.lector.library.displayTitle
import codelab.lector.library.listingFolder
import codelab.lector.ui.components.BookCover
import codelab.lector.ui.components.CheckBox
import codelab.lector.ui.components.IconAction
import codelab.lector.ui.components.LocalBottomInset
import codelab.lector.ui.components.LocalUndoState
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.toneKeyOf
import codelab.lector.ui.formatDuration
import codelab.lector.ui.theme.LectorTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Unir y Separar (design.md › Biblioteca › Unir y Separar; lienzo `Merge-Books`, `Split-Book`).
 * Pantallas completas: guardan la corrección y vuelven; la búsqueda que la aplica sigue en la app.
 */

data class MergeState(
    /** Carpeta donde aparece el libro, como en Carpetas: "Audiobooks / Saga". */
    val folder: String = "",
    /** Libros de esa carpeta, en orden natural de ruta. */
    val books: List<LibraryItem> = emptyList(),
    val covers: Map<String, String> = emptyMap(),
    val selected: Set<String> = emptySet(),
    val showCovers: Boolean = true,
)

class MergeBooksViewModel(private val app: AppContainer, bookId: String, storageRoots: List<String>) : ViewModel() {
    private val _state = MutableStateFlow(MergeState(selected = setOf(bookId)))
    val state: StateFlow<MergeState> = _state.asStateFlow()

    init {
        viewModelScope.launch { app.appearance.settings.collect { a -> _state.update { it.copy(showCovers = a.showCovers) } } }
        viewModelScope.launch {
            val items = app.database.books().observeLibrary().first()
            val book = items.firstOrNull { it.book.id == bookId }?.book ?: return@launch
            val folders = bookFolders(items.map { it.book })
            val folder = listingFolder(book, folders)
            val books = items
                .filter { !it.book.inaccessible && !it.book.removed && listingFolder(it.book, folders) == folder }
                .sortedWith(compareBy(NaturalOrder) { it.book.path })
            val covers = books.mapNotNull { item -> app.covers.file(item.book.id).takeIf { it.exists() }?.let { item.book.id to it.path } }.toMap()
            _state.update { it.copy(folder = displayPath(folder, storageRoots), books = books, covers = covers) }
        }
    }

    fun toggle(bookId: String) {
        _state.update { s -> s.copy(selected = if (bookId in s.selected) s.selected - bookId else s.selected + bookId) }
    }

    suspend fun merge(): Long? = app.corrections.merge(state.value.selected.toList())

    fun undo(id: Long) = app.corrections.undo(id)
}

@Composable
fun MergeBooksScreen(viewModel: MergeBooksViewModel, onClose: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val undo = LocalUndoState.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val merged = stringResource(R.string.books_merged)
    val count = state.selected.size

    Column(Modifier.fillMaxSize()) {
        CorrectionTopBar(
            title = pluralStringResource(R.plurals.selected_count, count, count),
            action = stringResource(R.string.merge_as_one),
            enabled = count >= 2,
            onClose = onClose,
        ) {
            scope.launch {
                val id = viewModel.merge()
                onClose()
                if (id != null) undo.show(merged, onUndo = { viewModel.undo(id) })
            }
        }
        Text(
            state.folder,
            style = t.body,
            color = c.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.StartEllipsis,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
        )
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(state.books, key = { it.book.id }) { item ->
                MergeRow(item, state.covers[item.book.id], item.book.id in state.selected, state.showCovers) { viewModel.toggle(item.book.id) }
            }
            item {
                if (state.books.size == 1) {
                    Text(stringResource(R.string.merge_alone), style = t.secondary, color = c.textSecondary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp))
                }
                CorrectionNote(stringResource(R.string.merge_note), Modifier.padding(top = 18.dp))
                Spacer(Modifier.height(LocalBottomInset.current))
            }
        }
    }
}

@Composable
private fun MergeRow(item: LibraryItem, cover: String?, checked: Boolean, showCover: Boolean, onToggle: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val book = item.book
    val title = book.displayTitle
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (checked) Modifier.background(c.surface) else Modifier)
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CheckBox(checked)
        if (showCover) BookCover(cover, title, Modifier.size(44.dp), radius = 4.dp, titleStyle = t.label.copy(fontSize = 8.sp), toneKey = toneKeyOf(book.author, title))
        Column(Modifier.weight(1f)) {
            Text(title, style = t.row, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(formatDuration(book.totalDurationMs), style = t.meta, color = c.textSecondary)
        }
    }
}

data class SplitState(
    val files: List<BookFile> = emptyList(),
    /** Archivos con los que empieza un libro nuevo; el primero no cuenta, siempre empieza uno. */
    val starts: Set<String> = emptySet(),
)

class SplitBookViewModel(private val app: AppContainer, private val bookId: String) : ViewModel() {
    private val _state = MutableStateFlow(SplitState())
    val state: StateFlow<SplitState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.update { it.copy(files = app.database.books().files(bookId)) } }
    }

    fun toggle(file: String) {
        if (file == state.value.files.firstOrNull()?.relativePath) return
        _state.update { s -> s.copy(starts = if (file in s.starts) s.starts - file else s.starts + file) }
    }

    suspend fun split() {
        val s = state.value
        app.corrections.split(bookId, s.files.map { it.relativePath }.filter { it in s.starts })
    }
}

@Composable
fun SplitBookScreen(viewModel: SplitBookViewModel, onClose: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val books = state.starts.size + 1

    Column(Modifier.fillMaxSize()) {
        CorrectionTopBar(
            // Sin marcas, el nombre de la pantalla: "Separar en 1 libro" no dice nada.
            title = if (books < 2) stringResource(R.string.split_book) else pluralStringResource(R.plurals.split_into, books, books),
            action = stringResource(R.string.split),
            enabled = books >= 2,
            onClose = onClose,
        ) {
            scope.launch {
                viewModel.split()
                onClose()
            }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            item {
                Text(stringResource(R.string.split_hint), style = t.secondary, color = c.textSecondary, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp))
            }
            var part = 0
            state.files.forEachIndexed { i, file ->
                val first = i == 0
                val starts = first || file.relativePath in state.starts
                if (starts) {
                    part++
                    val n = part
                    item(key = "part-$n") {
                        Text(
                            stringResource(R.string.split_part, n).uppercase(),
                            style = t.section,
                            color = c.accent,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
                        )
                    }
                }
                item(key = file.id) { SplitRow(file, checked = starts, locked = first) { viewModel.toggle(file.relativePath) } }
            }
            item {
                CorrectionNote(stringResource(R.string.split_note), Modifier.padding(top = 18.dp))
                Spacer(Modifier.height(LocalBottomInset.current))
            }
        }
    }
}

@Composable
private fun SplitRow(file: BookFile, checked: Boolean, locked: Boolean, onToggle: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(enabled = !locked, role = Role.Checkbox, onClick = onToggle)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CheckBox(checked)
        Text(file.relativePath, style = t.meta.copy(fontSize = 13.sp), color = c.text, maxLines = 1, overflow = TextOverflow.MiddleEllipsis, modifier = Modifier.weight(1f))
        Text(formatDuration(file.durationMs), style = t.meta, color = c.textSecondary)
    }
}

/** Cabecera de selección: ×, título y botón principal (lienzo: fondo de superficie, 60). */
@Composable
private fun CorrectionTopBar(title: String, action: String, enabled: Boolean, onClose: () -> Unit, onAction: () -> Unit) {
    val c = LectorTheme.colors
    Row(
        Modifier.fillMaxWidth().height(60.dp).background(c.surface).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconAction(painterResource(R.drawable.ic_close), stringResource(R.string.cancel), onClose, iconSize = 20.dp)
        Text(title, style = LectorTheme.type.sheetTitle, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        PrimaryButton(action, onAction, Modifier.padding(end = 6.dp), enabled = enabled)
    }
}

@Composable
private fun CorrectionNote(text: String, modifier: Modifier = Modifier) {
    val c = LectorTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Text(
        text,
        style = LectorTheme.type.secondary.copy(lineHeight = 19.sp),
        color = c.textSecondary,
        modifier = modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .border(1.dp, c.divider, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}
