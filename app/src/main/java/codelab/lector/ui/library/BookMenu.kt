package codelab.lector.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codelab.lector.R
import codelab.lector.data.db.Book
import codelab.lector.data.db.LibraryItem
import codelab.lector.library.displayTitle
import codelab.lector.library.progress
import codelab.lector.ui.components.BookCover
import codelab.lector.ui.components.LectorDialog
import codelab.lector.ui.components.LectorSheet
import codelab.lector.ui.components.LocalUndoState
import codelab.lector.ui.components.MenuRow
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.SheetDivider
import codelab.lector.ui.components.TextButton
import codelab.lector.ui.formatDuration
import codelab.lector.ui.formatSize
import codelab.lector.ui.theme.LectorTheme
import kotlin.math.roundToInt

/*
 * Menú del libro (design.md › Pantallas › Biblioteca › C; lienzo "Menú de un libro").
 */

/** Lo que el menú abre fuera de la Biblioteca. */
class BookMenuActions(
    val onCover: (String) -> Unit,
    val onFolder: (String) -> Unit,
    val onSplit: (String) -> Unit,
    val onMerge: (String) -> Unit,
    val onRename: (String) -> Unit,
    val onOpenWith: (Book) -> Unit,
    val onDelete: (String) -> Unit,
)

@Composable
fun BookMenuSheet(item: LibraryItem, cover: String?, viewModel: LibraryViewModel, actions: BookMenuActions, onDismiss: () -> Unit) {
    val undo = LocalUndoState.current
    val book = item.book
    // Sin archivos (inaccesible o quitado): nada que abrir, separar, unir ni borrar.
    val hasFiles = !book.inaccessible && !book.removed
    val positionReset = stringResource(R.string.position_reset)
    val hiddenFromRecents = stringResource(R.string.hidden_from_recents)
    val removed = stringResource(R.string.removed_from_library)
    val restored = stringResource(R.string.restored_to_library)

    /** Cierra la hoja y después actúa: las pantallas y diálogos se abren sin la hoja encima. */
    fun close(then: () -> Unit) = {
        onDismiss()
        then()
    }

    LectorSheet(onDismiss) {
        Column(Modifier.padding(bottom = 14.dp)) {
            MenuHeader(item, cover, viewModel)
            SheetDivider()
            Column(Modifier.padding(top = 6.dp)) {
                // Marcadores: inactivo hasta su función.
                MenuRow(stringResource(R.string.tab_bookmarks), {}, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_bookmark), trailing = item.bookmarkCount.toString(), enabled = false)
                MenuRow(stringResource(R.string.view_cover), close { actions.onCover(book.id) }, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_image))
                if (hasFiles) {
                    MenuRow(stringResource(R.string.go_to_folder), close { actions.onFolder(book.path) }, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_folder))
                    MenuRow(stringResource(R.string.split_book), close { actions.onSplit(book.id) }, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_split))
                    MenuRow(stringResource(R.string.merge_with_books), close { actions.onMerge(book.id) }, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_merge))
                }
                MenuRow(
                    stringResource(if (book.finished) R.string.mark_unfinished else R.string.mark_finished),
                    close { viewModel.setFinished(book.id, !book.finished) },
                    Modifier.fillMaxWidth(),
                    painterResource(R.drawable.ic_check_circle),
                )
                MenuRow(
                    stringResource(R.string.reset_position),
                    close { undo.show(positionReset, viewModel.resetPosition(item)) },
                    Modifier.fillMaxWidth(),
                    painterResource(R.drawable.ic_reset),
                    enabled = item.positionInBookMs > 0 || book.positionFile != null,
                )
                MenuRow(
                    stringResource(R.string.hide_from_recents),
                    close { undo.show(hiddenFromRecents, viewModel.hideFromRecents(book.id)) },
                    Modifier.fillMaxWidth(),
                    painterResource(R.drawable.ic_remove_circle),
                    enabled = book.lastPlayedAt != null && !book.hiddenFromRecents,
                )
                SheetDivider(Modifier.padding(horizontal = 22.dp, vertical = 4.dp))
                MenuRow(stringResource(R.string.rename), close { actions.onRename(book.id) }, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_edit))
                if (hasFiles) {
                    MenuRow(stringResource(R.string.open_with), close { actions.onOpenWith(book) }, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_open_with))
                    MenuRow(stringResource(R.string.delete_from_phone), close { actions.onDelete(book.id) }, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_delete), danger = true)
                } else if (book.removed) {
                    MenuRow(
                        stringResource(R.string.restore_to_library),
                        close { undo.show(restored, viewModel.setRemoved(book.id, false)) },
                        Modifier.fillMaxWidth(),
                        painterResource(R.drawable.ic_nav_library),
                    )
                } else {
                    MenuRow(
                        stringResource(R.string.remove_from_library),
                        close { undo.show(removed, viewModel.setRemoved(book.id, true)) },
                        Modifier.fillMaxWidth(),
                        painterResource(R.drawable.ic_remove_circle),
                    )
                }
            }
        }
    }
}

/** Portada 52, título, "autor · narrador · serie n" y "posición / total · % · tamaño". */
@Composable
private fun MenuHeader(item: LibraryItem, cover: String?, viewModel: LibraryViewModel) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val book = item.book
    val title = book.displayTitle
    val size by produceState<Long?>(null, book.id) { value = viewModel.sizeBytes(book.id) }
    val series = listOfNotNull(book.series, book.seriesPart).filter { it.isNotBlank() }.joinToString(" ")
    val byline = listOfNotNull(book.author, book.narrator, series).filter { it.isNotBlank() }.distinct().joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BookCover(cover, title, Modifier.size(52.dp), radius = 4.dp, titleStyle = t.label.copy(fontSize = 9.sp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = t.row.copy(fontWeight = FontWeight.SemiBold), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (byline.isNotEmpty()) Text(byline, style = t.body.copy(fontSize = 13.sp), color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(
                    R.string.book_menu_times,
                    formatDuration(item.positionInBookMs),
                    formatDuration(book.totalDurationMs),
                    (item.progress * 100).roundToInt(),
                    size?.let(::formatSize) ?: "…",
                ),
                style = t.meta,
                color = c.textSecondary,
            )
        }
    }
}

/** Borrar del móvil: archivos, tamaño y ruta; no se puede deshacer (lienzo "Borrar del móvil"). */
@Composable
fun DeleteBookDialog(book: Book, path: String, viewModel: LibraryViewModel, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val info by produceState<LibraryViewModel.DeleteInfo?>(null, book.id) { value = viewModel.deleteInfo(book.id) }
    LectorDialog(
        title = stringResource(R.string.delete_book_title, book.displayTitle),
        onDismiss = onDismiss,
        buttons = {
            TextButton(stringResource(R.string.cancel), onDismiss)
            // Sin el recuento aún no se sabe qué se borra: el botón espera.
            if (info != null) PrimaryButton(stringResource(R.string.delete), onConfirm, danger = true)
        },
    ) {
        info?.let {
            Text(
                pluralStringResource(R.plurals.delete_book_body, it.files, it.files, formatSize(it.bytes)),
                style = t.body.copy(lineHeight = 21.sp),
                color = c.textSecondary,
            )
        }
        Text(path, style = t.meta, color = c.textTertiary)
        Text(stringResource(R.string.delete_book_marks), style = t.body.copy(fontSize = 13.sp, lineHeight = 19.sp), color = c.textSecondary)
    }
}

/** Renombrar: solo cambia el nombre en LECTOR (lienzo "Renombrar"). */
@Composable
fun RenameDialog(book: Book, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val current = book.displayTitle
    var value by remember { mutableStateOf(TextFieldValue(current, TextRange(0, current.length))) }
    val focus = remember { FocusRequester() }
    val nameLabel = stringResource(R.string.name)
    fun save() {
        onSave(value.text)
        onDismiss()
    }
    LectorDialog(
        title = stringResource(R.string.rename),
        onDismiss = onDismiss,
        buttons = {
            TextButton(stringResource(R.string.cancel), onDismiss)
            PrimaryButton(stringResource(R.string.save), ::save)
        },
    ) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = t.row.copy(color = c.text),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(c.background, RoundedCornerShape(8.dp))
                .border(1.dp, c.accent, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp)
                .focusRequester(focus)
                .semantics { contentDescription = nameLabel },
            decorationBox = { inner ->
                Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) { inner() }
            },
        )
        Text(stringResource(R.string.rename_note), style = t.body.copy(fontSize = 13.sp, lineHeight = 19.sp), color = c.textSecondary)
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
