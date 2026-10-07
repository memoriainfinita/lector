package codelab.lector.library

import codelab.lector.data.db.FolderRule
import codelab.lector.data.db.WorkUnit

/** Archivo que por sí solo parece un libro completo, no una parte. */
const val LongFileMs = 3 * 3_600_000L

/**
 * Subcarpeta de disco: "CD1", "disc 2", "Disc 1 of 3", "(Disco 2)", "Parte 3", "Part 1 of 2". Con texto
 * detrás del número, solo cd / disc / disk / disco y tras un separador ("CD1 - El comienzo"): "Part 2 -
 * El regreso" suele ser un libro de una serie, no un disco.
 */
private val DiscName = Regex("""(?i)(?:^|[\s\-_.(\[])(?:cd|disc|disk|disco)[\s\-_.]*(\d+)(?:\s*(?:of|de)\s*\d+)?\s*[)\]]?\s*(?:$|[-–—:_.(\[].*$)""")
private val PartName = Regex("""(?i)(?:^|[\s\-_.(\[])(?:parte|part)[\s\-_.]*(\d+)(?:\s*(?:of|de)\s*\d+)?\s*[)\]]?\s*$""")

/** Número de una subcarpeta de disco; null si no lo es. */
fun discNumber(name: String): Int? = (DiscName.find(name) ?: PartName.find(name))?.groupValues?.get(1)?.toIntOrNull()

fun isDiscFolder(name: String) = discNumber(name) != null

/**
 * Orden de los archivos de un libro: por nombre, en orden natural, si todos llevan número y distinto
 * ("01 - Prólogo", "Part02"). Si no ("Prólogo", "1984 - Epílogo" y "1984 - Prólogo"), por disco y pista
 * de las etiquetas, si todos las tienen y no se repiten. Si tampoco, por nombre.
 */
internal fun orderFiles(files: List<ScannedFile>): List<ScannedFile> {
    val byName = files.sortedWith(compareBy(NaturalOrder) { it.name })
    if (files.size < 2) return byName
    val digits = files.map { stemOf(it.name).filter(Char::isDigit) }
    if (digits.none(String::isEmpty) && digits.toSet().size == digits.size) return byName
    val keys = files.map { f -> f.meta.trackNumber?.let { (f.meta.discNumber ?: 1) to it } }
    if (keys.any { it == null } || keys.toSet().size != keys.size) return byName
    return files.sortedWith(compareBy<ScannedFile> { it.meta.discNumber ?: 1 }.thenBy { it.meta.trackNumber }.thenBy(NaturalOrder) { it.name })
}

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
            .sortedWith(compareBy<ScannedFolder> { discNumber(it.name) }.thenBy(NaturalOrder) { it.name })
        when (rule?.workUnit) {
            WorkUnit.FILE -> {
                val shared = folder.files.size > 1
                folder.files.sortedWith(compareBy(NaturalOrder) { it.name }).forEach {
                    out += single(folder, it).let { b -> if (shared) b.copy(folderImages = folder.images, sharedFolder = true) else b }
                }
                folder.subfolders.forEach(::visit)
                return
            }
            WorkUnit.FOLDER -> {
                partsWithDiscs(folder, discs).takeIf { it.isNotEmpty() }?.let { out += whole(folder, it, discs) }
            }
            null, WorkUnit.DETECT -> {
                if (discs.isNotEmpty()) out += whole(folder, partsWithDiscs(folder, discs), discs)
                else out += groupLoose(folder)
            }
        }
        folder.subfolders.filterNot { it in discs }.forEach(::visit)
    }
    visit(root)
    return out
}

private fun partsWithDiscs(folder: ScannedFolder, discs: List<ScannedFolder>): List<BookPart> =
    orderFiles(folder.files).map { BookPart(it.name, it) } +
        discs.flatMap { d -> orderFiles(d.files).map { BookPart("${d.name}/${it.name}", it) } }

/** Imágenes de la carpeta, de sus discos y de sus subcarpetas sin audio ("Scans", "Artwork"…), en ese orden. */
private fun coverCandidates(folder: ScannedFolder, discs: List<ScannedFolder>): List<String> {
    fun ScannedFolder.sortedImages() = images.sortedWith(NaturalOrder)
    val imageOnly = folder.subfolders.filter { it.files.isEmpty() && it.subfolders.isEmpty() }
        .sortedWith(compareBy(NaturalOrder) { it.name })
    return folder.sortedImages() + (discs + imageOnly).flatMap { sub -> sub.sortedImages().map { "${sub.name}/$it" } }
}

private fun whole(folder: ScannedFolder, parts: List<BookPart>, discs: List<ScannedFolder> = emptyList()) = DetectedBook(
    identityKey = identityKey(folder.name, parts.map { it.relativePath }),
    path = folder.path,
    folderPath = folder.path,
    folderName = folder.name,
    parts = parts,
    wholeFolder = true,
    folderImages = coverCandidates(folder, discs),
)

private fun single(folder: ScannedFolder, file: ScannedFile) = DetectedBook(
    identityKey = identityKey(folder.name, listOf(file.name)),
    path = file.path,
    folderPath = folder.path,
    folderName = folder.name,
    parts = listOf(BookPart(file.name, file)),
    wholeFolder = false,
    folderImages = coverCandidates(folder, emptyList()),
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

    val books = groups.map(::orderFiles)
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
            folderImages = folder.images,
            sharedFolder = true,
        )
    } + singles.map { file ->
        // Con varios libros en la carpeta, su imagen solo vale si lleva el nombre del libro.
        single(folder, file)
            .let { if (onlyOne) it else it.copy(folderImages = folder.images, sharedFolder = true) }
            .let { if (file in sagaFiles) it.copy(albumIsTitle = false) else it }
    }
    return result.sortedWith(compareBy(NaturalOrder) { it.parts.first().file.name })
}

internal fun normalize(text: String) = text.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

/** Nombre sin números ni signos: "The Dharma Bums-Part01" y "-Part02" comparten clave. */
private fun stemKey(name: String) = normalize(stemOf(name).replace(Regex("\\d+"), " "))
