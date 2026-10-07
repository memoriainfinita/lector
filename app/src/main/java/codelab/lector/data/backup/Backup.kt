package codelab.lector.data.backup

import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import codelab.lector.data.db.Book
import codelab.lector.data.db.BookmarkKind
import codelab.lector.data.db.CorrectionType
import codelab.lector.data.db.LectorDatabase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Copia completa de Ajustes › Datos (design.md › Ajustes › D): libros por firma, nunca por id ni
 * ruta, que cambian de un móvil a otro.
 */
@Serializable
data class Backup(
    val format: String = Format,
    val version: Int = Version,
    val exportedAt: Long,
    val appVersion: String,
    val books: List<BackupBook>,
    val corrections: List<BackupCorrection>,
    /** DataStore por nombre de clave, sin los propios del móvil ([DeviceKeys]). */
    val settings: Map<String, BackupSetting>,
    /** Idioma de la app (etiqueta BCP 47); vacío si sigue al del sistema. */
    val language: String,
) {
    companion object {
        const val Format = "lector-backup"
        const val Version = 1
    }
}

@Serializable
data class BackupBook(
    val identityKey: String,
    val title: String,
    val customName: String? = null,
    val author: String? = null,
    val durationMs: Long,
    /**
     * Ruta en el móvil de origen. No identifica: coloca al libro no encontrado en Carpetas y deja que
     * el escaneo lo reconecte por contenido o por duración.
     */
    val path: String,
    /** Sus archivos, en orden. */
    val files: List<BackupFile>,
    val positionFile: String? = null,
    val positionMs: Long = 0,
    val positionUpdatedAt: Long? = null,
    val lastPlayedAt: Long? = null,
    val finished: Boolean = false,
    val removed: Boolean = false,
    val speed: Float,
    val skipSilence: Boolean = false,
    val ownSound: Boolean = false,
    val preampDb: Float? = null,
    val eqEnabled: Boolean = false,
    val eqBands: List<Float>? = null,
    val segments: List<BackupSegment> = emptyList(),
    val bookmarks: List<BackupBookmark> = emptyList(),
)

/**
 * Archivo de un libro: ruta relativa al libro y duración, para situar los marcadores sin sus archivos. Con el
 * tamaño, el libro se reconoce por contenido en otra ruta ([codelab.lector.library.contentKey]); 0 en copias
 * anteriores.
 */
@Serializable
data class BackupFile(val path: String, val durationMs: Long, val sizeBytes: Long = 0)

@Serializable
data class BackupSegment(val file: String, val startMs: Long, val positionMs: Long, val updatedAt: Long)

@Serializable
data class BackupBookmark(
    val id: String,
    val file: String,
    val positionMs: Long,
    val title: String? = null,
    val note: String? = null,
    val kind: BookmarkKind = BookmarkKind.NORMAL,
    val createdAt: Long,
    val updatedAt: Long,
    /** Por nombre: los ids de tag cambian de un móvil a otro. */
    val tags: List<String> = emptyList(),
)

@Serializable
data class BackupCorrection(
    val type: CorrectionType,
    val identityKeys: List<String>,
    val splitStartFiles: List<String> = emptyList(),
    val createdAt: Long,
    val label: String = "",
)

/** Valor de DataStore con su tipo, para devolverlo con la misma clave al importar. */
@Serializable
data class BackupSetting(val type: String, val value: JsonElement)

/** Claves de DataStore propias de este móvil: no viajan en la copia. */
val DeviceKeys = setOf("last_book_id", "playing_book_id", "library_last_scan_at", "library_last_scan_books")

val BackupJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

class BackupExporter(private val db: LectorDatabase, private val settings: DataStore<Preferences>) {

    suspend fun export(now: Long, appVersion: String): Backup {
        val files = db.books().allFiles().groupBy { it.bookId }
        val segments = db.segmentPositions().all().groupBy { it.bookId }
        val tags = db.bookmarks().observeAllTags().first().groupBy({ it.bookmarkId }, { it.name })
        val bookmarks = db.bookmarks().observeAll().first().groupBy { it.bookId }
        val books = db.books().all().sortedBy { it.title.lowercase() }.map { book ->
            book.toBackup(
                files = files[book.id].orEmpty().map { BackupFile(it.relativePath, it.durationMs, it.sizeBytes) },
                segments = segments[book.id].orEmpty().map { BackupSegment(it.file, it.startMs, it.positionMs, it.updatedAt) },
                bookmarks = bookmarks[book.id].orEmpty().sortedBy { it.createdAt }.map { b ->
                    BackupBookmark(b.id, b.file, b.positionMs, b.title, b.note, b.kind, b.createdAt, b.updatedAt, tags[b.id].orEmpty().sorted())
                },
            )
        }
        val corrections = db.corrections().all().map { BackupCorrection(it.type, it.identityKeys, it.splitStartFiles, it.createdAt, it.label) }
        return Backup(
            exportedAt = now,
            appVersion = appVersion,
            books = books,
            corrections = corrections,
            settings = settingsOf(settings.data.first()),
            language = AppCompatDelegate.getApplicationLocales().toLanguageTags(),
        )
    }
}

private fun Book.toBackup(files: List<BackupFile>, segments: List<BackupSegment>, bookmarks: List<BackupBookmark>) = BackupBook(
    identityKey = identityKey,
    title = title,
    customName = customName,
    author = author,
    durationMs = totalDurationMs,
    path = path,
    files = files,
    positionFile = positionFile,
    positionMs = positionMs,
    positionUpdatedAt = positionUpdatedAt,
    lastPlayedAt = lastPlayedAt,
    finished = finished,
    removed = removed,
    speed = speed,
    skipSilence = skipSilence,
    ownSound = ownSound,
    preampDb = preampDb,
    eqEnabled = eqEnabled,
    eqBands = eqBands,
    segments = segments,
    bookmarks = bookmarks,
)

internal fun settingsOf(prefs: Preferences): Map<String, BackupSetting> =
    prefs.asMap().mapNotNull { (key, value) ->
        if (key.name in DeviceKeys) return@mapNotNull null
        val setting = when (value) {
            is Boolean -> BackupSetting("boolean", JsonPrimitive(value))
            is Int -> BackupSetting("int", JsonPrimitive(value))
            is Long -> BackupSetting("long", JsonPrimitive(value))
            is Float -> BackupSetting("float", JsonPrimitive(value))
            is Double -> BackupSetting("double", JsonPrimitive(value))
            is String -> BackupSetting("string", JsonPrimitive(value))
            is Set<*> -> BackupSetting("stringSet", JsonArray(value.map { JsonPrimitive(it as String) }))
            else -> null
        }
        setting?.let { key.name to it }
    }.toMap().toSortedMap()
