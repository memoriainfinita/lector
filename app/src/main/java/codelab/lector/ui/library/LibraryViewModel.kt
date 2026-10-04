package codelab.lector.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codelab.lector.AppContainer
import codelab.lector.data.db.LibraryItem
import codelab.lector.library.LibraryEntry
import codelab.lector.library.LibraryFilter
import codelab.lector.library.LibrarySort
import codelab.lector.library.ScanState
import codelab.lector.library.buildLibrary
import codelab.lector.library.continueListening
import codelab.lector.library.displayPath
import codelab.lector.playback.PlayerAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    /** null mientras se lee la base de datos: ni cuadrícula ni estado vacío. */
    val hasFolders: Boolean? = null,
    val entries: List<LibraryEntry> = emptyList(),
    val continueItem: LibraryItem? = null,
    val loadedBookId: String? = null,
    /** El libro cargado va a sonar (el botón no parpadea mientras carga). */
    val playing: Boolean = false,
    val filters: Set<LibraryFilter> = emptySet(),
    val sort: LibrarySort = LibrarySort.RECENT,
    val scan: ScanState = ScanState(),
    /** Carpeta que se está recorriendo, relativa a su almacenamiento. */
    val scanFolder: String? = null,
    val showCovers: Boolean = true,
    /** Columnas en vertical, del pellizco; en horizontal, el doble. */
    val columns: Int = 2,
    /** Portada en caché por libro; los que no tienen, no están. */
    val covers: Map<String, String> = emptyMap(),
)

/** Cuadrícula de la biblioteca (design.md › Pantallas › Biblioteca › A). */
class LibraryViewModel(private val app: AppContainer, private val storageRoots: List<String>) : ViewModel() {
    private val filters = MutableStateFlow(emptySet<LibraryFilter>())
    /** Una sola consulta para la cuadrícula y las portadas. */
    private val items = app.database.books().observeLibrary()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    /**
     * Las portadas se escriben al final del escaneo: se vuelven a mirar cuando termina. Empieza
     * vacío para que la cuadrícula no espere a mirar el disco.
     */
    private val covers = combine(items, app.scanner.state.map { it.finishedAt }.distinctUntilChanged()) { list, _ ->
        list.mapNotNull { item -> app.covers.file(item.book.id).takeIf { it.exists() }?.let { item.book.id to it.path } }.toMap()
    }.flowOn(Dispatchers.IO).onStart { emit(emptyMap()) }

    private val view = combine(items, app.database.folders().observeRules(), filters, app.librarySettings.sort, app.playback.state) { list, rules, f, sort, np ->
        LibraryUiState(
            entries = buildLibrary(list, rules, f, sort),
            continueItem = continueListening(list, np?.bookId),
            loadedBookId = np?.bookId,
            playing = np?.playWhenReady == true,
            filters = f,
            sort = sort,
        )
    }.flowOn(Dispatchers.Default)

    private val withColumns = combine(view, app.librarySettings.gridColumns) { v, columns -> v.copy(columns = columns) }

    val state: StateFlow<LibraryUiState> = combine(
        withColumns,
        app.database.folders().observeFolders(),
        app.scanner.state,
        app.appearance.settings,
        covers,
    ) { v, folders, scan, appearance, coverMap ->
        v.copy(
            hasFolders = folders.isNotEmpty(),
            scan = scan,
            scanFolder = scan.currentFolder?.let { displayPath(it, storageRoots) },
            showCovers = appearance.showCovers,
            covers = coverMap,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun toggleFilter(filter: LibraryFilter) {
        filters.value = filters.value.let { if (filter in it) it - filter else it + filter }
    }

    fun setSort(sort: LibrarySort) {
        viewModelScope.launch { app.librarySettings.setSort(sort) }
    }

    /** Pellizco: separar los dedos agranda las tarjetas (menos columnas); juntarlos, al revés. */
    fun zoom(bigger: Boolean) {
        val next = state.value.columns + if (bigger) -1 else 1
        viewModelScope.launch { app.librarySettings.setGridColumns(next) }
    }

    /** Tocar un libro: si no es el cargado, lo carga y empieza a sonar. Después se abre Escuchando. */
    fun open(bookId: String) {
        if (bookId != app.playback.state.value?.bookId) app.playback.open(bookId, play = true)
    }

    /** Play de "Seguir escuchando": sin salir de la biblioteca. */
    fun playPause(bookId: String) {
        if (bookId == app.playback.state.value?.bookId) app.playback.act(PlayerAction.PLAY_PAUSE)
        else app.playback.open(bookId, play = true)
    }
}
