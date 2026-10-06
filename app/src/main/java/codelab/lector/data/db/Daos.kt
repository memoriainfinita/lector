package codelab.lector.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Upsert
    suspend fun upsert(book: Book)

    @Query("SELECT * FROM book WHERE id = :id")
    suspend fun get(id: String): Book?

    @Query("SELECT * FROM book WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<Book>

    @Query("SELECT * FROM book WHERE identityKey = :identityKey")
    suspend fun findByIdentity(identityKey: String): List<Book>

    @Query("SELECT * FROM book WHERE abs(totalDurationMs - :durationMs) <= :toleranceMs")
    suspend fun findByDuration(durationMs: Long, toleranceMs: Long = 1_000): List<Book>

    @Query("SELECT * FROM book ORDER BY lastPlayedAt DESC")
    fun observeAll(): Flow<List<Book>>

    @Query("SELECT * FROM book")
    suspend fun all(): List<Book>

    /**
     * Libros de las carpetas actuales de la biblioteca, con su posición en el libro y su número de
     * marcadores. Los de una carpeta quitada se conservan pero no salen aquí.
     */
    @Query(
        "SELECT book.*, " +
            "book.positionMs + coalesce((SELECT sum(f.durationMs) FROM book_file f WHERE f.bookId = book.id " +
            "AND f.sortIndex < (SELECT p.sortIndex FROM book_file p WHERE p.bookId = book.id AND p.relativePath = book.positionFile)), 0) " +
            "AS positionInBookMs, " +
            "(SELECT count(*) FROM bookmark b WHERE b.bookId = book.id AND b.kind = 'NORMAL') AS bookmarkCount " +
            "FROM book WHERE EXISTS (SELECT 1 FROM library_folder lf " +
            "WHERE book.path = lf.path OR substr(book.path, 1, length(lf.path) + 1) = lf.path || '/')"
    )
    fun observeLibrary(): Flow<List<LibraryItem>>

    @Query("UPDATE book SET inaccessible = 1 WHERE id IN (:ids)")
    suspend fun markInaccessible(ids: List<String>)

    @Query("DELETE FROM book_file WHERE bookId = :bookId")
    suspend fun deleteFiles(bookId: String)

    @Insert
    suspend fun insertFile(file: BookFile): Long

    @Query("DELETE FROM book WHERE id = :id")
    suspend fun delete(id: String)

    @Upsert
    suspend fun upsertFiles(files: List<BookFile>)

    @Query(
        "SELECT count(*) FROM book_file JOIN book ON book.id = book_file.bookId " +
            "WHERE book.path = :path OR substr(book.path, 1, length(:path) + 1) = :path || '/'"
    )
    suspend fun fileCountUnder(path: String): Int

    @Query("SELECT * FROM book_file WHERE bookId = :bookId ORDER BY sortIndex")
    suspend fun files(bookId: String): List<BookFile>

    /** Ajustes › Datos: la copia completa. */
    @Query("SELECT * FROM book_file ORDER BY bookId, sortIndex")
    suspend fun allFiles(): List<BookFile>

    /** Archivos de cualquier libro cuya ruta relativa acaba en [name] (el nombre del archivo). */
    @Query("SELECT * FROM book_file WHERE relativePath = :name OR substr(relativePath, -length(:name) - 1) = '/' || :name")
    suspend fun filesNamed(name: String): List<BookFile>

    @Insert
    suspend fun insertChapters(chapters: List<Chapter>)

    @Query(
        "SELECT chapter.* FROM chapter JOIN book_file ON chapter.fileId = book_file.id " +
            "WHERE book_file.bookId = :bookId ORDER BY book_file.sortIndex, chapter.startMs"
    )
    suspend fun chapters(bookId: String): List<Chapter>

    /** Escuchar de nuevo un libro quitado de recientes le quita la marca. */
    @Query("UPDATE book SET positionFile = :file, positionMs = :ms, positionUpdatedAt = :at, lastPlayedAt = :at, hiddenFromRecents = 0 WHERE id = :id")
    suspend fun savePosition(id: String, file: String, ms: Long, at: Long)

    /** Reiniciar posición, y su "Deshacer" con la posición anterior. */
    @Query("UPDATE book SET positionFile = :file, positionMs = :ms, positionUpdatedAt = :at WHERE id = :id")
    suspend fun setPosition(id: String, file: String?, ms: Long, at: Long?)

    @Query("UPDATE book SET hiddenFromRecents = :hidden WHERE id = :id")
    suspend fun setHiddenFromRecents(id: String, hidden: Boolean)

    @Query("UPDATE book SET customName = :name WHERE id = :id")
    suspend fun setCustomName(id: String, name: String?)

    @Query("SELECT coalesce(sum(sizeBytes), 0) FROM book_file WHERE bookId = :bookId")
    suspend fun sizeBytes(bookId: String): Long

    /** Posición traída de otro libro (reagrupar): no cuenta como escucha nueva. */
    @Query(
        "UPDATE book SET positionFile = :file, positionMs = :ms, positionUpdatedAt = :at, " +
            "lastPlayedAt = max(coalesce(lastPlayedAt, 0), :at) WHERE id = :id"
    )
    suspend fun movePosition(id: String, file: String, ms: Long, at: Long)

    @Query("UPDATE book SET speed = :speed WHERE id = :id")
    suspend fun setSpeed(id: String, speed: Float)

    @Query("UPDATE book SET skipSilence = :skipSilence WHERE id = :id")
    suspend fun setSkipSilence(id: String, skipSilence: Boolean)

    @Query("UPDATE book SET ownSound = :ownSound, preampDb = :preampDb, eqEnabled = :eqEnabled, eqBands = :eqBands WHERE id = :id")
    suspend fun setSound(id: String, ownSound: Boolean, preampDb: Float?, eqEnabled: Boolean, eqBands: List<Float>?)

    @Query("UPDATE book SET finished = :finished WHERE id = :id")
    suspend fun setFinished(id: String, finished: Boolean)

    @Query("UPDATE book SET inaccessible = :inaccessible WHERE id = :id")
    suspend fun setInaccessible(id: String, inaccessible: Boolean)

    @Query("UPDATE book SET removed = :removed WHERE id = :id")
    suspend fun setRemoved(id: String, removed: Boolean)

    /** Duración leída por el reproductor cuando el escaneo no la obtuvo. Recalcula el total del libro. */
    @Transaction
    suspend fun fixFileDuration(fileId: Long, bookId: String, durationMs: Long) {
        setFileDuration(fileId, durationMs)
        recomputeTotal(bookId)
    }

    @Query("UPDATE book_file SET durationMs = :durationMs WHERE id = :fileId")
    suspend fun setFileDuration(fileId: Long, durationMs: Long)

    @Query("UPDATE book SET totalDurationMs = (SELECT sum(durationMs) FROM book_file WHERE bookId = :bookId) WHERE id = :bookId")
    suspend fun recomputeTotal(bookId: String)
}

@Dao
interface BookmarkDao {
    @Upsert
    suspend fun upsert(bookmark: Bookmark)

    @Query("SELECT * FROM bookmark WHERE bookId = :bookId ORDER BY file, positionMs")
    fun observeForBook(bookId: String): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmark WHERE bookId = :bookId")
    suspend fun forBook(bookId: String): List<Bookmark>

    @Query("SELECT * FROM bookmark WHERE bookId = :bookId AND kind = :kind")
    suspend fun ofKind(bookId: String, kind: BookmarkKind): List<Bookmark>

    @Query("DELETE FROM bookmark WHERE bookId = :bookId AND kind = :kind")
    suspend fun deleteOfKind(bookId: String, kind: BookmarkKind)

    @Query("DELETE FROM bookmark WHERE id = :id")
    suspend fun delete(id: String)

    /** Solo el último marcador de pausa por libro. */
    @Transaction
    suspend fun replacePauseMarker(bookmark: Bookmark) {
        deleteOfKind(bookmark.bookId, BookmarkKind.PAUSE)
        upsert(bookmark.copy(kind = BookmarkKind.PAUSE))
    }

    @Query("SELECT * FROM bookmark WHERE id = :id")
    suspend fun get(id: String): Bookmark?

    @Query("SELECT * FROM bookmark")
    fun observeAll(): Flow<List<Bookmark>>

    @Query(
        "SELECT bookmark_tag.bookmarkId, tag.id AS tagId, tag.name FROM bookmark_tag " +
            "JOIN tag ON tag.id = bookmark_tag.tagId ORDER BY tag.name"
    )
    fun observeAllTags(): Flow<List<BookmarkTagName>>

    @Query("SELECT * FROM bookmark WHERE id = :id")
    fun observe(id: String): Flow<Bookmark?>

    @Query("UPDATE bookmark SET title = :title, note = :note, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setText(id: String, title: String?, note: String?, updatedAt: Long)

    @Insert
    suspend fun addTag(link: BookmarkTag)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTags(links: List<BookmarkTag>)

    @Query("DELETE FROM bookmark_tag WHERE bookmarkId = :bookmarkId AND tagId = :tagId")
    suspend fun removeTag(bookmarkId: String, tagId: Long)

    @Query("SELECT * FROM bookmark_tag WHERE bookmarkId = :bookmarkId")
    suspend fun tagLinks(bookmarkId: String): List<BookmarkTag>

    /** Tags de los marcadores de un libro, para sus filas. */
    @Query(
        "SELECT bookmark_tag.bookmarkId, tag.id AS tagId, tag.name FROM bookmark_tag " +
            "JOIN tag ON tag.id = bookmark_tag.tagId JOIN bookmark ON bookmark.id = bookmark_tag.bookmarkId " +
            "WHERE bookmark.bookId = :bookId ORDER BY tag.name"
    )
    fun observeTagsForBook(bookId: String): Flow<List<BookmarkTagName>>

    @Query(
        "SELECT bookmark_tag.bookmarkId, tag.id AS tagId, tag.name FROM bookmark_tag " +
            "JOIN tag ON tag.id = bookmark_tag.tagId WHERE bookmark_tag.bookmarkId = :bookmarkId ORDER BY tag.name"
    )
    fun observeTags(bookmarkId: String): Flow<List<BookmarkTagName>>

    @Query(
        "SELECT bookmark.* FROM bookmark JOIN bookmark_tag ON bookmark.id = bookmark_tag.bookmarkId " +
            "WHERE bookmark_tag.tagId = :tagId"
    )
    suspend fun withTag(tagId: Long): List<Bookmark>

    @Query("SELECT count(*) FROM bookmark")
    suspend fun count(): Int
}

@Dao
interface TagDao {
    @Insert
    suspend fun insert(tag: Tag): Long

    @Query("SELECT * FROM tag WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun byName(name: String): Tag?

    @Query("SELECT * FROM tag WHERE id = :id")
    suspend fun get(id: Long): Tag?

    @Query("SELECT * FROM bookmark_tag WHERE tagId = :tagId")
    suspend fun links(tagId: Long): List<BookmarkTag>

    @Query("UPDATE tag SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM tag WHERE id = :id")
    suspend fun delete(id: Long)

    /** Tags con su número de marcadores, el más usado primero. */
    @Query(
        "SELECT tag.id, tag.name, count(bookmark_tag.bookmarkId) AS uses FROM tag " +
            "LEFT JOIN bookmark_tag ON tag.id = bookmark_tag.tagId " +
            "GROUP BY tag.id ORDER BY uses DESC, tag.name COLLATE NOCASE"
    )
    fun observeByUse(): Flow<List<TagUse>>
}

@Dao
interface FolderDao {
    @Upsert
    suspend fun addFolder(folder: LibraryFolder)

    @Query("DELETE FROM library_folder WHERE path = :path")
    suspend fun removeFolder(path: String)

    @Query("SELECT * FROM library_folder ORDER BY path")
    fun observeFolders(): Flow<List<LibraryFolder>>

    @Query("SELECT * FROM library_folder ORDER BY path")
    suspend fun folders(): List<LibraryFolder>

    @Query("SELECT * FROM folder_rule")
    suspend fun rules(): List<FolderRule>

    @Query("SELECT * FROM folder_rule")
    fun observeRules(): Flow<List<FolderRule>>

    @Upsert
    suspend fun setRule(rule: FolderRule)

    @Query("DELETE FROM folder_rule WHERE folderPath = :folderPath")
    suspend fun clearRule(folderPath: String)

    /** Regla de la carpeta o de su ancestro más cercano. */
    @Query(
        "SELECT * FROM folder_rule WHERE folderPath = :path " +
            "OR substr(:path, 1, length(folderPath) + 1) = folderPath || '/' " +
            "ORDER BY length(folderPath) DESC LIMIT 1"
    )
    suspend fun ruleFor(path: String): FolderRule?
}

@Dao
interface FileMetaDao {
    @Query("SELECT * FROM file_meta")
    suspend fun all(): List<FileMeta>

    @Upsert
    suspend fun upsert(items: List<FileMeta>)

    @Query("DELETE FROM file_meta WHERE path IN (:paths)")
    suspend fun delete(paths: List<String>)

    @Query("UPDATE file_meta SET durationMs = :durationMs WHERE path = :path")
    suspend fun setDuration(path: String, durationMs: Long)
}

@Dao
interface CorrectionDao {
    @Insert
    suspend fun insert(correction: Correction): Long

    @Query("DELETE FROM correction WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM correction ORDER BY createdAt")
    suspend fun all(): List<Correction>

    @Query("SELECT * FROM correction ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Correction>>
}

@Dao
interface SegmentPositionDao {
    @Query("SELECT * FROM segment_position WHERE bookId = :bookId")
    suspend fun forBook(bookId: String): List<SegmentPosition>

    @Query("SELECT * FROM segment_position")
    suspend fun all(): List<SegmentPosition>

    @Upsert
    suspend fun save(position: SegmentPosition)

    @Query("DELETE FROM segment_position WHERE bookId = :bookId AND file = :file AND startMs = :startMs")
    suspend fun forget(bookId: String, file: String, startMs: Long)

    @Query("DELETE FROM segment_position WHERE bookId = :bookId")
    suspend fun clear(bookId: String)
}
