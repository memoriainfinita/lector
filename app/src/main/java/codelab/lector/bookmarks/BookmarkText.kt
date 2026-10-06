package codelab.lector.bookmarks

import codelab.lector.ui.formatDuration

/**
 * Marcadores como texto (Copiar texto y Exportar; lienzo `Export-Sheet`): título del libro y, por
 * cada marcador, "13:42 · título", la nota y los tags con #. Los libros, separados por una línea.
 */
fun bookmarksText(groups: List<Pair<String, List<BookmarkRow>>>): String =
    groups.joinToString("\n\n") { (bookTitle, rows) ->
        (listOf(bookTitle) + rows.map(::bookmarkLines)).joinToString("\n")
    }

private fun bookmarkLines(row: BookmarkRow): String {
    val mark = row.bookmark
    val head = listOfNotNull(row.bookMs?.let(::formatDuration), mark.title).joinToString(" · ")
    val tags = row.tags.joinToString(" ") { "#" + it.name.replace(' ', '_') }.ifEmpty { null }
    return listOfNotNull(head, mark.note, tags).joinToString("\n")
}
