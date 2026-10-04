package codelab.lector.data.db

import androidx.room3.Dao
import androidx.room3.Insert
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

    @Insert
    suspend fun insertChapters(chapters: List<Chapter>)

    @Query(
        "SELECT chapter.* FROM chapter JOIN book_file ON chapter.fileId = book_file.id " +
            "WHERE book_file.bookId = :bookId ORDER BY book_file.sortIndex, chapter.startMs"
    )
    suspend fun chapters(bookId: String): List<Chapter>

    @Query("UPDATE book SET positionFile = :file, positionMs = :ms, positionUpdatedAt = :at, lastPlayedAt = :at WHERE id = :id")
    suspend fun savePosition(id: String, file: String, ms: Long, at: Long)

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

    @Insert
    suspend fun addTag(link: BookmarkTag)

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

    @Query("UPDATE tag SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM tag WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        "SELECT tag.* FROM tag LEFT JOIN bookmark_tag ON tag.id = bookmark_tag.tagId " +
            "GROUP BY tag.id ORDER BY count(bookmark_tag.bookmarkId) DESC, tag.name"
    )
    fun observeByUse(): Flow<List<Tag>>
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
}
