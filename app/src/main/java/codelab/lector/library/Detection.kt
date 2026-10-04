package codelab.lector.library

import codelab.lector.data.db.FolderRule
import codelab.lector.data.db.WorkUnit

/** Archivo que por sí solo parece un libro completo, no una parte. */
const val LongFileMs = 3 * 3_600_000L

/** Subcarpeta de disco: "CD1", "disc 2", "Parte 3"… al final del nombre. */
private val DiscName = Regex("""(?i)(^|[\s\-_.(\[])(cd|disc|disk|disco|parte|part)[\s\-_.]*\d+\s*[)\]]?\s*$""")

fun isDiscFolder(name: String) = DiscName.containsMatchIn(name)

/** Regla de la carpeta o de su ancestro más cercano. */
fun ruleFor(path: String, rules: List<FolderRule>): FolderRule? =
    rules.filter { path == it.folderPath || path.startsWith(it.folderPath + "/") }
        .maxByOrNull { it.folderPath.length }

/**
 * Reglas de detección (design.md):
 * - Clase "cada archivo": cada archivo es una obra.
 * - Clase "la carpeta": la carpeta (con sus discos) es una obra.
 * - Libros: subcarpetas de disco se unen en un libro; si no, archivo con capítulos = un libro;
 *   mismo nombre salvo la numeración = un libro; mismo álbum = un libro (salvo varios archivos
 *   de 3 h o más: libros distintos); el resto, uno por archivo.
 * - Las demás subcarpetas se recorren por separado (carpetas de autor, volúmenes).
 */
fun detectBooks(root: ScannedFolder, rules: List<FolderRule>): List<DetectedBook> {
    val out = mutableListOf<DetectedBook>()
    fun visit(folder: ScannedFolder) {
        val rule = ruleFor(folder.path, rules)
        val discs = folder.subfolders.filter { isDiscFolder(it.name) && it.files.isNotEmpty() }
            .sortedWith(compareBy(NaturalOrder) { it.name })
        when (rule?.workUnit) {
            WorkUnit.FILE -> {
                folder.files.sortedWith(compareBy(NaturalOrder) { it.name }).forEach { out += single(folder, it) }
                folder.subfolders.forEach(::visit)
                return
            }
            WorkUnit.FOLDER -> {
                partsWithDiscs(folder, discs).takeIf { it.isNotEmpty() }?.let { out += whole(folder, it) }
            }
            null, WorkUnit.DETECT -> {
                if (discs.isNotEmpty()) out += whole(folder, partsWithDiscs(folder, discs))
                else out += groupLoose(folder)
            }
        }
        folder.subfolders.filterNot { it in discs }.forEach(::visit)
    }
    visit(root)
    return out
}

private fun partsWithDiscs(folder: ScannedFolder, discs: List<ScannedFolder>): List<BookPart> =
    folder.files.sortedWith(compareBy(NaturalOrder) { it.name }).map { BookPart(it.name, it) } +
        discs.flatMap { d ->
            d.files.sortedWith(compareBy(NaturalOrder) { it.name }).map { BookPart("${d.name}/${it.name}", it) }
        }

private fun whole(folder: ScannedFolder, parts: List<BookPart>) = DetectedBook(
    identityKey = identityKey(folder.name, parts.map { it.relativePath }),
    path = folder.path,
    folderPath = folder.path,
    folderName = folder.name,
    parts = parts,
    wholeFolder = true,
    folderImages = folder.images,
)

private fun single(folder: ScannedFolder, file: ScannedFile) = DetectedBook(
    identityKey = identityKey(folder.name, listOf(file.name)),
    path = file.path,
    folderPath = folder.path,
    folderName = folder.name,
    parts = listOf(BookPart(file.name, file)),
    wholeFolder = false,
    folderImages = folder.images,
)

private fun groupLoose(folder: ScannedFolder): List<DetectedBook> {
    if (folder.files.isEmpty()) return emptyList()
    val singles = mutableListOf<ScannedFile>()
    val groups = mutableListOf<List<ScannedFile>>()

    val sagaFiles = mutableSetOf<ScannedFile>()

    // 1. Con capítulos: libro completo.
    val (withChapters, rest) = folder.files.partition { it.meta.chapters.items.isNotEmpty() }
    singles += withChapters

    // 2. Mismo álbum: un libro, salvo varios archivos largos (libros distintos de una saga,
    //    cuyo álbum no es su título).
    val (tagged, untagged) = rest.partition { !it.meta.album.isNullOrBlank() }
    val albumGroups = mutableListOf<MutableList<ScannedFile>>()
    val loose = untagged.toMutableList()
    tagged.groupBy { normalize(it.meta.album!!) }.values.forEach { g ->
        if (g.count { it.meta.durationMs >= LongFileMs } >= 2) {
            loose += g
            sagaFiles += g
        } else {
            albumGroups += g.toMutableList()
        }
    }
    // Una parte sin álbum se une al grupo con el que comparte nombre.
    untagged.forEach { f ->
        albumGroups.firstOrNull { g -> g.any { stemKey(it.name) == stemKey(f.name) } }?.let { it += f; loose -= f }
    }
    groups += albumGroups

    // 3. Mismo nombre salvo la numeración: partes de un libro.
    val stemGroups = loose.filter { stemOf(it.name).any(Char::isDigit) }
        .groupBy { stemKey(it.name) }.values.filter { it.size > 1 }
    groups += stemGroups

    // 4. El resto: uno por archivo.
    singles += loose - stemGroups.flatten().toSet()

    val books = groups.map { it.sortedWith(compareBy(NaturalOrder) { f -> f.name }) }
        .partition { it.size > 1 }
        .let { (multi, one) -> singles += one.flatten(); multi }

    val onlyOne = books.size + singles.size == 1
    val result = books.map { files ->
        val parts = files.map { BookPart(it.name, it) }
        if (onlyOne) whole(folder, parts)
        else DetectedBook(
            identityKey = identityKey(folder.name, parts.map { it.relativePath }),
            path = folder.path,
            folderPath = folder.path,
            folderName = folder.name,
            parts = parts,
            wholeFolder = false,
        )
    } + singles.map { file ->
        // La imagen de carpeta solo vale si la carpeta tiene un único libro.
        single(folder, file)
            .let { if (onlyOne) it else it.copy(folderImages = emptyList()) }
            .let { if (file in sagaFiles) it.copy(albumIsTitle = false) else it }
    }
    return result.sortedWith(compareBy(NaturalOrder) { it.parts.first().file.name })
}

private fun normalize(text: String) = text.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

/** Nombre sin números ni signos: "The Dharma Bums-Part01" y "-Part02" comparten clave. */
private fun stemKey(name: String) = normalize(stemOf(name).replace(Regex("\\d+"), " "))
