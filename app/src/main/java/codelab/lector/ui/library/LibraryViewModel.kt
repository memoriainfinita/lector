package codelab.lector.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codelab.lector.AppContainer
import codelab.lector.data.db.FolderRule
import codelab.lector.data.db.LibraryItem
import codelab.lector.library.FolderClass
import codelab.lector.library.ruleToStore
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
    /** Casilla "No disponibles": libros quitados de la biblioteca, atenuados. */
    val showUnavailable: Boolean = false,
    val scan: ScanState = ScanState(),
    /** Carpeta que se está recorriendo, relativa a su almacenamiento. */
    val scanFolder: String? = null,
    val showCovers: Boolean = true,
    /** Columnas en vertical, del pellizco; en horizontal, el doble. */
    val columns: Int = 2,
    /** Portada en caché por libro; los que no tienen, no están. */
    val covers: Map<String, String> = emptyMap(),
    /** Para la vista Carpetas: todos los libros, las reglas y las carpetas de la biblioteca. */
    val items: List<LibraryItem> = emptyList(),
    val rules: List<FolderRule> = emptyList(),
    val roots: List<String> = emptyList(),
)

/** Biblioteca: cuadrícula y vista Carpetas (design.md › Pantallas › Biblioteca › A y B). */
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

    private val listing = combine(app.librarySettings.sort, app.librarySettings.showUnavailable, ::Pair)

    private val view = combine(items, app.database.folders().observeRules(), filters, listing, app.playback.state) { list, rules, f, (sort, unavailable), np ->
        LibraryUiState(
            entries = buildLibrary(list, rules, f, sort, unavailable),
            continueItem = continueListening(list, np?.bookId),
            loadedBookId = np?.bookId,
            playing = np?.playWhenReady == true,
            filters = f,
            sort = sort,
            showUnavailable = unavailable,
            items = list,
            rules = rules,
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
            roots = folders.map { it.path },
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

    fun setShowUnavailable(show: Boolean) {
        viewModelScope.launch { app.librarySettings.setShowUnavailable(show) }
    }

    /** Pellizco: separar los dedos agranda las tarjetas (menos columnas); juntarlos, al revés. */
    fun zoom(bigger: Boolean) {
        val next = state.value.columns + if (bigger) -1 else 1
        viewModelScope.launch { app.librarySettings.setGridColumns(next) }
    }

    /**
     * Clase de carpeta: se guarda la regla (o se quita, si coincide con la heredada) y una búsqueda
     * rápida reagrupa los libros.
     */
    fun setFolderClass(path: String, folderClass: FolderClass) {
        viewModelScope.launch {
            val rule = ruleToStore(path, folderClass, state.value.rules)
            if (rule == null) app.database.folders().clearRule(path) else app.database.folders().setRule(rule)
            app.scanner.start()
        }
    }

    /** Archivos de los libros de la carpeta, para la cabecera de la hoja de clase. */
    suspend fun fileCount(path: String): Int = app.database.books().fileCountUnder(path)

    /** Carpeta relativa a su almacenamiento: "Audiobooks / yoga nidra". */
    fun relativePath(path: String): String = displayPath(path, storageRoots)

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
