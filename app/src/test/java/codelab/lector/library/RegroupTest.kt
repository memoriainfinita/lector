package codelab.lector.library

import codelab.lector.data.db.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Cambio de clase: un libro de 2 archivos pasa a 2 sesiones. */
class RegroupTest {
    private val folder = "/storage/emulated/0/Audiobooks/nidra"

    private fun book(path: String) = Book(id = "old", identityKey = "k", totalDurationMs = 1, path = path, title = "t", addedAt = 0, speed = 1f)

    @Test
    fun baseFolderIsTheBookFolderOrTheFileFolder() {
        assertEquals(folder, baseFolder(book(folder)))
        assertEquals(folder, baseFolder(book("$folder/a.mp3")))
    }

    @Test
    fun everyFileGoesToTheBookThatHasItNow() {
        val placements = mapOf(
            "$folder/a.mp3" to Placement("s1", "a.mp3"),
            "$folder/b.mp3" to Placement("s2", "b.mp3"),
        )
        val moves = regroup(folder, listOf("a.mp3", "b.mp3"), placements)
        assertEquals(Placement("s2", "b.mp3"), moves?.get("b.mp3"))
        assertEquals(2, moves?.size)
    }

    @Test
    fun aMissingFileLeavesTheBookInaccessible() {
        val placements = mapOf("$folder/a.mp3" to Placement("s1", "a.mp3"))
        assertNull(regroup(folder, listOf("a.mp3", "b.mp3"), placements))
        assertNull(regroup(folder, emptyList(), placements))
    }
}
