package codelab.lector.ui.bookmarks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codelab.lector.R
import codelab.lector.container
import codelab.lector.data.db.TagUse
import codelab.lector.ui.components.IconAction
import codelab.lector.ui.components.LectorDialog
import codelab.lector.ui.components.LectorSheet
import codelab.lector.ui.components.LocalUndoState
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.TextButton
import codelab.lector.ui.settings.SettingsPage
import codelab.lector.ui.theme.LectorTheme

/**
 * Ajustes › Gestionar tags (lienzo `Settings-Tags`, `Merge-Tag`; design.md › Marcadores › D):
 * cada tag con su número de marcadores y su ⋮: Renombrar, Unir con otro tag y Borrar tag.
 */
@Composable
fun TagsSettingsScreen(onBack: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val store = LocalContext.current.container.bookmarks
    val tags by remember { store.observeTags() }.collectAsStateWithLifecycle(null)
    val undo = LocalUndoState.current
    val deletedText = stringResource(R.string.tag_deleted)
    var renaming by remember { mutableStateOf<TagUse?>(null) }
    var merging by remember { mutableStateOf<TagUse?>(null) }
    val list = tags ?: return

    SettingsPage(stringResource(R.string.settings_tags), onBack) {
        if (list.isEmpty()) {
            Text(
                stringResource(R.string.no_tags),
                style = t.body,
                color = c.textSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        list.forEach { tag ->
            TagRow(
                tag,
                onRename = { renaming = tag },
                onMerge = { merging = tag },
                onDelete = { undo.show(deletedText, store.deleteTag(tag.id)) },
                canMerge = list.size > 1,
            )
        }
    }

    renaming?.let { tag ->
        TagRenameDialog(tag, onSave = { store.renameTag(tag.id, it) }, onDismiss = { renaming = null })
    }
    merging?.let { tag ->
        MergeTagSheet(tag, list.filter { it.id != tag.id }, onMerge = { store.mergeTag(tag.id, it) }, onDismiss = { merging = null })
    }
}

@Composable
private fun TagRow(tag: TagUse, onRename: () -> Unit, onMerge: () -> Unit, onDelete: () -> Unit, canMerge: Boolean) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(if (open) c.surface else c.background)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(tag.name, style = t.meta.copy(fontSize = 14.sp), color = c.text, modifier = Modifier.weight(1f))
        Text("${tag.uses}", style = t.meta, color = c.textSecondary)
        Box {
            IconAction(
                painterResource(R.drawable.ic_more_vert),
                stringResource(R.string.tag_options, tag.name),
                { open = true },
                tint = if (open) c.text else c.textSecondary,
                iconSize = 18.dp,
            )
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                offset = DpOffset(0.dp, 4.dp),
                shape = RoundedCornerShape(8.dp),
                containerColor = c.popup,
                border = BorderStroke(1.dp, c.track),
                modifier = Modifier.width(230.dp),
            ) {
                MenuItem(stringResource(R.string.rename)) {
                    open = false
                    onRename()
                }
                if (canMerge) {
                    MenuItem(stringResource(R.string.merge_with_tag)) {
                        open = false
                        onMerge()
                    }
                }
                MenuItem(stringResource(R.string.delete_tag), danger = true) {
                    open = false
                    onDelete()
                }
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, danger: Boolean = false, onClick: () -> Unit) {
    val c = LectorTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = LectorTheme.type.body, color = if (danger) c.danger else c.text)
    }
}

/** Renombrar tag: diálogo con campo, Cancelar y Guardar (como Renombrar libro). */
@Composable
private fun TagRenameDialog(tag: TagUse, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    var value by remember { mutableStateOf(TextFieldValue(tag.name, TextRange(0, tag.name.length))) }
    val focus = remember { FocusRequester() }
    val label = stringResource(R.string.name)
    fun save() {
        if (value.text.isNotBlank()) onSave(value.text)
        onDismiss()
    }
    LectorDialog(
        title = stringResource(R.string.rename),
        onDismiss = onDismiss,
        buttons = {
            TextButton(stringResource(R.string.cancel), onDismiss)
            PrimaryButton(stringResource(R.string.save), ::save, enabled = value.text.isNotBlank())
        },
    ) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = t.row.copy(color = c.text, fontFamily = t.meta.fontFamily),
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
                .semantics { contentDescription = label },
            decorationBox = { inner ->
                Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) { inner() }
            },
        )
        Text(stringResource(R.string.rename_tag_note), style = t.body.copy(fontSize = 13.sp, lineHeight = 19.sp), color = c.textSecondary)
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** Unir con otro tag (lienzo `Merge-Tag`): elección única del tag destino; el original desaparece. */
@Composable
private fun MergeTagSheet(tag: TagUse, others: List<TagUse>, onMerge: (Long) -> Unit, onDismiss: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    var chosen by remember { mutableStateOf<Long?>(null) }
    LectorSheet(onDismiss, scrollable = false) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.merge_tag_title, tag.name), style = t.sheetTitle, color = c.text)
            Text(stringResource(R.string.merge_tag_note, tag.name), style = t.secondary.copy(lineHeight = 19.sp), color = c.textSecondary)
        }
        LazyColumn(Modifier.weight(1f, fill = false).padding(top = 6.dp)) {
            items(others, key = { it.id }) { other ->
                val on = other.id == chosen
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable(role = Role.RadioButton) { chosen = other.id }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Radio(on)
                    Text(other.name, style = t.meta.copy(fontSize = 14.sp), color = c.text, modifier = Modifier.weight(1f))
                    Text("${other.uses}", style = t.meta, color = c.textSecondary)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            TextButton(stringResource(R.string.cancel), onDismiss)
            PrimaryButton(stringResource(R.string.merge), {
                chosen?.let(onMerge)
                onDismiss()
            }, enabled = chosen != null)
        }
    }
}

/** Opción única: círculo de 20 con borde; elegida, punto en acento. */
@Composable
private fun Radio(on: Boolean) {
    val c = LectorTheme.colors
    Box(Modifier.size(20.dp).border(1.5.dp, if (on) c.accent else c.outline, CircleShape), contentAlignment = Alignment.Center) {
        if (on) Box(Modifier.size(10.dp).background(c.accent, CircleShape))
    }
}
