package codelab.lector.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import codelab.lector.AppContainer
import codelab.lector.R
import codelab.lector.bookmarks.BookmarkGroup
import codelab.lector.data.backup.Backup
import codelab.lector.data.backup.BackupJson
import codelab.lector.library.displayTitle
import codelab.lector.ui.bookmarks.BookmarkFilter
import codelab.lector.ui.bookmarks.ExportSheet
import codelab.lector.ui.bookmarks.filtered
import codelab.lector.ui.bookmarks.safeFileName
import codelab.lector.ui.components.OutlineButton
import codelab.lector.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Resultado de la última copia guardada, en la nota de su fila hasta salir de la pantalla. */
enum class SaveResult { SAVED, FAILED }

class DataViewModel(private val app: AppContainer) : ViewModel() {
    val groups: StateFlow<List<BookmarkGroup>> = app.bookmarks.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val import = BackupImportFlow(app, viewModelScope)

    private val _saved = MutableStateFlow<SaveResult?>(null)
    val saved: StateFlow<SaveResult?> = _saved.asStateFlow()

    fun export(context: Context, uri: Uri) {
        val resolver = context.applicationContext.contentResolver
        val version = appVersion(context)
        viewModelScope.launch {
            _saved.value = runCatching {
                val backup = app.backup.export(System.currentTimeMillis(), version)
                withContext(Dispatchers.IO) {
                    val out = resolver.openOutputStream(uri, "wt") ?: error("no output stream")
                    out.use { it.write(BackupJson.encodeToString(Backup.serializer(), backup).toByteArray()) }
                }
            }.fold({ SaveResult.SAVED }, { SaveResult.FAILED })
        }
    }
}

/**
 * Ajustes › Datos (design.md › Ajustes › D; lienzo `Settings-Data`): exportar la copia completa o
 * los marcadores como texto, y elegir una copia para combinarla con resumen previo.
 */
@Composable
fun DataScreen(viewModel: DataViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val pending by viewModel.import.pending.collectAsStateWithLifecycle()
    val importNote by viewModel.import.note.collectAsStateWithLifecycle()
    var exportingText by remember { mutableStateOf(false) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.export(context, uri)
    }
    val choose = rememberBackupPicker(viewModel.import)
    val fileName = safeFileName(stringResource(R.string.backup_file_name, "lector", LocalDate.now().toString()))

    SettingsPage(stringResource(R.string.settings_data), onBack) {
        SectionHeader(stringResource(R.string.export))
        SettingRow(
            stringResource(R.string.full_backup),
            note = when (saved) {
                SaveResult.SAVED -> stringResource(R.string.backup_saved)
                SaveResult.FAILED -> stringResource(R.string.backup_failed)
                null -> stringResource(R.string.full_backup_note)
            },
        ) { OutlineButton(stringResource(R.string.export), { save.launch("$fileName.json") }) }
        SettingRow(stringResource(R.string.bookmarks_as_text), note = stringResource(R.string.bookmarks_as_text_note)) {
            OutlineButton(stringResource(R.string.export), { exportingText = true })
        }

        SectionHeader(stringResource(R.string.import_merge))
        SettingRow(stringResource(R.string.choose_backup), note = importNoteText(importNote) ?: stringResource(R.string.choose_backup_note)) {
            OutlineButton(stringResource(R.string.choose), { choose.launch(arrayOf("*/*")) })
        }
        pending?.let {
            ImportSummary(it, onCancel = viewModel.import::cancel, onCombine = viewModel.import::combine, Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp))
        }
    }

    if (exportingText) {
        // Todos los marcadores, como la recopilación sin filtro: sin los de pausa.
        val shown = groups.filtered(BookmarkFilter())
        ExportSheet(
            "${shown.sumOf { it.rows.size }}",
            shown.map { it.book.displayTitle to it.rows },
            safeFileName(stringResource(R.string.bookmarks_file_name, "lector")),
            onDismiss = { exportingText = false },
        )
    }
}
