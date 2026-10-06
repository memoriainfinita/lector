package codelab.lector.bookmarks

import codelab.lector.data.db.Book
import codelab.lector.data.db.Bookmark
import codelab.lector.data.db.BookmarkTag
import codelab.lector.data.db.BookmarkTagName
import codelab.lector.data.db.LectorDatabase
import codelab.lector.data.db.Tag
import codelab.lector.data.db.TagUse
import codelab.lector.data.settings.PlaybackSettingsRepository
import codelab.lector.playback.BookTimeline
import codelab.lector.playback.FileChapter
import codelab.lector.playback.FilePosition
import codelab.lector.playback.PlaybackConnection
import codelab.lector.playback.TimelineFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

/** Marcador listo para mostrar: dónde cae en el libro y sus tags. */
data class BookmarkRow(
    val bookmark: Bookmark,
    val tags: List<BookmarkTagName>,
    /** Posición en ms del libro; null si su archivo ya no está en el libro. */
    val bookMs: Long?,
    /** Capítulo, o archivo sin extensión si el libro no tiene capítulos. */
    val segmentTitle: String?,
    /** Posición dentro del tramo. */
    val segmentMs: Long?,
)

/** Marcadores de un libro en la recopilación. */
data class BookmarkGroup(val book: Book, val rows: List<BookmarkRow>)

/**
 * Marcadores y tags (design.md › Pantallas › Marcadores). Lecturas para las hojas y escrituras
 * en [scope], que sobreviven a la hoja que las pide (guardar al cerrarla).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkStore(
    private val db: LectorDatabase,
    private val playback: PlaybackConnection,
    private val settings: PlaybackSettingsRepository,
    private val scope: CoroutineScope,
) {
    private val _created = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Marcadores creados desde la app con "Abrir la hoja al marcar" activado: la raíz abre su hoja. */
    val created: SharedFlow<String> = _created

    /** Marcar desde la app: guarda en la posición actual y, según el ajuste, pide su hoja. */
    fun markHere() {
        scope.launch {
            val id = playback.addBookmark() ?: return@launch
            if (settings.current().openSheetOnMark) _created.emit(id)
        }
    }

    /** Marcadores de un libro por posición, con el de pausa. */
    fun observeBook(bookId: String): Flow<List<BookmarkRow>> =
        combine(db.bookmarks().observeForBook(bookId), db.bookmarks().observeTagsForBook(bookId)) { marks, tags -> marks to tags }
            .mapLatest { (marks, tags) ->
                val timeline = timeline(bookId)
                val byMark = tags.groupBy { it.bookmarkId }
                marks.map { row(it, byMark[it.id].orEmpty(), timeline) }
                    .sortedWith(compareBy(nullsLast()) { it.bookMs })
            }

    /**
     * Todos los marcadores, por libro: el escuchado más recientemente arriba y, dentro de cada
     * libro, por posición (design.md › Marcadores › Huecos resueltos). El orden de los libros se
     * fija al abrir: escuchar desde un marcador no los reordena mientras se mira la lista.
     */
    fun observeAll(): Flow<List<BookmarkGroup>> {
        var order: Map<String, Int>? = null
        return combine(db.bookmarks().observeAll(), db.bookmarks().observeAllTags()) { marks, tags -> marks to tags }
            .mapLatest { (marks, tags) ->
                val byMark = tags.groupBy { it.bookmarkId }
                val books = db.books().byIds(marks.map { it.bookId }.distinct()).associateBy { it.id }
                val known = order ?: books.values
                    .sortedWith(compareByDescending<Book> { it.lastPlayedAt ?: 0L }.thenBy { (it.customName ?: it.title).lowercase() })
                    .mapIndexed { i, b -> b.id to i }.toMap()
                    .also { order = it }
                marks.groupBy { it.bookId }.mapNotNull { (bookId, list) ->
                    val book = books[bookId] ?: return@mapNotNull null
                    val timeline = timeline(bookId)
                    BookmarkGroup(book, list.map { row(it, byMark[it.id].orEmpty(), timeline) }.sortedWith(compareBy(nullsLast()) { it.bookMs }))
                }.sortedBy { known[it.book.id] ?: Int.MAX_VALUE }
            }
    }

    fun observeOne(id: String): Flow<BookmarkRow?> =
        db.bookmarks().observe(id).flatMapLatest { mark ->
            if (mark == null) flowOf(null)
            else db.bookmarks().observeTags(id).mapLatest { tags -> row(mark, tags, timeline(mark.bookId)) }
        }

    fun observeTags(): Flow<List<TagUse>> = db.tags().observeByUse()

    /** Título y nota; en blanco quedan sin título o sin nota. */
    fun setText(id: String, title: String, note: String) {
        scope.launch {
            val mark = db.bookmarks().get(id) ?: return@launch
            val t = title.trim().ifEmpty { null }
            val n = note.trim().ifEmpty { null }
            if (t != mark.title || n != mark.note) db.bookmarks().setText(id, t, n, System.currentTimeMillis())
        }
    }

    fun setTag(id: String, tagId: Long, on: Boolean) {
        scope.launch {
            if (on) db.bookmarks().addTags(listOf(BookmarkTag(id, tagId))) else db.bookmarks().removeTag(id, tagId)
        }
    }

    /** Tag con ese nombre (sin distinguir mayúsculas), creado si no existe, y puesto en el marcador. */
    fun addTagByName(id: String, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        scope.launch {
            val tagId = db.tags().byName(clean)?.id ?: db.tags().insert(Tag(name = clean))
            db.bookmarks().addTags(listOf(BookmarkTag(id, tagId)))
        }
    }

    /** Borra el marcador. Devuelve el "Deshacer", que lo devuelve con sus tags. */
    fun delete(id: String): () -> Unit {
        var saved: Pair<Bookmark, List<BookmarkTag>>? = null
        val job = scope.launch {
            val mark = db.bookmarks().get(id) ?: return@launch
            saved = mark to db.bookmarks().tagLinks(id)
            db.bookmarks().delete(id)
        }
        return {
            scope.launch {
                job.join()
                saved?.let { (mark, links) ->
                    db.bookmarks().upsert(mark)
                    db.bookmarks().addTags(links)
                }
            }
        }
    }

    /** "Escuchar desde aquí". */
    fun play(row: BookmarkRow) {
        row.bookMs?.let { playback.playFrom(row.bookmark.bookId, it) }
    }

    private suspend fun timeline(bookId: String): BookTimeline {
        val chapters = db.books().chapters(bookId).groupBy { it.fileId }
        return BookTimeline(
            db.books().files(bookId).map { f ->
                TimelineFile(f.relativePath, f.durationMs, chapters[f.id].orEmpty().map { FileChapter(it.title, it.startMs, it.endMs) })
            },
        )
    }

    private fun row(mark: Bookmark, tags: List<BookmarkTagName>, timeline: BookTimeline): BookmarkRow {
        val index = timeline.indexOfFile(mark.file)
        if (index < 0) return BookmarkRow(mark, tags, null, null, null)
        val bookMs = timeline.toBook(FilePosition(index, mark.positionMs))
        val segment = timeline.segments.getOrNull(timeline.segmentIndexAt(bookMs))
        return BookmarkRow(mark, tags, bookMs, segment?.title, segment?.let { (bookMs - it.startMs).coerceAtLeast(0) })
    }
}
