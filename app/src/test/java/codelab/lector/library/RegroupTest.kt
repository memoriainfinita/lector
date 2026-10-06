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
    fun carryOverTakesSettingsFromTheLatestAndFinishedOnlyIfAll() {
        val a = book(folder).copy(id = "a", speed = 1.5f, ownSound = true, preampDb = 3f, finished = true, positionUpdatedAt = 100)
        val b = book(folder).copy(id = "b", speed = 2f, skipSilence = true, finished = false, positionUpdatedAt = 200)
        // Separar: un solo antiguo, pasa tal cual.
        val split = carryOver(listOf(a))!!
        assertEquals(1.5f, split.speed)
        assertEquals(3f, split.preampDb)
        assertEquals(true, split.finished)
        // Juntar: ajustes del más reciente; terminado solo si lo estaban todos.
        val merged = carryOver(listOf(a, b))!!
        assertEquals(2f, merged.speed)
        assertEquals(true, merged.skipSilence)
        assertEquals(false, merged.ownSound)
        assertEquals(false, merged.finished)
        assertNull(carryOver(emptyList()))
    }

    @Test
    fun pathsOfABookMergedWithAnotherFolderAreNormalized() {
        assertEquals("/a/Vol 2/01.mp3", normalizePath("/a/Vol 1/../Vol 2/01.mp3"))
        assertEquals("/a/b.mp3", normalizePath("/a/./b.mp3"))
        // Deshacer la unión: los archivos de la otra carpeta se encuentran en su libro.
        val placements = mapOf(
            "$folder/a.mp3" to Placement("v1", "a.mp3"),
            "/storage/emulated/0/Audiobooks/vol2/b.mp3" to Placement("v2", "b.mp3"),
        )
        val moves = regroup(folder, listOf("a.mp3", "../vol2/b.mp3"), placements)
        assertEquals(Placement("v2", "b.mp3"), moves?.get("../vol2/b.mp3"))
    }

    @Test
    fun aMissingFileLeavesTheBookInaccessible() {
        val placements = mapOf("$folder/a.mp3" to Placement("s1", "a.mp3"))
        assertNull(regroup(folder, listOf("a.mp3", "b.mp3"), placements))
        assertNull(regroup(folder, emptyList(), placements))
    }
}
