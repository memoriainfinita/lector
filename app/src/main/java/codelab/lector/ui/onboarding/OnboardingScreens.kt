package codelab.lector.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import codelab.lector.AppContainer
import codelab.lector.R
import codelab.lector.data.db.LibraryFolder
import codelab.lector.library.allFilesAccessIntent
import codelab.lector.library.countAudioFiles
import codelab.lector.library.findAudioFolders
import codelab.lector.library.foldersAfterAdding
import codelab.lector.library.hasStorageAccess
import codelab.lector.library.libraryFolderInfo
import codelab.lector.ui.components.CheckBox
import codelab.lector.ui.components.HeroButton
import codelab.lector.ui.components.LectorSheet
import codelab.lector.ui.components.OutlineButton
import codelab.lector.ui.components.TextButton
import codelab.lector.ui.library.StorageRoots
import codelab.lector.ui.settings.BackupImportFlow
import codelab.lector.ui.settings.ImportSummary
import codelab.lector.ui.settings.importNoteText
import codelab.lector.ui.settings.rememberBackupPicker
import codelab.lector.ui.theme.LectorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/*
 * Primer arranque (design.md › Navegación, Decisiones de diseño; lienzo `Onboarding-Permission`
 * y `Onboarding-Folder`).
 */

/** "Importar una copia de otro móvil" (design.md › Ajustes › D, entrega C). */
class OnboardingImportViewModel(app: AppContainer) : ViewModel() {
    val import = BackupImportFlow(app, viewModelScope)
}

/**
 * Permiso: "Dar permiso" abre el ajuste de acceso a todos los archivos (Android 11+) o pide la
 * lectura clásica (8–10; denegada para siempre, aviso y "Abrir ajustes" con la ficha de la app). Al volver a la app con el
 * acceso concedido, [onGranted]. "Importar una copia de otro móvil": selector de Android y resumen
 * en una hoja, con los ajustes activados de entrada (el móvil nuevo no tiene nada que perder).
 */
@Composable
fun OnboardingPermissionScreen(viewModel: OnboardingImportViewModel, onGranted: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val context = LocalContext.current
    val activity = LocalActivity.current
    val pending by viewModel.import.pending.collectAsStateWithLifecycle()
    val note by viewModel.import.note.collectAsStateWithLifecycle()
    val choose = rememberBackupPicker(viewModel.import)
    LifecycleResumeEffect(Unit) {
        if (hasStorageAccess(context)) onGranted()
        onPauseOrDispose {}
    }
    // Denegado para siempre: Android ya no pregunta. Se queda aquí con el aviso y el botón abre los ajustes.
    var blocked by rememberSaveable { mutableStateOf(false) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        when {
            granted -> onGranted()
            activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE) ->
                blocked = true
        }
    }
    Column(Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, bottom = 32.dp)) {
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.app_name), style = t.secondary.copy(letterSpacing = 0.2.em), color = c.accent)
        Text(stringResource(R.string.onboarding_welcome), style = t.headline.copy(lineHeight = 34.sp), color = c.text, modifier = Modifier.padding(top = 14.dp))
        Text(stringResource(R.string.onboarding_permission_body), style = t.row.copy(lineHeight = 22.sp), color = c.textSecondary, modifier = Modifier.padding(top = 14.dp))
        if (blocked) {
            Text(stringResource(R.string.onboarding_denied), style = t.row.copy(lineHeight = 22.sp), color = c.text, modifier = Modifier.padding(top = 14.dp))
        }
        Spacer(Modifier.weight(1f))
        HeroButton(
            stringResource(if (blocked) R.string.onboarding_open_settings else R.string.onboarding_grant),
            {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> openAllFilesAccess(context)
                    blocked -> openAppDetails(context)
                    else -> request.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            },
            Modifier.fillMaxWidth(),
            fill = true,
        )
        TextButton(stringResource(R.string.onboarding_import), { choose.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth().padding(top = 8.dp))
        importNoteText(note)?.let {
            Text(it, style = t.secondary, color = c.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
    pending?.let {
        LectorSheet(viewModel.import::cancel) {
            ImportSummary(
                it,
                onCancel = viewModel.import::cancel,
                onCombine = viewModel.import::combine,
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                settingsByDefault = true,
            )
        }
    }
}

/** Ajuste de la app; si el móvil no lo tiene, la lista general de acceso a todos los archivos. */
private fun openAllFilesAccess(context: Context) {
    runCatching { context.startActivity(allFilesAccessIntent(context)) }
        .recoverCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        .onFailure { openAppDetails(context) }
}

private fun openAppDetails(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }
}

/** Fila de carpetas. [name]: ruta desde la raíz del almacenamiento. [audio]: null mientras se cuenta. */
data class OnboardingFolder(val path: String, val name: String, val onSd: Boolean, val audio: Int?)

/**
 * Carpetas con audio de niveles 1 y 2, buscadas en segundo plano, más las que se añadan desde el
 * explorador (ya guardadas, llegan marcadas). Se marca de entrada la que tiene nombre de
 * audiolibros. "Empezar" deja como carpetas de la biblioteca exactamente las marcadas.
 */
class OnboardingFoldersViewModel(private val app: AppContainer, private val storage: StorageRoots) : ViewModel() {

    private val _folders = MutableStateFlow<List<OnboardingFolder>?>(null)
    /** null mientras se buscan. */
    val folders: StateFlow<List<OnboardingFolder>?> = _folders.asStateFlow()

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    init {
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) { findAudioFolders(storage.all.map(::File)) }
            val known = _folders.value.orEmpty()
            _folders.value = known + found.filter { f -> known.none { it.path == f.dir.path } }.map { row(it.dir.path, it.audioFiles) }
            _selected.update { it + found.filter { f -> looksLikeAudiobooks(f.dir.name) }.map { it.dir.path } }
        }
        // Las elegidas en el explorador: el explorador ya las guarda; aquí aparecen marcadas.
        viewModelScope.launch {
            app.database.folders().observeFolders().collect { list ->
                val added = list.map { it.path }.filter { p -> _folders.value.orEmpty().none { it.path == p } }
                if (added.isEmpty()) return@collect
                _folders.update { it.orEmpty() + added.map { p -> row(p, null) } }
                _selected.update { it + added }
                added.forEach { p ->
                    launch {
                        val n = withContext(Dispatchers.IO) { countAudioFiles(File(p)) }
                        _folders.update { rows -> rows?.map { if (it.path == p) it.copy(audio = n) else it } }
                    }
                }
            }
        }
    }

    private fun row(path: String, audio: Int?): OnboardingFolder {
        val info = libraryFolderInfo(listOf(path), emptyList(), storage.all, storage.primary).single()
        return OnboardingFolder(path, info.name.ifEmpty { path.substringAfterLast('/') }, info.onSd, audio)
    }

    fun toggle(path: String) = _selected.update { if (path in it) it - path else it + path }

    /** Guarda las marcadas (las contenidas en otra marcada salen) y busca libros; después [onDone]. */
    fun start(onDone: () -> Unit) {
        val wanted = _selected.value.sorted().fold(emptyList<String>(), ::foldersAfterAdding)
        viewModelScope.launch {
            val dao = app.database.folders()
            val current = dao.folders().map { it.path }
            (current - wanted.toSet()).forEach { dao.removeFolder(it) }
            (wanted - current.toSet()).forEach { dao.addFolder(LibraryFolder(it)) }
            app.appScope.launch {
                if (app.scanner.state.value.running) app.scanner.start().join()
                app.scanner.start()
            }
            onDone()
        }
    }
}

private val AudiobookNames = listOf("audiobook", "audio book", "audiolibro", "libros")

private fun looksLikeAudiobooks(name: String) = name.lowercase().let { n -> AudiobookNames.any { it in n } }

/** Lienzo `Onboarding-Folder`. */
@Composable
fun OnboardingFoldersScreen(viewModel: OnboardingFoldersViewModel, onPickFolder: () -> Unit, onStart: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val list = folders
    Column(Modifier.fillMaxSize().padding(top = 56.dp, bottom = 32.dp)) {
        Text(stringResource(R.string.onboarding_folders), style = t.tabTitle, color = c.text, modifier = Modifier.padding(horizontal = 28.dp))
        val subtitle = when {
            list == null -> R.string.onboarding_searching
            list.isEmpty() -> R.string.onboarding_none_found
            else -> R.string.onboarding_found
        }
        Text(stringResource(subtitle), style = t.body, color = c.textSecondary, modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 10.dp, bottom = 18.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(list.orEmpty(), key = { it.path }) { folder ->
                FolderRow(folder, folder.path in selected) { viewModel.toggle(folder.path) }
            }
            item {
                OutlineButton(stringResource(R.string.onboarding_other_folder), onPickFolder, Modifier.padding(horizontal = 28.dp, vertical = 10.dp))
            }
        }
        HeroButton(
            stringResource(R.string.onboarding_start),
            { viewModel.start(onStart) },
            Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            enabled = selected.isNotEmpty(),
            fill = true,
        )
    }
}

@Composable
private fun FolderRow(folder: OnboardingFolder, checked: Boolean, onToggle: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val location = stringResource(if (folder.onSd) R.string.storage_sd else R.string.storage_primary)
    val detail = folder.audio?.let { "$location · ${pluralStringResource(R.plurals.audio_files, it, it)}" } ?: location
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 28.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CheckBox(checked)
        Column(Modifier.weight(1f)) {
            Text(folder.name, style = t.row, color = c.text)
            Text(detail, style = t.secondary, color = c.textSecondary)
        }
    }
}
