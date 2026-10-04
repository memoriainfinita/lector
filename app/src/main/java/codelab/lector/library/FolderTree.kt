package codelab.lector.library

import codelab.lector.data.db.Book
import codelab.lector.data.db.FolderRule
import codelab.lector.data.db.LibraryItem
import codelab.lector.data.db.OnFinish
import codelab.lector.data.db.WorkUnit

/*
 * Vista Carpetas (design.md › Pantallas › Biblioteca › B): el árbol sale de las rutas de los libros
 * guardados, sin recorrer el disco.
 */

/** Clase de carpeta (design.md › Modelo de datos › Clases de carpeta). */
enum class FolderClass(val workUnit: WorkUnit, val onFinish: OnFinish) {
    BOOKS(WorkUnit.DETECT, OnFinish.MARK_FINISHED),
    EPISODES(WorkUnit.FILE, OnFinish.MARK_FINISHED),
    ALBUMS(WorkUnit.FOLDER, OnFinish.RESTART),
    SESSIONS(WorkUnit.FILE, OnFinish.RESTART),
}

/** Sin regla, o con la regla "Libros", la clase es Libros. */
fun classOf(rule: FolderRule?): FolderClass = when {
    rule == null || rule.workUnit == WorkUnit.DETECT -> FolderClass.BOOKS
    rule.workUnit == WorkUnit.FOLDER -> FolderClass.ALBUMS
    rule.onFinish == OnFinish.RESTART -> FolderClass.SESSIONS
    else -> FolderClass.EPISODES
}

/**
 * Regla que hay que guardar para [folder] al elegir [chosen]: null si coincide con la heredada de la
 * carpeta madre (se quita la propia), si no la regla de esa clase.
 */
fun ruleToStore(folder: String, chosen: FolderClass, rules: List<FolderRule>): FolderRule? {
    val inherited = classOf(ruleFor(parentOf(folder), rules.filter { it.folderPath != folder }))
    return if (chosen == inherited) null else FolderRule(folder, chosen.workUnit, chosen.onFinish)
}

fun parentOf(path: String): String = path.substringBeforeLast('/', "")

/** Carpeta donde aparece el libro: la que contiene su archivo o, si la carpeta entera es el libro, su madre. */
fun listingFolder(book: Book, bookFolders: Set<String>): String = when {
    extensionOf(book.path) in AudioExtensions -> parentOf(book.path)
    book.path in bookFolders -> parentOf(book.path)
    else -> book.path
}

/**
 * Carpetas que son un libro: la ruta de un solo libro sin otros libros dentro. Las de varios libros
 * (archivos sueltos agrupados) se abren y los muestran dentro.
 */
fun bookFolders(books: List<Book>): Set<String> {
    val folderPaths = books.map { it.path }.filter { extensionOf(it) !in AudioExtensions }
    val counts = folderPaths.groupingBy { it }.eachCount()
    return counts.filter { (path, n) -> n == 1 && books.none { it.path.startsWith("$path/") } }.keys
}

sealed interface FolderSubtitle {
    data class Class(val folderClass: FolderClass, val works: Int) : FolderSubtitle
    data object Author : FolderSubtitle
    data class Books(val count: Int) : FolderSubtitle
}

data class FolderRow(val path: String, val name: String, val subtitle: FolderSubtitle)

/** Contenido de una carpeta: subcarpetas con libros dentro y libros que aparecen en ella. */
data class FolderContent(val path: String?, val folders: List<FolderRow>, val books: List<LibraryItem>)

/**
 * Lo que se ve en [path]. Con null, la raíz: las carpetas de la biblioteca; con una sola, su
 * contenido directamente.
 */
fun folderContent(path: String?, items: List<LibraryItem>, roots: List<String>, rules: List<FolderRule>): FolderContent {
    if (path == null && roots.size == 1) return folderContent(roots.single(), items, roots, rules)
    val bookFolders = bookFolders(items.map { it.book })
    val listed = items.map { it to listingFolder(it.book, bookFolders) }
    if (path == null) {
        val rows = roots.map { root -> folderRow(root, listed, rules) }
        return FolderContent(null, rows, emptyList())
    }
    val books = listed.filter { it.second == path }.map { it.first }
        .sortedWith(compareBy(NaturalOrder) { it.book.path })
    val children = listed.mapNotNull { (_, folder) ->
        folder.takeIf { it.startsWith("$path/") }?.let { "$path/" + it.removePrefix("$path/").substringBefore('/') }
    }.distinct().sortedWith(NaturalOrder)
    return FolderContent(path, children.map { folderRow(it, listed, rules) }, books)
}

private fun folderRow(path: String, listed: List<Pair<LibraryItem, String>>, rules: List<FolderRule>): FolderRow {
    val inside = listed.filter { it.second == path || it.second.startsWith("$path/") }
    val folderClass = classOf(ruleFor(path, rules))
    val subtitle = when {
        folderClass != FolderClass.BOOKS -> FolderSubtitle.Class(folderClass, inside.size)
        // De autor: sin audio propio (archivos o grupos de la carpeta); las subcarpetas-libro no cuentan.
        inside.none { (item, _) -> item.book.path == path || parentOf(item.book.path) == path && extensionOf(item.book.path) in AudioExtensions } ->
            FolderSubtitle.Author
        else -> FolderSubtitle.Books(inside.size)
    }
    return FolderRow(path, path.substringAfterLast('/'), subtitle)
}

/** Carpeta que hay que abrir para "Ir a la carpeta": donde aparece el libro con esa ruta, o la ruta misma. */
fun folderToShow(path: String, items: List<LibraryItem>): String {
    val book = items.firstOrNull { it.book.path == path }?.book ?: return path
    return listingFolder(book, bookFolders(items.map { it.book }))
}

/** Raíz de la biblioteca que contiene [path], para no subir por encima de ella. */
fun rootOf(path: String, roots: List<String>): String? =
    roots.filter { path == it || path.startsWith("$it/") }.maxByOrNull { it.length }
