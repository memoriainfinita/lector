package codelab.lector.library

import codelab.lector.data.db.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFoldersTest {
    private val primary = "/storage/emulated/0"
    private val sd = "/storage/1234-ABCD"

    private fun book(id: String, path: String, removed: Boolean = false) =
        Book(id = id, identityKey = id, totalDurationMs = 1, path = path, title = id, addedAt = 0, speed = 1f, removed = removed)

    @Test
    fun rowsNameTheFolderFromItsStorageAndCountBooksNotRemoved() {
        val folders = listOf("$primary/Audiobooks", "$primary/Music/Lectures", "$sd/Books", sd)
        val books = listOf(
            book("a", "$primary/Audiobooks/A"),
            book("b", "$primary/Audiobooks/B", removed = true),
            book("c", "$primary/Audiobooks 2/C"),
            book("d", "$primary/Music/Lectures/D"),
        )
        val rows = libraryFolderInfo(folders, books, listOf(primary, sd), primary)
        assertEquals(listOf("Audiobooks", "Music/Lectures", "Books", ""), rows.map { it.name })
        assertEquals(listOf(false, false, true, true), rows.map { it.onSd })
        assertEquals(listOf(1, 1, 0, 0), rows.map { it.books })
    }

    @Test
    fun addingAFolderReplacesTheOnesInsideIt() {
        val folders = listOf("$primary/Music/Audiobooks", "$primary/Music/Lectures", "$primary/Podcasts")
        assertEquals(listOf("$primary/Podcasts", "$primary/Music"), foldersAfterAdding(folders, "$primary/Music"))
        // Dentro de una de la lista: nada cambia.
        assertEquals(folders, foldersAfterAdding(folders, "$primary/Music/Audiobooks/Dune"))
        assertTrue(inLibrary("$primary/Music/Audiobooks/Dune", folders))
        assertFalse(inLibrary("$primary/Music/Audiobooks 2", folders))
    }
}
