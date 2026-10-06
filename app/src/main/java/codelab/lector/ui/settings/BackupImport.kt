package codelab.lector.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
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
import codelab.lector.AppContainer
import codelab.lector.R
import codelab.lector.data.backup.Backup
import codelab.lector.data.backup.BackupRead
import codelab.lector.data.backup.ImportPlan
import codelab.lector.data.backup.readBackup
import codelab.lector.ui.components.LectorSwitch
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.TextButton
import codelab.lector.ui.theme.LectorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Lo último que pasó al elegir o combinar una copia, en la nota de "Elegir copia" o bajo el botón del primer arranque. */
enum class ImportNote { UNREADABLE, NOT_BACKUP, NEWER, MERGED, FAILED }

/** Copia elegida y su resumen, a la espera de Combinar o Cancelar. */
data class PendingImport(val fileName: String, val backup: Backup, val plan: ImportPlan)

private const val LogTag = "LectorData"

/**
 * Elegir una copia y combinarla (design.md › Ajustes › D): Ajustes › Datos y "Importar una copia de
 * otro móvil" del primer arranque. Vive en el [scope] de su ViewModel.
 */
class BackupImportFlow(private val app: AppContainer, private val scope: CoroutineScope) {
    private val _pending = MutableStateFlow<PendingImport?>(null)
    val pending: StateFlow<PendingImport?> = _pending.asStateFlow()

    private val _note = MutableStateFlow<ImportNote?>(null)
    val note: StateFlow<ImportNote?> = _note.asStateFlow()

    /** Lee la copia elegida y prepara el resumen; si no sirve, lo dice en la nota. */
    fun open(context: Context, uri: Uri) {
        val resolver = context.applicationContext.contentResolver
        scope.launch {
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
                    _note.value = null
                    _pending.value = PendingImport(name ?: uri.lastPathSegment.orEmpty(), read.backup, app.importer.plan(read.backup))
                }
                BackupRead.NotBackup -> fail(ImportNote.NOT_BACKUP)
                BackupRead.Newer -> fail(ImportNote.NEWER)
            }
        }
    }

    private fun fail(note: ImportNote) {
        _pending.value = null
        _note.value = note
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
        scope.launch {
            _note.value = runCatching {
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

/** Selector de Android para elegir la copia. Algunos gestores dan a .json otro tipo: se admite cualquiera y se comprueba al leer. */
@Composable
fun rememberBackupPicker(flow: BackupImportFlow): ManagedActivityResultLauncher<Array<String>, Uri?> {
    val context = LocalContext.current
    return rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) flow.open(context, uri)
    }
}

/** Texto de [note]; null si no hay nada que decir. */
@Composable
fun importNoteText(note: ImportNote?): String? = when (note) {
    ImportNote.UNREADABLE -> stringResource(R.string.backup_unreadable)
    ImportNote.NOT_BACKUP -> stringResource(R.string.backup_not_valid)
    ImportNote.NEWER -> stringResource(R.string.backup_newer)
    ImportNote.MERGED -> stringResource(R.string.backup_merged)
    ImportNote.FAILED -> stringResource(R.string.backup_merge_failed)
    null -> null
}

/**
 * Tarjeta del resumen previo (lienzo `Settings-Data`): cuentas, ajustes, Cancelar y Combinar.
 * [settingsByDefault]: "Importar también los ajustes" de entrada (activada en el primer arranque).
 */
@Composable
fun ImportSummary(
    pending: PendingImport,
    onCancel: () -> Unit,
    onCombine: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    settingsByDefault: Boolean = false,
) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    var withSettings by rememberSaveable(pending.fileName) { mutableStateOf(settingsByDefault) }
    val plan = pending.plan
    Column(
        modifier
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
