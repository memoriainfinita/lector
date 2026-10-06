package codelab.lector.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.room3.withWriteTransaction
import codelab.lector.data.db.Book
import codelab.lector.data.db.BookFile
import codelab.lector.data.db.Bookmark
import codelab.lector.data.db.BookmarkKind
import codelab.lector.data.db.BookmarkTag
import codelab.lector.data.db.Correction
import codelab.lector.data.db.LectorDatabase
import codelab.lector.data.db.SegmentPosition
import codelab.lector.data.db.Tag
import codelab.lector.library.DurationToleranceMs
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.util.UUID
import kotlin.math.abs

/** Lectura de un archivo elegido en Ajustes › Datos. */
sealed interface BackupRead {
    data class Ok(val backup: Backup) : BackupRead
    /** No es una copia de LECTOR, o está dañada. */
    data object NotBackup : BackupRead
    /** De una versión de LECTOR más nueva: su formato puede no entenderse entero. */
    data object Newer : BackupRead
}

/** [onError]: por qué no se pudo leer como copia (para el log). */
fun readBackup(text: String, onError: (Throwable) -> Unit = {}): BackupRead {
    val backup = runCatching { BackupJson.decodeFromString(Backup.serializer(), text) }.onFailure(onError).getOrNull()
    return when {
        backup == null || backup.format != Backup.Format -> BackupRead.NotBackup
        backup.version > Backup.Version -> BackupRead.Newer
        else -> BackupRead.Ok(backup)
    }
}

/** Libro de la copia y el libro de aquí que le corresponde; null si no está. */
data class BookMatch(val book: BackupBook, val local: Book?)

/** Resumen previo (lienzo `Settings-Data`) y lo que Combinar hará. Marcadores de pausa fuera de las cuentas. */
data class ImportPlan(
    val matches: List<BookMatch>,
    val newBookmarks: Int,
    val existingBookmarks: Int,
    val newerPositions: Int,
    val notFound: Int,
)

/**
 * Libro por firma; si no, por duración ±1 s entre los de aquí que no ha tomado ninguna firma, como la
 * reconciliación del escaneo. Cada libro de aquí corresponde a uno solo de la copia.
 */
fun planImport(backup: Backup, local: List<Book>, bookmarkIds: Set<String>): ImportPlan {
    val free = local.toMutableList()
    val byIdentity = backup.books.associateWith { b ->
        free.firstOrNull { it.identityKey == b.identityKey }?.also { free.remove(it) }
    }
    val matches = backup.books.map { b ->
        val found = byIdentity[b] ?: free
            .filter { abs(it.totalDurationMs - b.durationMs) <= DurationToleranceMs }
            .minByOrNull { abs(it.totalDurationMs - b.durationMs) }
            ?.also { free.remove(it) }
        BookMatch(b, found)
    }
    val marks = backup.books.flatMap { it.bookmarks }.filter { it.kind == BookmarkKind.NORMAL }
    return ImportPlan(
        matches = matches,
        newBookmarks = marks.count { it.id !in bookmarkIds },
        existingBookmarks = marks.count { it.id in bookmarkIds },
        newerPositions = matches.count { (b, l) -> l != null && b.isNewerThan(l) },
        notFound = matches.count { it.local == null },
    )
}

/** La posición de la copia es más reciente que la de aquí. */
internal fun BackupBook.isNewerThan(local: Book): Boolean {
    val at = positionUpdatedAt ?: return false
    return positionFile != null && at > (local.positionUpdatedAt ?: Long.MIN_VALUE)
}

class BackupImporter(private val db: LectorDatabase, private val settings: DataStore<Preferences>) {

    /** Plan contra la base de ahora. */
    suspend fun plan(backup: Backup): ImportPlan =
        planImport(backup, db.books().all(), db.bookmarks().observeAll().first().map { it.id }.toSet())

    /**
     * Combinar (design.md › Ajustes › D), en una sola transacción. Devuelve cuántas correcciones se han
     * añadido: si hay alguna, toca volver a buscar para aplicarlas.
     */
    suspend fun combine(backup: Backup, plan: ImportPlan, now: Long): Int {
        var added = 0
        db.withWriteTransaction {
            val tagIds = mutableMapOf<String, Long>()
            for ((b, local) in plan.matches) {
                val id = local?.id ?: addMissing(b, now)
                if (local != null) mergeInto(local, b)
                for (m in b.bookmarks) addBookmark(id, m, tagIds)
            }
            val existing = db.corrections().all().map { Triple(it.type, it.identityKeys, it.splitStartFiles) }.toSet()
            for (c in backup.corrections) {
                if (Triple(c.type, c.identityKeys, c.splitStartFiles) in existing) continue
                db.corrections().insert(Correction(type = c.type, identityKeys = c.identityKeys, splitStartFiles = c.splitStartFiles, createdAt = c.createdAt, label = c.label))
                added++
            }
        }
        return added
    }

    /** Libro que no está aquí: entra quitado, con sus archivos y su posición, como uno quitado aquí. */
    private suspend fun addMissing(b: BackupBook, now: Long): String {
        val id = UUID.randomUUID().toString()
        db.books().upsert(
            Book(
                id = id,
                identityKey = b.identityKey,
                totalDurationMs = b.durationMs,
                path = b.path,
                title = b.title,
                customName = b.customName,
                author = b.author,
                addedAt = now,
                lastPlayedAt = b.lastPlayedAt,
                finished = b.finished,
                removed = true,
                positionFile = b.positionFile,
                positionMs = b.positionMs,
                positionUpdatedAt = b.positionUpdatedAt,
                speed = b.speed,
                skipSilence = b.skipSilence,
                ownSound = b.ownSound,
                preampDb = b.preampDb,
                eqEnabled = b.eqEnabled,
                eqBands = b.eqBands,
            ),
        )
        db.books().upsertFiles(b.files.mapIndexed { i, f -> BookFile(bookId = id, relativePath = f.path, sortIndex = i, durationMs = f.durationMs, sizeBytes = 0) })
        b.segments.forEach { db.segmentPositions().save(SegmentPosition(id, it.file, it.startMs, it.positionMs, it.updatedAt)) }
        return id
    }

    /** Libro que ya está: gana la posición más reciente y con ella sus ajustes (como `carryOver`). */
    private suspend fun mergeInto(local: Book, b: BackupBook) {
        val id = local.id
        if (b.isNewerThan(local)) {
            db.books().movePosition(id, b.positionFile!!, b.positionMs, b.positionUpdatedAt!!)
            db.books().setSpeed(id, b.speed)
            db.books().setSkipSilence(id, b.skipSilence)
            db.books().setSound(id, b.ownSound, b.preampDb, b.eqEnabled, b.eqBands)
            db.books().setFinished(id, b.finished)
        }
        if (local.customName == null && b.customName != null) db.books().setCustomName(id, b.customName)
        val mine = db.segmentPositions().forBook(id).associateBy { it.file to it.startMs }
        for (s in b.segments) {
            val here = mine[s.file to s.startMs]
            if (here != null && here.updatedAt >= s.updatedAt) continue
            // Borrar y guardar, sin @Upsert sobre una fila existente: en las pruebas de la JVM falla.
            if (here != null) db.segmentPositions().forget(id, s.file, s.startMs)
            db.segmentPositions().save(SegmentPosition(id, s.file, s.startMs, s.positionMs, s.updatedAt))
        }
    }

    /** Un id que ya existe se omite. El de pausa, uno por libro: queda el más reciente. */
    private suspend fun addBookmark(bookId: String, m: BackupBookmark, tagIds: MutableMap<String, Long>) {
        if (db.bookmarks().get(m.id) != null) return
        val mark = Bookmark(m.id, bookId, m.file, m.positionMs, m.title, m.note, m.kind, m.createdAt, m.updatedAt)
        if (m.kind == BookmarkKind.PAUSE) {
            val here = db.bookmarks().ofKind(bookId, BookmarkKind.PAUSE).maxOfOrNull { it.updatedAt }
            if (here == null || here < m.updatedAt) db.bookmarks().replacePauseMarker(mark)
            return
        }
        db.bookmarks().upsert(mark)
        val links = m.tags.distinctBy { it.lowercase() }.map { name ->
            val tagId = tagIds.getOrPut(name.lowercase()) { db.tags().byName(name)?.id ?: db.tags().insert(Tag(name = name)) }
            BookmarkTag(m.id, tagId)
        }
        if (links.isNotEmpty()) db.bookmarks().addTags(links)
    }

    /** "Importar también los ajustes": los de la copia sustituyen a los de aquí, clave a clave. */
    suspend fun importSettings(backup: Backup) {
        settings.edit { p -> backup.settings.forEach { (name, s) -> if (name !in DeviceKeys) p.put(name, s) } }
    }
}

private fun MutablePreferences.put(name: String, s: BackupSetting) {
    runCatching {
        val v = s.value
        when (s.type) {
            "boolean" -> this[booleanPreferencesKey(name)] = v.jsonPrimitive.boolean
            "int" -> this[intPreferencesKey(name)] = v.jsonPrimitive.int
            "long" -> this[longPreferencesKey(name)] = v.jsonPrimitive.long
            "float" -> this[floatPreferencesKey(name)] = v.jsonPrimitive.float
            "double" -> this[doublePreferencesKey(name)] = v.jsonPrimitive.double
            "string" -> this[stringPreferencesKey(name)] = v.jsonPrimitive.content
            "stringSet" -> this[stringSetPreferencesKey(name)] = v.jsonArray.map { it.jsonPrimitive.content }.toSet()
        }
    }
}
