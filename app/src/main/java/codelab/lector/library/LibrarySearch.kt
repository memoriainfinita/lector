package codelab.lector.library

import codelab.lector.data.db.LibraryItem
import java.text.Normalizer
import java.util.Locale

/*
 * Búsqueda en la biblioteca (design.md › Pantallas › Biblioteca › D). Sin pantalla aparte: filtra
 * la propia cuadrícula, en memoria, a medida que se escribe.
 */

/**
 * Minúsculas y sin acentos, letra a letra: el resultado tiene la misma longitud que el original,
 * así las posiciones de lo encontrado sirven para resaltar el texto original.
 */
fun fold(text: String): String = buildString(text.length) {
    for (ch in text) {
        val base = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD).firstOrNull() ?: ch
        append(base.toString().lowercase(Locale.ROOT).singleOrNull() ?: ch)
    }
}

/** Palabras de la búsqueda, ya normalizadas. Vacía: no se está buscando. */
fun searchTerms(query: String): List<String> = fold(query).split(Regex("\\s+")).filter { it.isNotEmpty() }

/**
 * Cada palabra tiene que estar en el título, autor, narrador, serie o carpeta (la ruta dentro de la
 * carpeta de la biblioteca). [terms] viene de [searchTerms].
 */
fun matchesSearch(item: LibraryItem, terms: List<String>, roots: List<String>): Boolean {
    val book = item.book
    val fields = listOfNotNull(book.displayTitle, book.author, book.narrator, book.series, folderOf(book.path, roots))
        .joinToString("\n") { fold(it) }
    return terms.all { it in fields }
}

/** Dónde resaltar [terms] en [text]: todas las apariciones. */
fun searchHighlights(text: String, terms: List<String>): List<IntRange> {
    if (terms.isEmpty()) return emptyList()
    val folded = fold(text)
    return terms.flatMap { term ->
        generateSequence(folded.indexOf(term).takeIf { it >= 0 }) { from -> folded.indexOf(term, from + 1).takeIf { it >= 0 } }
            .map { it until it + term.length }
            .toList()
    }
}

/** Ruta del libro dentro de su carpeta de la biblioteca: "Frank Herbert/Dune". */
private fun folderOf(path: String, roots: List<String>): String {
    val root = roots.filter { path == it || path.startsWith("$it/") }.maxByOrNull { it.length } ?: return path
    return path.removePrefix(root).trimStart('/')
}
