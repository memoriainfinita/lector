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

    @Query("SELECT * FROM book_file WHERE bookId = :bookId ORDER BY sortIndex")
    suspend fun files(bookId: String): List<BookFile>

    @Insert
    suspend fun insertChapters(chapters: List<Chapter>)
}

@Dao
interface BookmarkDao {
    @Upsert
    suspend fun upsert(bookmark: Bookmark)

    @Query("SELECT * FROM bookmark WHERE bookId = :bookId ORDER BY file, positionMs")
    fun observeForBook(bookId: String): Flow<List<Bookmark>>

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
