package codelab.lector.data.db

import android.content.Context
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        Book::class, BookFile::class, Chapter::class,
        Bookmark::class, Tag::class, BookmarkTag::class,
        LibraryFolder::class, FolderRule::class, Correction::class,
    ],
    version = 1,
    exportSchema = true,
)
@ColumnTypeConverters(Converters::class)
abstract class LectorDatabase : RoomDatabase() {
    abstract fun books(): BookDao
    abstract fun bookmarks(): BookmarkDao
    abstract fun tags(): TagDao
    abstract fun folders(): FolderDao
    abstract fun corrections(): CorrectionDao

    companion object {
        fun create(context: Context): LectorDatabase =
            Room.databaseBuilder(context, LectorDatabase::class.java, "lector.db")
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
