package codelab.lector.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codelab.lector.AppContainer
import codelab.lector.data.db.Book
import codelab.lector.data.db.FolderRule
import codelab.lector.data.db.LibraryItem
import codelab.lector.library.FolderClass
import codelab.lector.library.ruleToStore
import codelab.lector.library.LibraryEntry
import codelab.lector.library.LibraryFilter
import codelab.lector.library.LibrarySort
import codelab.lector.library.ScanState
import codelab.lector.library.baseFolder
import codelab.lector.library.buildLibrary
import codelab.lector.library.searchTerms
import codelab.lector.library.continueListening
import codelab.lector.library.displayPath
import codelab.lector.playback.PlayerAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    /** Buscando: palabras de la búsqueda, para resaltar los títulos. Vacía: sin búsqueda. */
    val searchTerms: List<String> = emptyList(),
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

    private val _query = MutableStateFlow("")
    /** Texto de la búsqueda, aparte del estado: el campo no espera a que se filtre la cuadrícula. */
    val query: StateFlow<String> = _query.asStateFlow()

    private class Listing(val sort: LibrarySort, val unavailable: Boolean, val terms: List<String>, val roots: List<String>)

    private val listing = combine(
        app.librarySettings.sort,
        app.librarySettings.showUnavailable,
        _query.map(::searchTerms).distinctUntilChanged(),
        app.database.folders().observeFolders(),
    ) { sort, unavailable, terms, folders -> Listing(sort, unavailable, terms, folders.map { it.path }) }

    private val view = combine(items, app.database.folders().observeRules(), filters, listing, app.playback.state) { list, rules, f, l, np ->
        val sort = l.sort
        val unavailable = l.unavailable
        LibraryUiState(
            entries = buildLibrary(list, rules, f, sort, unavailable, l.terms, l.roots),
            searchTerms = l.terms,
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

    fun setQuery(text: String) {
        _query.value = text
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

    /** Separar necesita al menos dos archivos. */
    suspend fun bookFileCount(bookId: String): Int = books.files(bookId).size

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

    // ---- Menú del libro (design.md › Pantallas › Biblioteca › C). Las que se deshacen devuelven el "Deshacer". ----

    private val books get() = app.database.books()

    private fun isLoaded(bookId: String) = bookId == app.playback.state.value?.bookId

    /** Escribe y, si es el libro cargado, el motor lo relee (título de la notificación, terminado). */
    private fun update(bookId: String, write: suspend () -> Unit) {
        viewModelScope.launch {
            write()
            if (isLoaded(bookId)) app.playback.refresh(bookId)
        }
    }

    suspend fun sizeBytes(bookId: String): Long = books.sizeBytes(bookId)

    fun setFinished(bookId: String, finished: Boolean) = update(bookId) { books.setFinished(bookId, finished) }

    /** Nombre propio solo en LECTOR; vacío o igual al título lo quita. */
    fun rename(book: Book, name: String) {
        val custom = name.trim().takeIf { it.isNotEmpty() && it != book.title }
        update(book.id) { books.setCustomName(book.id, custom) }
    }

    /** Al inicio. El libro cargado, además, en pausa. */
    fun resetPosition(item: LibraryItem): () -> Unit {
        val b = item.book
        if (isLoaded(b.id)) app.playback.reset(b.id)
        else viewModelScope.launch { books.setPosition(b.id, null, 0, System.currentTimeMillis()) }
        return {
            if (isLoaded(b.id)) app.playback.jumpTo(item.positionInBookMs)
            else viewModelScope.launch { books.setPosition(b.id, b.positionFile, b.positionMs, b.positionUpdatedAt) }
        }
    }

    fun hideFromRecents(bookId: String): () -> Unit {
        viewModelScope.launch { books.setHiddenFromRecents(bookId, true) }
        return { viewModelScope.launch { books.setHiddenFromRecents(bookId, false) } }
    }

    /** Archivo para "Abrir con…": el que está en curso o, si no, el primero. */
    suspend fun fileToOpen(book: Book): File? {
        val relative = book.positionFile ?: books.files(book.id).firstOrNull()?.relativePath ?: return null
        return File(baseFolder(book), relative)
    }

    class DeleteInfo(val files: Int, val bytes: Long)

    suspend fun deleteInfo(bookId: String): DeleteInfo {
        val files = books.files(bookId)
        return DeleteInfo(files.size, files.sumOf { it.sizeBytes })
    }

    /**
     * Borrar del móvil: solo los archivos del libro. Si suena, primero se descarga. El libro queda
     * inaccesible con su posición, marcadores y portada. False si alguno no se pudo borrar.
     */
    suspend fun deleteFromPhone(book: Book): Boolean {
        if (isLoaded(book.id)) app.playback.unload(book.id)
        val base = baseFolder(book)
        val deleted = withContext(Dispatchers.IO) {
            books.files(book.id).map { File(base, it.relativePath) }.map { !it.exists() || it.delete() }
        }
        books.setInaccessible(book.id, true)
        return deleted.all { it }
    }

    /** Quitar (o devolver) un libro sin archivos: no borra nada. */
    fun setRemoved(bookId: String, removed: Boolean): () -> Unit {
        viewModelScope.launch { books.setRemoved(bookId, removed) }
        return { viewModelScope.launch { books.setRemoved(bookId, !removed) } }
    }
}
