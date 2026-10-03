package codelab.lector.library

import codelab.lector.data.db.FileMeta
import java.security.MessageDigest

val AudioExtensions = setOf("mp3", "m4a", "m4b", "aac", "ogg", "oga", "opus", "flac", "wav")
val ImageExtensions = setOf("jpg", "jpeg", "png", "webp")

fun extensionOf(name: String) = name.substringAfterLast('.', "").lowercase()
fun stemOf(name: String) = name.substringBeforeLast('.')

/** Un archivo de audio encontrado, con lo leído de él. */
data class ScannedFile(val path: String, val name: String, val meta: FileMeta)

/** Una carpeta recorrida: su audio, sus imágenes y sus subcarpetas. */
data class ScannedFolder(
    val path: String,
    val name: String,
    val files: List<ScannedFile> = emptyList(),
    val images: List<String> = emptyList(),
    val subfolders: List<ScannedFolder> = emptyList(),
)

/** Parte de un libro: ruta relativa a la carpeta del libro. */
data class BookPart(val relativePath: String, val file: ScannedFile)

data class DetectedBook(
    val identityKey: String,
    /** Carpeta del libro, o el archivo si el libro es un solo archivo. */
    val path: String,
    /** Carpeta que lo contiene: reglas, portada de carpeta y rutas relativas. */
    val folderPath: String,
    val folderName: String,
    val parts: List<BookPart>,
    /** El libro es la carpeta entera (portada de carpeta y título de carpeta). */
    val wholeFolder: Boolean,
    val folderImages: List<String> = emptyList(),
    /** Falso en libros de una saga que comparten álbum: el álbum no es su título. */
    val albumIsTitle: Boolean = true,
) {
    val durationMs: Long get() = parts.sumOf { it.file.meta.durationMs }
    val isSingleFile: Boolean get() = parts.size == 1 && !wholeFolder
}

/** Firma de identidad: nombre de carpeta + nombres de archivo ordenados. Nunca la ruta completa. */
fun identityKey(folderName: String, relativePaths: List<String>): String =
    sha256(folderName + "\n" + relativePaths.sortedWith(NaturalOrder).joinToString("\n"))

fun sha256(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

/** Orden natural: "Part2" antes que "Part10". */
object NaturalOrder : Comparator<String> {
    private val chunk = Regex("\\d+|\\D+")

    override fun compare(a: String, b: String): Int {
        val ca = chunk.findAll(a.lowercase()).map { it.value }.toList()
        val cb = chunk.findAll(b.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(ca.size, cb.size)) {
            val x = ca[i]
            val y = cb[i]
            val r = if (x[0].isDigit() && y[0].isDigit()) {
                x.trimStart('0').length.compareTo(y.trimStart('0').length).takeIf { it != 0 }
                    ?: x.trimStart('0').compareTo(y.trimStart('0'))
            } else {
                x.compareTo(y)
            }
            if (r != 0) return r
        }
        return ca.size.compareTo(cb.size).takeIf { it != 0 } ?: a.compareTo(b)
    }
}
