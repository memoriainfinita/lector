package codelab.lector.library

import codelab.lector.data.db.Book

/*
 * Carpetas de la biblioteca: lista de Ajustes › Biblioteca y explorador (design.md › Ajustes › B).
 */

/**
 * Fila de la lista. [name]: la carpeta, o su ruta desde la raíz del almacenamiento si no es de
 * primer nivel; vacío si es el almacenamiento entero. [books]: los no quitados.
 */
data class LibraryFolderInfo(val path: String, val name: String, val onSd: Boolean, val books: Int)

fun libraryFolderInfo(folders: List<String>, books: List<Book>, storageRoots: List<String>, primary: String): List<LibraryFolderInfo> =
    folders.map { folder ->
        val root = storageRoots.filter { isInside(folder, it) }.maxByOrNull { it.length }
        val name = when (root) {
            null -> folder.substringAfterLast('/')
            folder -> ""
            else -> folder.removePrefix("$root/")
        }
        LibraryFolderInfo(folder, name, onSd = root != null && root != primary, books = books.count { !it.removed && isInside(it.path, folder) })
    }

/** [path] ya está en la biblioteca: es una de sus carpetas o está dentro de una. */
fun inLibrary(path: String, folders: List<String>) = folders.any { isInside(path, it) }

/**
 * Lista tras añadir [path]: las carpetas que contiene salen, para no recorrerlas dos veces. Una
 * que ya está en la biblioteca no cambia nada.
 */
fun foldersAfterAdding(folders: List<String>, path: String): List<String> =
    if (inLibrary(path, folders)) folders else folders.filterNot { isInside(it, path) } + path
