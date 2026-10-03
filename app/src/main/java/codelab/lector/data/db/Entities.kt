package codelab.lector.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "book",
    indices = [Index("identityKey"), Index("totalDurationMs")],
)
data class Book(
    @PrimaryKey val id: String,
    val type: BookType = BookType.AUDIO,
    /** Hash de nombre de carpeta + nombres de archivo ordenados. Nunca la ruta. */
    val identityKey: String,
    val totalDurationMs: Long,
    /** Ruta actual de la carpeta o del archivo; cambia al volver a buscar. */
    val path: String,
    val title: String,
    val customName: String? = null,
    val author: String? = null,
    val narrator: String? = null,
    val series: String? = null,
    val seriesPart: String? = null,
    val coverSource: CoverSource = CoverSource.NONE,
    val addedAt: Long,
    val lastPlayedAt: Long? = null,
    val hiddenFromRecents: Boolean = false,
    val finished: Boolean = false,
    val inaccessible: Boolean = false,
    /** Ruta relativa del archivo en curso; null si no se ha empezado. */
    val positionFile: String? = null,
    val positionMs: Long = 0,
    val positionUpdatedAt: Long? = null,
    val speed: Float,
    val skipSilence: Boolean = false,
    val ownSound: Boolean = false,
    val preampDb: Float? = null,
    /** Ecualizador activado en el sonido propio. Versión 2 de la base de datos. */
    @ColumnInfo(defaultValue = "0") val eqEnabled: Boolean = false,
    val eqBands: List<Float>? = null,
)

@Entity(
    tableName = "book_file",
    foreignKeys = [ForeignKey(Book::class, ["id"], ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["bookId", "sortIndex"], unique = true)],
)
data class BookFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val relativePath: String,
    val sortIndex: Int,
    val durationMs: Long,
    val sizeBytes: Long,
)

@Entity(
    tableName = "chapter",
    foreignKeys = [ForeignKey(BookFile::class, ["id"], ["fileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("fileId")],
)
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileId: Long,
    val title: String,
    val startMs: Long,
    val endMs: Long,
)

/** RESTRICT: un libro con marcadores no se borra; los marcadores se mueven antes. */
@Entity(
    tableName = "bookmark",
    foreignKeys = [ForeignKey(Book::class, ["id"], ["bookId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("bookId")],
)
data class Bookmark(
    @PrimaryKey val id: String,
    val bookId: String,
    /** Ruta relativa al libro, para que sobreviva a mover el libro o importar una copia. */
    val file: String,
    val positionMs: Long,
    val title: String? = null,
    val note: String? = null,
    val kind: BookmarkKind = BookmarkKind.NORMAL,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "tag", indices = [Index(value = ["name"], unique = true)])
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity(
    tableName = "bookmark_tag",
    primaryKeys = ["bookmarkId", "tagId"],
    foreignKeys = [
        ForeignKey(Bookmark::class, ["id"], ["bookmarkId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Tag::class, ["id"], ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tagId")],
)
data class BookmarkTag(
    val bookmarkId: String,
    val tagId: Long,
)

@Entity(tableName = "library_folder")
data class LibraryFolder(
    @PrimaryKey val path: String,
)

/** La heredan las subcarpetas. */
@Entity(tableName = "folder_rule")
data class FolderRule(
    @PrimaryKey val folderPath: String,
    val workUnit: WorkUnit,
    val onFinish: OnFinish,
)

data class ChapterInfo(val startMs: Long, val endMs: Long, val title: String)

data class ChapterList(val items: List<ChapterInfo> = emptyList())

/**
 * Caché de lo leído de cada archivo de audio, por ruta. Si tamaño y fecha no cambian,
 * el escaneo rápido no vuelve a leer el archivo.
 */
@Entity(tableName = "file_meta")
data class FileMeta(
    @PrimaryKey val path: String,
    val sizeBytes: Long,
    val modifiedAt: Long,
    val durationMs: Long,
    val title: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val albumArtist: String? = null,
    val composer: String? = null,
    val series: String? = null,
    val seriesPart: String? = null,
    val hasArtwork: Boolean = false,
    val chapters: ChapterList = ChapterList(),
)

@Entity(tableName = "correction")
data class Correction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: CorrectionType,
    /** Firmas de identidad de los libros afectados, en orden. */
    val identityKeys: List<String>,
    /** SPLIT: archivo con el que empieza cada libro nuevo. MERGE: vacío. */
    val splitStartFiles: List<String> = emptyList(),
    val createdAt: Long,
)
