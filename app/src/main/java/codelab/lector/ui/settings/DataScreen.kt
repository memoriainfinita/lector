package codelab.lector.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import codelab.lector.AppContainer
import codelab.lector.R
import codelab.lector.bookmarks.BookmarkGroup
import codelab.lector.data.backup.Backup
import codelab.lector.data.backup.BackupJson
import codelab.lector.data.backup.BackupRead
import codelab.lector.data.backup.ImportPlan
import codelab.lector.data.backup.readBackup
import codelab.lector.library.displayTitle
import codelab.lector.ui.bookmarks.BookmarkFilter
import codelab.lector.ui.bookmarks.ExportSheet
import codelab.lector.ui.bookmarks.filtered
import codelab.lector.ui.bookmarks.safeFileName
import codelab.lector.ui.components.LectorSwitch
import codelab.lector.ui.components.OutlineButton
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.SectionHeader
import codelab.lector.ui.components.TextButton
import codelab.lector.ui.theme.LectorTheme
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

/** Lo último que pasó al elegir o combinar una copia, en la nota de "Elegir copia". */
enum class ImportNote { UNREADABLE, NOT_BACKUP, NEWER, MERGED, FAILED }

/** Copia elegida y su resumen, a la espera de Combinar o Cancelar. */
data class PendingImport(val fileName: String, val backup: Backup, val plan: ImportPlan)

private const val LogTag = "LectorData"

class DataViewModel(private val app: AppContainer) : ViewModel() {
    val groups: StateFlow<List<BookmarkGroup>> = app.bookmarks.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saved = MutableStateFlow<SaveResult?>(null)
    val saved: StateFlow<SaveResult?> = _saved.asStateFlow()

    private val _pending = MutableStateFlow<PendingImport?>(null)
    val pending: StateFlow<PendingImport?> = _pending.asStateFlow()

    private val _importNote = MutableStateFlow<ImportNote?>(null)
    val importNote: StateFlow<ImportNote?> = _importNote.asStateFlow()

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

    /** Lee la copia elegida y prepara el resumen; si no sirve, lo dice en la nota. */
    fun open(context: Context, uri: Uri) {
        val resolver = context.applicationContext.contentResolver
        viewModelScope.launch {
            val (name, text) = withContext(Dispatchers.IO) {
                val name = runCatching {
                    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
                }.getOrNull()
                name to runCatching { resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }
                    .onFailure { Log.w(LogTag, "cannot read $uri", it) }.getOrNull()
            }
            if (text == null) return@launch fail(ImportNote.UNREADABLE)
            when (val read = readBackup(text) { Log.w(LogTag, "not a backup: $uri", it) }) {
                is BackupRead.Ok -> {
                    _importNote.value = null
                    _pending.value = PendingImport(name ?: uri.lastPathSegment.orEmpty(), read.backup, app.importer.plan(read.backup))
                }
                BackupRead.NotBackup -> fail(ImportNote.NOT_BACKUP)
                BackupRead.Newer -> fail(ImportNote.NEWER)
            }
        }
    }

    private fun fail(note: ImportNote) {
        _pending.value = null
        _importNote.value = note
    }

    fun cancel() {
        _pending.value = null
    }

    /**
     * Combina con un plan nuevo (la base puede haber cambiado desde el resumen). Las correcciones
     * añadidas se aplican con una búsqueda discreta.
     */
    fun combine(withSettings: Boolean) {
        val pending = _pending.value ?: return
        viewModelScope.launch {
            _importNote.value = runCatching {
                val added = app.importer.combine(pending.backup, app.importer.plan(pending.backup), System.currentTimeMillis())
                if (withSettings) {
                    app.importer.importSettings(pending.backup)
                    val language = pending.backup.language
                    if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != language) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
                    }
                }
                if (added > 0) app.scanner.start(quiet = true)
            }.fold({ ImportNote.MERGED }, { ImportNote.FAILED })
            _pending.value = null
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
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val importNote by viewModel.importNote.collectAsStateWithLifecycle()
    var exportingText by remember { mutableStateOf(false) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.export(context, uri)
    }
    // Algunos gestores de archivos dan a .json otro tipo: se admite cualquiera y se comprueba al leer.
    val choose = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.open(context, uri)
    }
    val fileName = safeFileName(stringResource(R.string.backup_file_name, "LECTOR", LocalDate.now().toString()))

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
        SettingRow(
            stringResource(R.string.choose_backup),
            note = when (importNote) {
                ImportNote.UNREADABLE -> stringResource(R.string.backup_unreadable)
                ImportNote.NOT_BACKUP -> stringResource(R.string.backup_not_valid)
                ImportNote.NEWER -> stringResource(R.string.backup_newer)
                ImportNote.MERGED -> stringResource(R.string.backup_merged)
                ImportNote.FAILED -> stringResource(R.string.backup_merge_failed)
                null -> stringResource(R.string.choose_backup_note)
            },
        ) { OutlineButton(stringResource(R.string.choose), { choose.launch(arrayOf("*/*")) }) }
        pending?.let { ImportSummary(it, onCancel = viewModel::cancel, onCombine = viewModel::combine) }
    }

    if (exportingText) {
        // Todos los marcadores, como la recopilación sin filtro: sin los de pausa.
        val shown = groups.filtered(BookmarkFilter())
        ExportSheet(
            "${shown.sumOf { it.rows.size }}",
            shown.map { it.book.displayTitle to it.rows },
            safeFileName(stringResource(R.string.bookmarks_file_name, "LECTOR")),
            onDismiss = { exportingText = false },
        )
    }
}

/** Tarjeta del resumen previo (lienzo `Settings-Data`): cuentas, ajustes, Cancelar y Combinar. */
@Composable
private fun ImportSummary(pending: PendingImport, onCancel: () -> Unit, onCombine: (Boolean) -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    var withSettings by rememberSaveable(pending.fileName) { mutableStateOf(false) }
    val plan = pending.plan
    Column(
        Modifier
            .padding(start = 20.dp, end = 20.dp, top = 10.dp)
            .fillMaxWidth()
            .border(1.dp, c.track, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(pending.fileName, style = t.body.copy(fontWeight = FontWeight.SemiBold), color = c.text)
        Count(stringResource(R.string.backup_new_bookmarks), plan.newBookmarks)
        Count(stringResource(R.string.backup_existing_bookmarks), plan.existingBookmarks)
        Count(stringResource(R.string.backup_newer_positions), plan.newerPositions)
        Count(stringResource(R.string.backup_not_found), plan.notFound)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Switch) { withSettings = !withSettings }
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.import_settings_too), style = t.body, color = c.text, modifier = Modifier.weight(1f))
            LectorSwitch(withSettings, { withSettings = it })
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            TextButton(stringResource(R.string.cancel), onCancel)
            PrimaryButton(stringResource(R.string.merge_backup), { onCombine(withSettings) })
        }
    }
}

@Composable
private fun Count(label: String, value: Int) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = t.body, color = c.textSecondary, modifier = Modifier.weight(1f))
        Text("$value", style = t.meta.copy(fontSize = 14.sp), color = c.text)
    }
}
