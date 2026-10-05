package codelab.lector.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import codelab.lector.AppContainer
import codelab.lector.R
import codelab.lector.data.db.LibraryFolder
import codelab.lector.library.countAudioFiles
import codelab.lector.library.foldersAfterAdding
import codelab.lector.library.inLibrary
import codelab.lector.library.scannableSubfolders
import codelab.lector.ui.components.IconAction
import codelab.lector.ui.components.PrimaryButton
import codelab.lector.ui.components.SegmentedControl
import codelab.lector.ui.library.StorageRoots
import codelab.lector.ui.theme.LectorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Subcarpeta del explorador. [audio]: archivos de audio dentro; null mientras se cuentan. */
data class PickerEntry(val path: String, val name: String, val audio: Int? = null)

/**
 * Explorador de carpetas (design.md › Pantallas › Ajustes › B): recorre el almacenamiento con el
 * acceso a todos los archivos. Las cuentas de audio se hacen en segundo plano, fila a fila.
 */
class FolderPickerViewModel(private val app: AppContainer, val storage: StorageRoots) : ViewModel() {

    private val _dir = MutableStateFlow(storage.primary)
    val dir: StateFlow<String> = _dir.asStateFlow()

    private val _entries = MutableStateFlow<List<PickerEntry>>(emptyList())
    val entries: StateFlow<List<PickerEntry>> = _entries.asStateFlow()

    val libraryFolders: StateFlow<List<String>> = app.database.folders().observeFolders()
        .map { list -> list.map { it.path } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            _dir.collectLatest { dir ->
                val subs = withContext(Dispatchers.IO) { scannableSubfolders(File(dir)) }
                _entries.value = subs.map { PickerEntry(it.path, it.name) }
                for (sub in subs) {
                    val n = withContext(Dispatchers.IO) { countAudioFiles(sub) }
                    _entries.update { list -> list.map { if (it.path == sub.path) it.copy(audio = n) else it } }
                }
            }
        }
    }

    /** Almacenamiento de la carpeta actual. */
    val root: String get() = storage.all.filter { _dir.value == it || _dir.value.startsWith("$it/") }.maxByOrNull { it.length } ?: storage.primary

    fun open(path: String) {
        _dir.value = path
    }

    fun selectRoot(path: String) {
        _dir.value = path
    }

    /** Sube un nivel; en la raíz del almacenamiento no hace nada. */
    fun up() {
        val current = _dir.value
        if (current != root) _dir.value = current.substringBeforeLast('/')
    }

    /**
     * Añade la carpeta actual (sustituye a las de la lista que contiene) y busca libros. Si ya hay
     * una búsqueda en curso, no incluye la carpeta nueva: espera a que termine y lanza otra.
     */
    fun useCurrent() {
        val path = _dir.value
        val current = libraryFolders.value
        val next = foldersAfterAdding(current, path)
        app.appScope.launch {
            val folders = app.database.folders()
            (current - next.toSet()).forEach { folders.removeFolder(it) }
            (next - current.toSet()).forEach { folders.addFolder(LibraryFolder(it)) }
            if (app.scanner.state.value.running) app.scanner.start().join()
            app.scanner.start()
        }
    }
}

/** Lienzo `Folder-Picker`. Atrás sube un nivel; en la raíz del almacenamiento, cierra. */
@Composable
fun FolderPickerScreen(viewModel: FolderPickerViewModel, onClose: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val dir by viewModel.dir.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val library by viewModel.libraryFolders.collectAsStateWithLifecycle()
    val storage = viewModel.storage
    val root = viewModel.root
    val atRoot = dir == root
    BackHandler(enabled = !atRoot) { viewModel.up() }

    fun storageLabel(path: String) = if (path == storage.primary) R.string.storage_primary else R.string.storage_sd
    val rootLabel = stringResource(storageLabel(root))
    val location = (listOf(rootLabel) + dir.removePrefix(root).split('/').filter { it.isNotEmpty() }).joinToString(" / ")

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(painterResource(R.drawable.ic_close), stringResource(R.string.cancel), onClose, iconSize = 20.dp)
            Spacer(Modifier.size(4.dp))
            Text(stringResource(R.string.folder_picker), style = t.subpageTitle, color = c.text)
        }
        if (storage.all.size > 1) {
            val labels = storage.all.map { stringResource(storageLabel(it)) }
            SegmentedControl(
                labels,
                selected = storage.all.indexOf(root),
                onSelect = { viewModel.selectRoot(storage.all[it]) },
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(start = if (atRoot) 20.dp else 12.dp, end = 20.dp, top = 4.dp, bottom = 8.dp).height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!atRoot) IconAction(painterResource(R.drawable.ic_back), stringResource(R.string.go_up), viewModel::up, iconSize = 20.dp)
            Text(location, style = t.body, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.StartEllipsis)
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(entries, key = { it.path }) { entry ->
                PickerRow(entry, inLibrary = inLibrary(entry.path, library)) { viewModel.open(entry.path) }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(location, style = t.secondary, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.StartEllipsis, modifier = Modifier.weight(1f))
            PrimaryButton(
                stringResource(R.string.use_this_folder),
                {
                    viewModel.useCurrent()
                    onClose()
                },
                enabled = !inLibrary(dir, library),
            )
        }
    }
}

@Composable
private fun PickerRow(entry: PickerEntry, inLibrary: Boolean, onOpen: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val note = when {
        inLibrary -> stringResource(R.string.already_in_library)
        entry.audio == null -> "…"
        entry.audio == 0 -> stringResource(R.string.no_audio)
        else -> pluralStringResource(R.plurals.audio_files, entry.audio, entry.audio)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(enabled = !inLibrary, role = Role.Button, onClick = onOpen)
            .alpha(if (inLibrary) 0.5f else 1f)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(painterResource(R.drawable.ic_folder), null, Modifier.size(20.dp), tint = c.textSecondary)
        Column(Modifier.weight(1f)) {
            Text(entry.name, style = t.row, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(note, style = t.meta.copy(fontFamily = t.secondary.fontFamily), color = c.textSecondary)
        }
        if (!inLibrary) Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(16.dp), tint = c.textSecondary)
    }
}
