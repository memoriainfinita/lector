package codelab.lector.library

import codelab.lector.data.db.Book
import codelab.lector.data.db.FolderRule
import codelab.lector.data.db.LibraryItem
import codelab.lector.data.db.OnFinish
import codelab.lector.data.db.WorkUnit

/*
 * Lo que muestra la cuadrícula de la biblioteca (design.md › Pantallas › Biblioteca › A).
 * Funciones puras: el ViewModel las aplica sobre BookDao.observeLibrary.
 */

enum class LibrarySort { RECENT, ADDED, TITLE, AUTHOR, REMAINING }

enum class LibraryFilter { IN_PROGRESS, NOT_STARTED, FINISHED }

enum class FolderKind { EPISODES, SESSIONS }

val Book.displayTitle: String get() = customName ?: title

val LibraryItem.status: LibraryFilter
    get() = when {
        book.finished -> LibraryFilter.FINISHED
        book.positionFile == null && positionInBookMs == 0L -> LibraryFilter.NOT_STARTED
        else -> LibraryFilter.IN_PROGRESS
    }

/** Tiempo restante a 1x, sin la velocidad del libro. */
val LibraryItem.remainingMs: Long get() = (book.totalDurationMs - positionInBookMs).coerceAtLeast(0)

val LibraryItem.progress: Float
    get() = if (book.totalDurationMs <= 0) 0f else (positionInBookMs.toFloat() / book.totalDurationMs).coerceIn(0f, 1f)

/** Fecha de escucha para ordenar: los quitados de recientes cuentan como no escuchados. */
private val LibraryItem.recentAt: Long? get() = book.lastPlayedAt?.takeUnless { book.hiddenFromRecents }

sealed interface LibraryEntry {
    val key: String

    data class BookEntry(val item: LibraryItem) : LibraryEntry {
        override val key get() = item.book.id
    }

    /** Carpeta de Episodios o Sesiones: una sola tarjeta que abre la carpeta. */
    data class FolderEntry(val path: String, val name: String, val kind: FolderKind, val items: List<LibraryItem>) : LibraryEntry {
        override val key get() = "folder:$path"
    }
}

/**
 * Agrupa, filtra y ordena. Los libros de una carpeta con clase "cada archivo" se agrupan por la
 * carpeta que contiene los archivos, no por la que tiene la regla. Varios filtros: la unión;
 * ninguno: todo. Una carpeta sale si alguno de sus libros pasa el filtro. Los quitados de la
 * biblioteca, solo con [showUnavailable] ("No disponibles" en ordenar) o buscando.
 *
 * Buscando ([searchTerms] no vacío), además del filtro: un libro sale si coincide; una carpeta, si
 * coincide su nombre o alguno de sus libros.
 */
fun buildLibrary(
    items: List<LibraryItem>,
    rules: List<FolderRule>,
    filters: Set<LibraryFilter>,
    sort: LibrarySort,
    showUnavailable: Boolean = false,
    searchTerms: List<String> = emptyList(),
    roots: List<String> = emptyList(),
): List<LibraryEntry> {
    val searching = searchTerms.isNotEmpty()
    val entries = mutableListOf<LibraryEntry>()
    val groups = linkedMapOf<String, MutableList<LibraryItem>>()
    val kinds = mutableMapOf<String, FolderKind>()
    for (item in items) {
        if (item.book.removed && !showUnavailable && !searching) continue
        val rule = ruleFor(item.book.path, rules)
        if (rule?.workUnit == WorkUnit.FILE) {
            val folder = item.book.path.substringBeforeLast('/')
            groups.getOrPut(folder) { mutableListOf() } += item
            kinds[folder] = if (rule.onFinish == OnFinish.RESTART) FolderKind.SESSIONS else FolderKind.EPISODES
        } else {
            entries += LibraryEntry.BookEntry(item)
        }
    }
    groups.forEach { (path, members) ->
        entries += LibraryEntry.FolderEntry(path, path.substringAfterLast('/'), kinds.getValue(path), members.sortedWith(compareBy(NaturalOrder) { it.book.path }))
    }
    val filtered = if (filters.isEmpty()) entries else entries.filter { e ->
        when (e) {
            is LibraryEntry.BookEntry -> e.item.status in filters
            is LibraryEntry.FolderEntry -> e.items.any { it.status in filters }
        }
    }
    val visible = if (!searching) filtered else filtered.filter { e ->
        when (e) {
            is LibraryEntry.BookEntry -> matchesSearch(e.item, searchTerms, roots)
            is LibraryEntry.FolderEntry ->
                searchTerms.all { it in fold(e.name) } || e.items.any { matchesSearch(it, searchTerms, roots) }
        }
    }
    return visible.sortedWith(comparatorFor(sort))
}

private val LibraryEntry.members: List<LibraryItem>
    get() = when (this) {
        is LibraryEntry.BookEntry -> listOf(item)
        is LibraryEntry.FolderEntry -> items
    }

private val LibraryEntry.title: String
    get() = when (this) {
        is LibraryEntry.BookEntry -> item.book.displayTitle
        is LibraryEntry.FolderEntry -> name
    }

private val LibraryEntry.author: String?
    get() = when (this) {
        is LibraryEntry.BookEntry -> item.book.author
        is LibraryEntry.FolderEntry -> null
    }

private fun comparatorFor(sort: LibrarySort): Comparator<LibraryEntry> {
    val byTitle = compareBy(NaturalOrder) { e: LibraryEntry -> e.title }
    val newestAdded = compareByDescending { e: LibraryEntry -> e.members.maxOf { it.book.addedAt } }
    return when (sort) {
        // Nunca escuchados (o quitados de recientes) al final, por fecha de añadido.
        LibrarySort.RECENT -> compareByDescending<LibraryEntry, Long?>(nullsFirst()) { e -> e.members.mapNotNull { it.recentAt }.maxOrNull() }
            .then(newestAdded).then(byTitle)
        LibrarySort.ADDED -> newestAdded.then(byTitle)
        LibrarySort.TITLE -> byTitle
        // Sin autor al final.
        LibrarySort.AUTHOR -> compareBy<LibraryEntry, String?>(nullsLast(NaturalOrder)) { it.author }.then(byTitle)
        LibrarySort.REMAINING -> compareBy { e: LibraryEntry -> e.members.minOf { it.remainingMs } }.then(byTitle)
    }
}

/**
 * Libro de la tarjeta "Seguir escuchando": el cargado; sin él, el último escuchado que no esté
 * quitado de recientes, terminado ni quitado de la biblioteca; sin ninguno, null.
 */
fun continueListening(items: List<LibraryItem>, loadedBookId: String?): LibraryItem? =
    items.firstOrNull { it.book.id == loadedBookId }
        ?: items.filter { it.recentAt != null && !it.book.finished && !it.book.removed }.maxByOrNull { it.recentAt!! }

/** Ruta para mostrar, relativa a su almacenamiento: "Audiobooks / Frank Herbert". */
fun displayPath(path: String, storageRoots: List<String>): String {
    val root = storageRoots.filter { path == it || path.startsWith("$it/") }.maxByOrNull { it.length }
    val relative = if (root == null) path else path.removePrefix(root).trimStart('/')
    return relative.split('/').filter { it.isNotEmpty() }.joinToString(" / ")
}
