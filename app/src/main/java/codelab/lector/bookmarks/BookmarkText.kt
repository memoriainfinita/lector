package codelab.lector.bookmarks

import codelab.lector.data.db.BookmarkKind
import codelab.lector.ui.formatDuration

/**
 * Marcadores como texto (Copiar texto y Exportar; lienzo `Export-Sheet`): título del libro y, por
 * cada marcador, "13:42 · título", la nota y los tags con #. Los libros, separados por una línea.
 * [pauseLabel]: el título del marcador de pausa ("Pausa diferida").
 */
fun bookmarksText(groups: List<Pair<String, List<BookmarkRow>>>, pauseLabel: String): String =
    groups.joinToString("\n\n") { (bookTitle, rows) ->
        (listOf(bookTitle) + rows.map { bookmarkLines(it, pauseLabel) }).joinToString("\n")
    }

private fun bookmarkLines(row: BookmarkRow, pauseLabel: String): String {
    val mark = row.bookmark
    val title = if (mark.kind == BookmarkKind.PAUSE) pauseLabel else mark.title
    val head = listOfNotNull(row.bookMs?.let(::formatDuration), title).joinToString(" · ")
    val tags = row.tags.joinToString(" ") { "#" + it.name.replace(' ', '_') }.ifEmpty { null }
    return listOfNotNull(head, mark.note, tags).joinToString("\n")
}
