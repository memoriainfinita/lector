package codelab.lector.ui.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import codelab.lector.AppContainer
import codelab.lector.R
import codelab.lector.data.db.LibraryFolder
import codelab.lector.data.settings.LastScan
import codelab.lector.library.LibraryFolderInfo
import codelab.lector.library.ScanState
import codelab.lector.library.libraryFolderInfo
import codelab.lector.ui.components.IconAction
import codelab.lector.ui.components.LocalUndoState
import codelab.lector.ui.components.OutlineButton
import codelab.lector.ui.components.SectionHeader
import codelab.lector.ui.library.StorageRoots
import codelab.lector.ui.theme.LectorTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ajustes › Biblioteca: carpetas de la biblioteca, volver a buscar y portadas. */
class LibrarySettingsViewModel(private val app: AppContainer, storage: StorageRoots) : ViewModel() {

    val folders: StateFlow<List<LibraryFolderInfo>> =
        combine(app.database.folders().observeFolders(), app.database.books().observeLibrary()) { folders, items ->
            libraryFolderInfo(folders.map { it.path }, items.map { it.book }, storage.all, storage.primary)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lastScan: StateFlow<LastScan?> = app.librarySettings.lastScan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val scan: StateFlow<ScanState> = app.scanner.state

    val showCovers: StateFlow<Boolean> =
        app.appearance.settings.map { it.showCovers }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Sin búsqueda: la Biblioteca solo muestra los libros de las carpetas de la lista. */
    fun remove(path: String) {
        viewModelScope.launch { app.database.folders().removeFolder(path) }
    }

    /** Deshacer de quitar: en el ámbito de la app, el aviso puede sobrevivir a esta pantalla. */
    fun restore(path: String) {
        app.appScope.launch { app.database.folders().addFolder(LibraryFolder(path)) }
    }

    fun rescan() {
        app.scanner.start(full = true)
    }

    fun setShowCovers(show: Boolean) {
        viewModelScope.launch { app.appearance.setShowCovers(show) }
    }
}

/** Lienzo `Settings-Folders` (design.md › Pantallas › Ajustes › B). Correcciones llegan con Unir y Separar. */
@Composable
fun LibrarySettingsScreen(viewModel: LibrarySettingsViewModel, onBack: () -> Unit, onAddFolder: () -> Unit) {
    val context = LocalContext.current
    val c = LectorTheme.colors
    val undo = LocalUndoState.current
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val lastScan by viewModel.lastScan.collectAsStateWithLifecycle()
    val scan by viewModel.scan.collectAsStateWithLifecycle()
    val showCovers by viewModel.showCovers.collectAsStateWithLifecycle()
    val removedText = stringResource(R.string.folder_removed)

    SettingsPage(stringResource(R.string.settings_library), onBack) {
        SectionHeader(stringResource(R.string.library_folders))
        folders.forEach { folder ->
            FolderRow(folder) {
                viewModel.remove(folder.path)
                undo.show(removedText) { viewModel.restore(folder.path) }
            }
        }
        OutlineButton(stringResource(R.string.add_folder_plus), onAddFolder, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

        val scanning = scan.running && !scan.quiet
        val last = lastScan
        SettingRow(
            stringResource(R.string.rescan_books),
            note = when {
                scanning -> stringResource(R.string.scanning_found, scan.found)
                last != null -> stringResource(
                    R.string.last_scan,
                    DateUtils.formatDateTime(context, last.at, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH),
                    pluralStringResource(R.plurals.folder_books, last.books, last.books),
                )
                else -> null
            },
            enabled = !scanning,
            onClick = viewModel::rescan,
        ) {
            Icon(painterResource(R.drawable.ic_reset), null, Modifier.size(18.dp), tint = c.textSecondary)
        }
        SwitchRow(stringResource(R.string.show_covers), showCovers, viewModel::setShowCovers)
    }
}

@Composable
private fun FolderRow(folder: LibraryFolderInfo, onRemove: () -> Unit) {
    val c = LectorTheme.colors
    val t = LectorTheme.type
    val storage = stringResource(if (folder.onSd) R.string.storage_sd else R.string.storage_primary)
    val name = folder.name.ifEmpty { storage }
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(painterResource(R.drawable.ic_folder), null, Modifier.size(20.dp), tint = c.textSecondary)
        Column(Modifier.weight(1f)) {
            Text(name, style = t.row, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.dot_join, storage, pluralStringResource(R.plurals.folder_books, folder.books, folder.books)),
                style = t.secondary,
                color = c.textSecondary,
            )
        }
        IconAction(painterResource(R.drawable.ic_close), stringResource(R.string.remove_folder, name), onRemove, tint = c.textSecondary, iconSize = 18.dp)
    }
}
