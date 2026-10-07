package codelab.lector.ui.bookmarks

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.R
import codelab.lector.bookmarks.BookmarkRow
import codelab.lector.bookmarks.bookmarksText
import codelab.lector.data.db.BookmarkKind
import codelab.lector.data.db.playable
import codelab.lector.library.displayTitle
import codelab.lector.library.fold
import codelab.lector.library.searchTerms
import codelab.lector.ui.components.IconAction
import codelab.lector.ui.components.LectorSheet
import codelab.lector.ui.components.LocalBottomInset
import codelab.lector.ui.components.MenuRow
import codelab.lector.ui.theme.LectorTheme

/**
 * Búsqueda en marcadores (lienzo `Bookmarks-Search`): cada palabra en el título, la nota o un tag,
 * sin distinguir mayúsculas ni acentos; lo encontrado en acento. Sin los de pausa.
 */
@Composable
fun BookmarksSearchScreen(viewModel: BookmarksViewModel, onBack: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val all by viewModel.groups.collectAsStateWithLifecycle()
    val sheets = LocalBookmarkSheets.current
    var query by rememberSaveable { mutableStateOf("") }
    val terms = remember(query) { searchTerms(query) }
    val results = remember(all, terms) {
        if (terms.isEmpty()) emptyList()
        else all.orEmpty()
            .map { g -> g.copy(rows = g.rows.filter { it.bookmark.kind == BookmarkKind.NORMAL && matches(it, terms) }) }
            .filter { it.rows.isNotEmpty() }
    }

    Column(Modifier.fillMaxSize()) {
        SearchHeader(query, { query = it }, onBack)
        if (terms.isNotEmpty()) {
            val count = results.sumOf { it.rows.size }
            Text(
                pluralStringResource(R.plurals.bookmarks_count, count, count) + " · " + stringResource(R.string.search_bookmarks_fields),
                style = t.body.copy(fontSize = 12.sp),
                color = c.textSecondary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
            )
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = LocalBottomInset.current + 20.dp)) {
            results.forEach { group ->
                item(key = "book:" + group.book.id) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(group.book.displayTitle, style = t.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = c.text, modifier = Modifier.weight(1f), maxLines = 1)
                        Text("${group.rows.size}", style = t.meta, color = c.textSecondary)
                    }
                }
                items(group.rows, key = { it.bookmark.id }) { row ->
                    BookmarkItem(
                        row,
                        onEdit = { sheets.edit(row.bookmark.id) },
                        onPlay = if (group.book.playable && row.bookMs != null) {
                            { viewModel.store.play(row) }
                        } else null,
                        onBackground = true,
                        terms = terms,
                    )
                }
            }
        }
    }
}

/** Cada palabra en el título, la nota o algún tag. */
private fun matches(row: BookmarkRow, terms: List<String>): Boolean {
    val text = fold(listOfNotNull(row.bookmark.title, row.bookmark.note).plus(row.tags.map { it.name }).joinToString("\n"))
    return terms.all { it in text }
}

@Composable
private fun SearchHeader(query: String, onQuery: (String) -> Unit, onBack: () -> Unit) {
    val c = LectorTheme.colors
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val label = stringResource(R.string.search_bookmarks)
    Row(
        Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconAction(painterResource(R.drawable.ic_back), stringResource(R.string.close_search), onBack)
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

/**
 * Exportar marcadores (lienzo `Export-Sheet`): vista previa del texto y tres destinos: copiar,
 * guardar como .txt y compartir con otra app. [subtitle]: cuántos y qué filtro, o el libro.
 */
@Composable
internal fun ExportSheet(subtitle: String, groups: List<Pair<String, List<BookmarkRow>>>, fileName: String, onDismiss: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val context = LocalContext.current
    val pauseLabel = stringResource(R.string.settings_sleep)
    val text = remember(groups, pauseLabel) { bookmarksText(groups, pauseLabel) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            onDismiss()
        }
    }
    LectorSheet(onDismiss) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.export_bookmarks), style = t.sheetTitle, color = c.text, modifier = Modifier.weight(1f))
            Text(subtitle, style = t.secondary, color = c.textSecondary, textAlign = TextAlign.End, maxLines = 1)
        }
        Box(
            Modifier
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .background(c.background, RoundedCornerShape(8.dp))
                .border(1.dp, c.track, RoundedCornerShape(8.dp))
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            Text(text, style = t.meta.copy(lineHeight = 19.sp), color = c.textSecondary)
        }
        Column(Modifier.padding(bottom = 14.dp)) {
            MenuRow(stringResource(R.string.copy), {
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(fileName, text))
                onDismiss()
            }, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_copy))
            MenuRow(stringResource(R.string.save_as_file), { save.launch("$fileName.txt") }, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_download), trailing = ".txt")
            MenuRow(stringResource(R.string.share_with_app), {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text).putExtra(Intent.EXTRA_SUBJECT, fileName)
                context.startActivity(Intent.createChooser(send, null))
                onDismiss()
            }, Modifier.fillMaxWidth(), painterResource(R.drawable.ic_share))
        }
    }
}

/** Nombre de archivo sin caracteres que Android no admite. */
internal fun safeFileName(name: String) = name.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim()
