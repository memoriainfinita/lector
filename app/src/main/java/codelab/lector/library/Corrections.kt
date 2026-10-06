package codelab.lector.library

import codelab.lector.data.db.Correction
import codelab.lector.data.db.CorrectionType
import codelab.lector.data.db.LectorDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Unir y separar libros (design.md › Biblioteca › Unir y Separar): cada uno guarda una corrección que
 * el escaneo aplica sobre lo detectado, y una búsqueda la aplica al momento. El reagrupado del
 * escaneo pasa marcadores, posición y ajustes a los libros nuevos; el reproductor sigue al suyo.
 */
class LibraryCorrections(
    private val db: LectorDatabase,
    private val scanner: LibraryScanner,
    private val scope: CoroutineScope,
) {
    /** Las más recientes primero (Ajustes › Biblioteca › Correcciones). */
    fun observe(): Flow<List<Correction>> = db.corrections().observeAll()

    /** Une los libros en orden natural de ruta. Devuelve la corrección, para "Deshacer". */
    suspend fun merge(bookIds: List<String>): Long? {
        val books = db.books().byIds(bookIds).sortedWith(compareBy(NaturalOrder) { it.path })
        if (books.size < 2) return null
        return save(
            Correction(
                type = CorrectionType.MERGE,
                identityKeys = books.map { it.identityKey },
                label = books.joinToString(" + ") { it.displayTitle },
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    /** [startFiles]: ruta relativa del archivo con el que empieza cada libro nuevo, sin el primero. */
    suspend fun split(bookId: String, startFiles: List<String>): Long? {
        val book = db.books().get(bookId) ?: return null
        if (startFiles.isEmpty()) return null
        return save(
            Correction(
                type = CorrectionType.SPLIT,
                identityKeys = listOf(book.identityKey),
                splitStartFiles = startFiles,
                label = book.displayTitle,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    fun undo(id: Long) {
        scope.launch {
            db.corrections().delete(id)
            rescan()
        }
    }

    private suspend fun save(correction: Correction): Long {
        val id = db.corrections().insert(correction)
        scope.launch { rescan() }
        return id
    }

    /** Una búsqueda en curso ya leyó las correcciones: se espera y se lanza otra. */
    private suspend fun rescan() {
        if (scanner.state.value.running) scanner.start().join()
        scanner.start().join()
    }
}

/** Libro que tiene ahora el archivo [path] (ruta completa), con su ruta relativa dentro de él. */
suspend fun bookWithFile(db: LectorDatabase, path: String): Placement? {
    for (file in db.books().filesNamed(path.substringAfterLast('/'))) {
        val book = db.books().get(file.bookId) ?: continue
        if (normalizePath("${baseFolder(book)}/${file.relativePath}") == path) return Placement(book.id, file.relativePath)
    }
    return null
}
