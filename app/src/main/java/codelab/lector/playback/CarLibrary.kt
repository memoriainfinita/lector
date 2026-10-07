package codelab.lector.playback

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaConstants
import codelab.lector.AppContainer
import codelab.lector.R
import codelab.lector.bookmarks.BookmarkRow
import codelab.lector.data.db.Book
import codelab.lector.data.db.BookmarkKind
import codelab.lector.data.db.LibraryItem
import codelab.lector.library.LibraryEntry
import codelab.lector.library.LibraryFilter
import codelab.lector.library.buildLibrary
import codelab.lector.library.displayTitle
import codelab.lector.library.matchesSearch
import codelab.lector.library.progress
import codelab.lector.library.searchTerms
import codelab.lector.library.status
import codelab.lector.ui.formatDuration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * La biblioteca vista desde el coche (design.md › Pantallas › Android Auto): tres pestañas, libros
 * que se tocan para escuchar y marcadores para escuchar desde ellos. Mismas consultas que la app.
 */
@OptIn(UnstableApi::class)
class CarLibrary(private val context: Context, private val app: AppContainer) {

    /** Lo que se pide reproducir desde el coche. */
    sealed interface Target {
        data class Book(val bookId: String) : Target
        data class Mark(val bookId: String, val bookMs: Long) : Target
    }

    fun root(): MediaItem = folder(Root, context.getString(R.string.app_name), null)

    /** Hijos de [parentId]; null si no existe. */
    suspend fun children(parentId: String): List<MediaItem>? = when {
        parentId == Root -> tabs()
        parentId == TabContinue -> continueItems()
        parentId == TabLibrary -> libraryEntries().map { entryItem(it, showCovers()) }
        parentId == TabBookmarks -> bookmarkBooks()
        parentId.startsWith(FolderPrefix) -> folderEntry(parentId.removePrefix(FolderPrefix))
            ?.let { e -> val covers = showCovers(); e.items.map { bookItem(it, covers) } }
        parentId.startsWith(MarksPrefix) -> marks(parentId.removePrefix(MarksPrefix))
        else -> null
    }

    suspend fun item(id: String): MediaItem? = when {
        id == Root -> root()
        id.startsWith(TabPrefix) -> tabs().firstOrNull { it.mediaId == id }
        id.startsWith(BookPrefix) -> libraryItems().firstOrNull { it.book.id == id.removePrefix(BookPrefix) }?.let { bookItem(it, showCovers()) }
        id.startsWith(FolderPrefix) -> folderEntry(id.removePrefix(FolderPrefix))?.let { entryItem(it, showCovers()) }
        id.startsWith(MarksPrefix) -> app.database.books().get(id.removePrefix(MarksPrefix))?.let { marksBook(it, showCovers()) }
        id.startsWith(MarkPrefix) -> app.bookmarks.observeOne(id.removePrefix(MarkPrefix)).first()?.let { row ->
            app.database.books().get(row.bookmark.bookId)?.let { markItem(row, it, showCovers()) }
        }
        else -> null
    }

    /** Búsqueda de la Biblioteca (título, autor, narrador, serie, carpeta), sin los quitados. */
    suspend fun search(query: String): List<MediaItem> {
        val terms = searchTerms(query)
        if (terms.isEmpty()) return emptyList()
        val items = libraryItems()
        val roots = app.database.folders().folders().map { it.path }
        val found = buildLibrary(items, app.database.folders().rules(), emptySet(), app.librarySettings.sort.first(), false, terms, roots)
            .flatMap { e ->
                when (e) {
                    is LibraryEntry.BookEntry -> listOf(e.item)
                    is LibraryEntry.FolderEntry -> e.items.filter { matchesSearch(it, terms, roots) }.ifEmpty { e.items }
                }
            }
            .filter { !it.book.removed && it.book.unsupportedFormat == null }
        val covers = showCovers()
        return found.map { bookItem(it, covers) }
    }

    /**
     * Qué reproducir para un elemento pedido desde el coche: un libro, un marcador o, por voz, el
     * primer resultado de la búsqueda. Null: lo que ya está cargado.
     */
    suspend fun target(request: MediaItem): Target? {
        val id = request.mediaId
        return when {
            id.startsWith(BookPrefix) -> Target.Book(id.removePrefix(BookPrefix))
            id.startsWith(MarkPrefix) -> app.bookmarks.observeOne(id.removePrefix(MarkPrefix)).first()
                ?.let { row -> row.bookMs?.let { Target.Mark(row.bookmark.bookId, it) } }
            else -> request.requestMetadata.searchQuery?.let { search(it).firstOrNull() }
                ?.let { Target.Book(it.mediaId.removePrefix(BookPrefix)) }
        }
    }

    /**
     * Cambia lo que se ve en las pestañas: libros, su orden, estado o portada, y marcadores. No la
     * posición, que se guarda cada pocos segundos sonando.
     */
    val changes: Flow<Unit> = combine(
        app.database.books().observeLibrary(),
        app.database.bookmarks().observeAll(),
        app.librarySettings.sort,
        app.appearance.settings.map { it.showCovers },
        app.scanner.state.map { it.finishedAt },
    ) { items, marks, sort, covers, scanned ->
        listOf(
            items.sortedByDescending { it.book.lastPlayedAt ?: 0L }
                .map { listOf(it.book.id, it.book.displayTitle, it.book.author, it.status, it.book.removed, it.book.hiddenFromRecents) },
            marks.map { listOf(it.id, it.title, it.positionMs, it.kind) },
            sort, covers, scanned,
        )
    }.distinctUntilChanged().map { }

    val tabIds = listOf(TabContinue, TabLibrary, TabBookmarks)

    // ---- Pestañas ----

    private fun tabs() = listOf(
        folder(TabContinue, context.getString(R.string.continue_listening), null, playable = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM),
        folder(
            TabLibrary, context.getString(R.string.tab_library), null,
            browsable = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
            playable = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
        ),
        folder(
            TabBookmarks, context.getString(R.string.tab_bookmarks), null,
            browsable = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
            playable = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
        ),
    )

    /** Libros a medias, sin los quitados de recientes ni de la biblioteca; el más reciente primero. */
    private suspend fun continueItems(): List<MediaItem> {
        val covers = showCovers()
        return libraryItems()
            .filter { it.status == LibraryFilter.IN_PROGRESS && !it.book.removed && it.book.unsupportedFormat == null && !it.book.hiddenFromRecents && it.book.lastPlayedAt != null }
            .sortedByDescending { it.book.lastPlayedAt }
            .map { bookItem(it, covers) }
    }

    /** Biblioteca en el orden guardado, sin filtros ni quitados. */
    private suspend fun libraryEntries(): List<LibraryEntry> =
        buildLibrary(libraryItems(), app.database.folders().rules(), emptySet(), app.librarySettings.sort.first())

    private suspend fun folderEntry(path: String) =
        libraryEntries().filterIsInstance<LibraryEntry.FolderEntry>().firstOrNull { it.path == path }

    /** Libros con marcadores normales, el escuchado más recientemente primero (como la recopilación). */
    private suspend fun bookmarkBooks(): List<MediaItem> {
        val ids = app.database.bookmarks().observeAll().first().filter { it.kind == BookmarkKind.NORMAL }.map { it.bookId }.distinct()
        val covers = showCovers()
        return app.database.books().byIds(ids)
            .sortedWith(compareByDescending<Book> { it.lastPlayedAt ?: 0L }.thenBy { it.displayTitle.lowercase() })
            .map { marksBook(it, covers) }
    }

    private suspend fun marks(bookId: String): List<MediaItem>? {
        val book = app.database.books().get(bookId) ?: return null
        val covers = showCovers()
        return app.bookmarks.observeBook(bookId).first()
            .filter { it.bookmark.kind == BookmarkKind.NORMAL && it.bookMs != null }
            .map { markItem(it, book, covers) }
    }

    // ---- Elementos ----

    private fun entryItem(entry: LibraryEntry, covers: Boolean): MediaItem = when (entry) {
        is LibraryEntry.BookEntry -> bookItem(entry.item, covers)
        is LibraryEntry.FolderEntry -> folder(
            FolderPrefix + entry.path, entry.name,
            entry.items.firstNotNullOfOrNull { app.covers.uri(it.book.id) }.takeIf { covers },
            playable = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
        )
    }

    private fun bookItem(item: LibraryItem, covers: Boolean): MediaItem {
        val extras = Bundle()
        when (item.status) {
            LibraryFilter.NOT_STARTED -> extras.putInt(MediaConstants.EXTRAS_KEY_COMPLETION_STATUS, MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED)
            LibraryFilter.IN_PROGRESS -> {
                extras.putInt(MediaConstants.EXTRAS_KEY_COMPLETION_STATUS, MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED)
                extras.putDouble(MediaConstants.EXTRAS_KEY_COMPLETION_PERCENTAGE, item.progress.toDouble())
            }
            LibraryFilter.FINISHED -> extras.putInt(MediaConstants.EXTRAS_KEY_COMPLETION_STATUS, MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED)
        }
        val book = item.book
        return MediaItem.Builder()
            .setMediaId(BookPrefix + book.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(book.displayTitle)
                    .setArtist(book.author)
                    .setArtworkUri(if (covers) app.covers.uri(book.id) else null)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }

    /** Libro de la pestaña Marcadores: se abre para ver sus marcadores. */
    private fun marksBook(book: Book, covers: Boolean) = folder(
        MarksPrefix + book.id, book.displayTitle,
        if (covers) app.covers.uri(book.id) else null,
        subtitle = book.author,
        playable = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
    )

    /** Título del marcador (o capítulo); debajo, tiempo del libro y capítulo, como en la app. */
    private fun markItem(row: BookmarkRow, book: Book, covers: Boolean): MediaItem {
        val mark = row.bookmark
        val title = mark.title ?: row.segmentTitle ?: context.getString(R.string.bookmark_untitled)
        val subtitle = listOfNotNull(row.bookMs?.let(::formatDuration), row.segmentTitle.takeIf { mark.title != null }).joinToString(" · ")
        return MediaItem.Builder()
            .setMediaId(MarkPrefix + mark.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(subtitle)
                    .setAlbumTitle(book.displayTitle)
                    .setArtworkUri(if (covers) app.covers.uri(book.id) else null)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()
    }

    /** Nodo navegable; [browsable] y [playable], el estilo de sus hijos (cuadrícula o lista). */
    private fun folder(id: String, title: String, artwork: Uri?, subtitle: String? = null, browsable: Int? = null, playable: Int? = null): MediaItem {
        val extras = Bundle().apply {
            browsable?.let { putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, it) }
            playable?.let { putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, it) }
        }
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(subtitle)
                    .setArtworkUri(artwork)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }

    private suspend fun libraryItems(): List<LibraryItem> = app.database.books().observeLibrary().first()

    private suspend fun showCovers(): Boolean = app.appearance.settings.first().showCovers

    private companion object {
        const val Root = "root"
        const val TabPrefix = "tab:"
        const val TabContinue = "tab:continue"
        const val TabLibrary = "tab:library"
        const val TabBookmarks = "tab:bookmarks"
        const val FolderPrefix = "folder:"
        const val MarksPrefix = "marks:"
        const val BookPrefix = "book:"
        const val MarkPrefix = "mark:"
    }
}
