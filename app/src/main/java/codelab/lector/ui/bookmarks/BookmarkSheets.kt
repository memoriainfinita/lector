package codelab.lector.ui.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.R
import codelab.lector.bookmarks.BookmarkRow
import codelab.lector.bookmarks.BookmarkStore
import codelab.lector.container
import codelab.lector.data.db.BookmarkKind
import codelab.lector.ui.components.CheckBox
import codelab.lector.ui.components.LectorSheet
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.SegmentedControl
import codelab.lector.ui.components.SheetDivider
import codelab.lector.ui.components.SheetLink
import codelab.lector.ui.components.TagChip
import codelab.lector.ui.components.TextButton
import codelab.lector.ui.formatDuration
import codelab.lector.ui.theme.LectorTheme

/**
 * Hojas de marcadores, comunes a toda la app (design.md › Pantallas › Marcadores › A): marcadores
 * del libro, hoja de marcador y lista de tags. Se apilan: Atrás o tocar fuera cierra la de arriba y
 * vuelve a la anterior.
 */
@Stable
class BookmarkSheetsState internal constructor(initial: List<Sheet>) {
    sealed interface Sheet {
        data class Book(val bookId: String) : Sheet
        data class Edit(val bookmarkId: String, val created: Boolean) : Sheet
        data class Tags(val bookmarkId: String, val search: Boolean) : Sheet
    }

    internal val stack = mutableStateListOf<Sheet>().apply { addAll(initial) }

    /** Marcadores de un libro; sustituye lo que hubiera abierto. */
    fun showBook(bookId: String) {
        stack.clear()
        stack.add(Sheet.Book(bookId))
    }

    /** Hoja de marcador, encima de la de su libro si está abierta. [created]: con "Deshacer". */
    fun edit(bookmarkId: String, created: Boolean = false) {
        stack.removeAll { it !is Sheet.Book }
        stack.add(Sheet.Edit(bookmarkId, created))
    }

    internal fun tags(bookmarkId: String, search: Boolean) = stack.add(Sheet.Tags(bookmarkId, search))

    internal fun back() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    fun close() = stack.clear()

    internal companion object {
        /** Se conserva al girar la pantalla. */
        val saver = Saver<BookmarkSheetsState, List<String>>(
            save = { state ->
                state.stack.map {
                    when (it) {
                        is Sheet.Book -> "B|${it.bookId}"
                        is Sheet.Edit -> "E|${it.bookmarkId}|${it.created}"
                        is Sheet.Tags -> "T|${it.bookmarkId}|${it.search}"
                    }
                }
            },
            restore = { saved ->
                BookmarkSheetsState(
                    saved.mapNotNull { line ->
                        val p = line.split('|')
                        when (p[0]) {
                            "B" -> Sheet.Book(p[1])
                            "E" -> Sheet.Edit(p[1], p[2].toBoolean())
                            "T" -> Sheet.Tags(p[1], p[2].toBoolean())
                            else -> null
                        }
                    },
                )
            },
        )
    }
}

@Composable
fun rememberBookmarkSheetsState() = rememberSaveable(saver = BookmarkSheetsState.saver) { BookmarkSheetsState(emptyList()) }

val LocalBookmarkSheets = staticCompositionLocalOf<BookmarkSheetsState> { error("BookmarkSheetsState sin proveer") }

/**
 * Pone la hoja de arriba de la pila. [onSeeAll]: "Ver todos los marcadores"; [onManageTags]:
 * "Gestionar" en la lista de tags.
 */
@Composable
fun BookmarkSheetsHost(state: BookmarkSheetsState, onSeeAll: () -> Unit, onManageTags: () -> Unit) {
    val store = LocalContext.current.container.bookmarks
    // Marcar desde la app con "Abrir la hoja al marcar": la hoja del marcador nuevo.
    LaunchedEffect(store) { store.created.collect { state.edit(it, created = true) } }
    when (val top = state.stack.lastOrNull()) {
        is BookmarkSheetsState.Sheet.Book -> BookBookmarksSheet(
            top.bookId,
            store,
            onEdit = { state.edit(it) },
            onSeeAll = {
                state.close()
                onSeeAll()
            },
            onDismiss = state::back,
            onPlayed = state::close,
        )
        is BookmarkSheetsState.Sheet.Edit -> BookmarkSheet(
            top.bookmarkId,
            top.created,
            store,
            onTags = { search -> state.tags(top.bookmarkId, search) },
            onDismiss = state::back,
        )
        is BookmarkSheetsState.Sheet.Tags -> TagPickerSheet(
            top.bookmarkId,
            top.search,
            store,
            onManage = {
                state.close()
                onManageTags()
            },
            onDismiss = state::back,
        )
        null -> Unit
    }
}

/** "1:47:57 · CD02 · 31:58": posición en el libro, tramo y posición en el tramo. */
private fun positionLine(row: BookmarkRow): String? {
    val bookMs = row.bookMs ?: return null
    return listOfNotNull(formatDuration(bookMs), row.segmentTitle, row.segmentMs?.let(::formatDuration)).joinToString(" · ")
}

/**
 * Hoja de marcador (lienzo `Bookmark-Sheet`): crea y edita. Título y nota se guardan al cerrarla, por
 * cualquier camino; los tags, al tocarlos.
 */
@Composable
private fun BookmarkSheet(
    id: String,
    created: Boolean,
    store: BookmarkStore,
    onTags: (search: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val row by remember(id) { store.observeOne(id) }.collectAsState(initial = null)
    val tags by remember { store.observeTags() }.collectAsStateWithLifecycle(emptyList())
    var loaded by rememberSaveable(id) { mutableStateOf(false) }
    var deleted by remember(id) { mutableStateOf(false) }
    var title by rememberSaveable(id) { mutableStateOf("") }
    var note by rememberSaveable(id) { mutableStateOf("") }
    LaunchedEffect(row) {
        val r = row ?: return@LaunchedEffect
        if (!loaded) {
            title = r.bookmark.title.orEmpty()
            note = r.bookmark.note.orEmpty()
            loaded = true
        }
    }
    val save by rememberUpdatedState {
        if (loaded && !deleted) store.setText(id, title, note)
    }
    DisposableEffect(id) { onDispose { save() } }

    LectorSheet(onDismiss) {
        Column(
            Modifier.imePadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(if (created) c.accent else c.track),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(if (created) R.drawable.ic_check else R.drawable.ic_bookmark),
                        null,
                        Modifier.size(18.dp),
                        tint = if (created) c.onAccent else c.accent,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(if (created) R.string.bookmark_saved else R.string.bookmark),
                        style = t.row.copy(fontWeight = FontWeight.SemiBold),
                        color = c.text,
                    )
                    val line = row?.let(::positionLine) ?: if (row != null) stringResource(R.string.file_missing) else ""
                    Text(line, style = t.meta.copy(fontSize = 13.sp), color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (created) {
                    TextButton(stringResource(R.string.undo), {
                        deleted = true
                        store.delete(id)
                        onDismiss()
                    })
                }
            }

            Field(stringResource(R.string.bookmark_title), title, { title = it }, stringResource(R.string.bookmark_untitled), singleLine = true)
            Field(stringResource(R.string.bookmark_note), note, { note = it }, stringResource(R.string.bookmark_note_hint), singleLine = false)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.tags_most_used), style = t.secondary, color = c.textSecondary, modifier = Modifier.weight(1f))
                    SheetLink(stringResource(R.string.all_tags), { onTags(false) })
                }
                val selected = row?.tags.orEmpty().map { it.tagId }.toSet()
                // Los más usados y, aunque no lo sean, los que ya lleva.
                val shown = tags.take(MostUsedTags) + tags.drop(MostUsedTags).filter { it.id in selected }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    shown.forEach { tag ->
                        val on = tag.id in selected
                        TagChip(tag.name, on, { store.setTag(id, tag.id, !on) })
                    }
                    TagChip(stringResource(R.string.add_tag), false, { onTags(true) }, dashed = true)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PrimaryButton(stringResource(R.string.done), onDismiss)
            }
        }
    }
}

/** Tags que la hoja de marcador muestra sin abrir la lista completa. */
private const val MostUsedTags = 6

/** Campo con etiqueta encima (lienzo `Bookmark-Sheet`): 44 de alto, o 3 líneas para la nota. */
@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, placeholder: String, singleLine: Boolean) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = t.secondary, color = c.textSecondary)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            maxLines = if (singleLine) 1 else 8,
            textStyle = t.row.copy(color = c.text, lineHeight = 21.sp),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .background(c.background, shape)
                .border(1.dp, if (focused) c.accent else c.track, shape)
                .onFocusChanged { focused = it.isFocused }
                .padding(horizontal = 12.dp, vertical = 11.dp)
                .semantics { contentDescription = label },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, style = t.row, color = c.textTertiary)
                    inner()
                }
            },
        )
    }
}

/**
 * Lista completa de tags para un marcador (lienzo `Tag-Picker`): búsqueda, orden por uso o A–Z,
 * tocar pone o quita; "Crear «x»" si lo escrito no existe. [search]: llega desde "+ tag", con el
 * teclado abierto.
 */
@Composable
private fun TagPickerSheet(id: String, search: Boolean, store: BookmarkStore, onManage: () -> Unit, onDismiss: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val row by remember(id) { store.observeOne(id) }.collectAsState(initial = null)
    val tags by remember { store.observeTags() }.collectAsStateWithLifecycle(emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var byName by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val selected = row?.tags.orEmpty().map { it.tagId }.toSet()
    val q = query.trim()
    val shown = tags
        .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
        .let { list -> if (byName) list.sortedBy { it.name.lowercase() } else list }
    val canCreate = q.isNotEmpty() && tags.none { it.name.equals(q, ignoreCase = true) }
    fun create() {
        if (!canCreate) return
        store.addTagByName(id, q)
        query = ""
    }

    LectorSheet(onDismiss, scrollable = false) {
        Column(Modifier.imePadding().padding(bottom = 14.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tags), style = t.sheetTitle, color = c.text, modifier = Modifier.weight(1f))
                SheetLink(stringResource(R.string.manage), onManage)
            }
            SearchField(query, { query = it }, stringResource(R.string.search_tags), focus, onDone = ::create, modifier = Modifier.padding(horizontal = 20.dp))
            SegmentedControl(
                listOf(stringResource(R.string.tags_by_use), stringResource(R.string.tags_a_z)),
                selected = if (byName) 1 else 0,
                onSelect = { byName = it == 1 },
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
            )
            LazyColumn(Modifier.weight(1f, fill = false)) {
                if (canCreate) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button, onClick = ::create).padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_add), null, Modifier.size(20.dp), tint = c.accent)
                            Text(stringResource(R.string.create_tag, q), style = t.row, color = c.accent)
                        }
                    }
                }
                items(shown, key = { it.id }) { tag ->
                    val on = tag.id in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clickable(role = Role.Checkbox) { store.setTag(id, tag.id, !on) }
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CheckBox(on)
                        Text(tag.name, style = t.meta.copy(fontSize = 14.sp), color = c.text, modifier = Modifier.weight(1f))
                        Text("${tag.uses}", style = t.meta, color = c.textSecondary)
                    }
                }
                if (tags.isEmpty() && q.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.no_tags_yet),
                            style = t.body,
                            color = c.textSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp), horizontalArrangement = Arrangement.End) {
                PrimaryButton(stringResource(R.string.done), onDismiss)
            }
        }
    }
    LaunchedEffect(Unit) { if (search) focus.requestFocus() }
}

@Composable
private fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    focus: FocusRequester,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val shape = RoundedCornerShape(6.dp)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = t.row.copy(color = c.text),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .background(c.background, shape)
            .border(1.dp, c.track, shape)
            .focusRequester(focus)
            .semantics { contentDescription = placeholder },
        decorationBox = { inner ->
            Row(Modifier.heightIn(min = 40.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(R.drawable.ic_search), null, Modifier.size(18.dp), tint = c.textSecondary)
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = t.row, color = c.textTertiary)
                    inner()
                }
            }
        },
    )
}

/**
 * Marcadores del libro (lienzo `Book-Bookmarks`): "Marcar aquí" y "estás aquí" si es el libro
 * cargado; el play de cada fila, si sus archivos están. Tocar una fila la edita.
 */
@Composable
private fun BookBookmarksSheet(
    bookId: String,
    store: BookmarkStore,
    onEdit: (String) -> Unit,
    onSeeAll: () -> Unit,
    onDismiss: () -> Unit,
    onPlayed: () -> Unit,
) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val app = LocalContext.current.container
    val rows by remember(bookId) { store.observeBook(bookId) }.collectAsStateWithLifecycle(emptyList())
    val book by produceState<codelab.lector.data.db.Book?>(null, bookId) { value = app.database.books().get(bookId) }
    val playing by app.playback.state.collectAsStateWithLifecycle()
    val here = playing?.takeIf { it.bookId == bookId }?.positionMs
    val playable = book?.let { !it.inaccessible && !it.removed } == true
    val normal = rows.count { it.bookmark.kind == BookmarkKind.NORMAL }

    LectorSheet(onDismiss, scrollable = false) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.tab_bookmarks), style = t.sheetTitle, color = c.text)
                book?.let {
                    Text("${it.customName ?: it.title} · $normal", style = t.secondary, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (here != null) {
                MarkHereButton { store.markHere() }
            }
        }
        LazyColumn(Modifier.weight(1f, fill = false)) {
            val hereIndex = here?.let { pos -> rows.indexOfFirst { (it.bookMs ?: Long.MAX_VALUE) > pos }.let { if (it < 0) rows.size else it } }
            rows.forEachIndexed { i, row ->
                if (i == hereIndex) item(key = "here") { YouAreHere(here) }
                item(key = row.bookmark.id) {
                    BookmarkItem(
                        row,
                        onEdit = { onEdit(row.bookmark.id) }.takeIf { row.bookmark.kind == BookmarkKind.NORMAL },
                        onPlay = if (playable && row.bookMs != null) {
                            {
                                store.play(row)
                                onPlayed()
                            }
                        } else null,
                    )
                }
            }
            if (hereIndex == rows.size) item(key = "here") { YouAreHere(here) }
            if (rows.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_bookmarks_in_book),
                        style = t.body,
                        color = c.textSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SheetLink(stringResource(R.string.see_all_bookmarks), onSeeAll, Modifier.weight(1f, fill = false))
            Spacer(Modifier.weight(1f))
            // Exportar llega con su hoja (entrega C).
            Text(
                stringResource(R.string.export),
                style = t.secondary,
                color = c.inactive,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

/** "Marcar aquí": secundario con borde e icono de marcador en acento. */
@Composable
internal fun MarkHereButton(onClick: () -> Unit) {
    val c = LectorTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier
            .heightIn(min = 36.dp)
            .clip(shape)
            .border(1.dp, c.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(R.drawable.ic_bookmark), null, Modifier.size(14.dp), tint = c.accent)
        Text(stringResource(R.string.mark_here), style = LectorTheme.type.secondary, color = c.text)
    }
}

/** Línea "1:47:57 · estás aquí" entre los marcadores. */
@Composable
internal fun YouAreHere(positionMs: Long) {
    val c = LectorTheme.colors
    Row(
        Modifier.fillMaxWidth().background(c.popup).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(6.dp).background(c.accent, CircleShape))
        Text(stringResource(R.string.you_are_here, formatDuration(positionMs)), style = LectorTheme.type.meta, color = c.textSecondary)
    }
}

/**
 * Fila de marcador: posición en el libro, título (o "Sin título"), nota, tags y tramo; el de pausa,
 * con la luna y en gris. [onEdit] null: no se edita (el de pausa). [onPlay] null: sin play.
 */
@Composable
internal fun BookmarkItem(row: BookmarkRow, onEdit: (() -> Unit)?, onPlay: (() -> Unit)?) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val mark = row.bookmark
    val pause = mark.kind == BookmarkKind.PAUSE
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .let { if (onEdit != null) it.clickable(role = Role.Button, onClick = onEdit) else it }
                .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                row.bookMs?.let(::formatDuration) ?: "—",
                style = t.meta.copy(fontSize = 13.sp),
                color = if (pause) c.textSecondary else c.accent,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(top = 1.dp).widthIn(min = 64.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when {
                    pause -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(painterResource(R.drawable.ic_sleep), null, Modifier.size(13.dp), tint = c.textSecondary)
                        Text(stringResource(R.string.pause_marker, ago(mark.createdAt)), style = t.body, color = c.textSecondary)
                    }
                    mark.title != null -> Text(mark.title, style = t.row.copy(fontWeight = FontWeight.Medium), color = c.text)
                    else -> Text(stringResource(R.string.bookmark_untitled), style = t.body, color = c.textTertiary)
                }
                mark.note?.let { Text(it, style = t.secondary.copy(lineHeight = 19.sp), color = c.textSecondary, maxLines = 4, overflow = TextOverflow.Ellipsis) }
                if (row.tags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.tags.forEach { TagLabel(it.name) }
                    }
                }
                Text(
                    row.segmentTitle ?: stringResource(R.string.file_missing),
                    style = t.meta,
                    color = c.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onPlay != null) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.listen_from_here), onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(30.dp).border(1.dp, c.outline, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_play), stringResource(R.string.listen_from_here), Modifier.size(12.dp), tint = c.accent)
                    }
                }
            }
        }
        SheetDivider()
    }
}

/** Tag dentro de una fila: 11 mono, borde fino. */
@Composable
internal fun TagLabel(name: String) {
    val c = LectorTheme.colors
    Text(
        name,
        style = LectorTheme.type.label.copy(fontFamily = LectorTheme.type.meta.fontFamily),
        color = c.text,
        modifier = Modifier.border(1.dp, c.outline, RoundedCornerShape(2.dp)).padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

/** "hace 5 min", "hace 3 h", "hace 2 d". */
@Composable
private fun ago(time: Long): String {
    val minutes = ((System.currentTimeMillis() - time) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 60 -> stringResource(R.string.ago_minutes, minutes.toInt())
        minutes < 48 * 60 -> stringResource(R.string.ago_hours, (minutes / 60).toInt())
        else -> stringResource(R.string.ago_days, (minutes / (24 * 60)).toInt())
    }
}

/**
 * Pestaña Marcadores del panel derecho de Escuchando en horizontal: las filas de la hoja de
 * marcadores del libro, con "estás aquí". Tocar edita; el play salta con "Deshacer".
 */
@Composable
fun BookBookmarksPanel(bookId: String, positionMs: Long, modifier: Modifier = Modifier) {
    val store = LocalContext.current.container.bookmarks
    val sheets = LocalBookmarkSheets.current
    val rows by remember(bookId) { store.observeBook(bookId) }.collectAsStateWithLifecycle(emptyList())
    val hereIndex = rows.indexOfFirst { (it.bookMs ?: Long.MAX_VALUE) > positionMs }.let { if (it < 0) rows.size else it }
    LazyColumn(modifier) {
        rows.forEachIndexed { i, row ->
            if (i == hereIndex) item(key = "here") { YouAreHere(positionMs) }
            item(key = row.bookmark.id) {
                BookmarkItem(
                    row,
                    onEdit = { sheets.edit(row.bookmark.id) }.takeIf { row.bookmark.kind == BookmarkKind.NORMAL },
                    onPlay = if (row.bookMs != null) {
                        { store.play(row) }
                    } else null,
                )
            }
        }
        if (hereIndex == rows.size) item(key = "here") { YouAreHere(positionMs) }
        if (rows.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_bookmarks_in_book),
                    style = LectorTheme.type.body,
                    color = LectorTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }
        }
    }
}
