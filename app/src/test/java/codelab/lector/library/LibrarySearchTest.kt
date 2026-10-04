package codelab.lector.library

import codelab.lector.data.db.Book
import codelab.lector.data.db.FolderRule
import codelab.lector.data.db.LibraryItem
import codelab.lector.data.db.OnFinish
import codelab.lector.data.db.WorkUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySearchTest {
    private val root = "/storage/emulated/0/Audiobooks"
    private val roots = listOf(root)

    private fun item(
        id: String,
        title: String,
        path: String = "$root/$id",
        author: String? = null,
        narrator: String? = null,
        series: String? = null,
        finished: Boolean = false,
        removed: Boolean = false,
    ) = LibraryItem(
        Book(
            id = id, identityKey = id, totalDurationMs = 1, path = path, title = title, author = author,
            narrator = narrator, series = series, addedAt = 0, finished = finished, speed = 1f, removed = removed,
        ),
        positionInBookMs = 0,
        bookmarkCount = 0,
    )

    private fun search(items: List<LibraryItem>, query: String, filters: Set<LibraryFilter> = emptySet(), rules: List<FolderRule> = emptyList()) =
        buildLibrary(items, rules, filters, LibrarySort.TITLE, searchTerms = searchTerms(query), roots = roots).map {
            when (it) {
                is LibraryEntry.BookEntry -> it.item.book.id
                is LibraryEntry.FolderEntry -> it.name
            }
        }

    @Test
    fun foldKeepsLengthAndIgnoresCaseAndAccents() {
        assertEquals("cancion de pena", fold("Canción DE Peña"))
        assertEquals("Canción DE Peña".length, fold("Canción DE Peña").length)
        assertTrue(searchTerms("   ").isEmpty())
    }

    @Test
    fun findsInEveryFieldButNotInTheLibraryRoot() {
        val items = listOf(
            item("a", "Dune", author = "Frank Herbert"),
            item("b", "Otro", narrator = "Scott Brick"),
            item("c", "Tercero", series = "Fundación"),
            item("d", "Cuarto", path = "$root/Ciencia ficción/Cuarto"),
            item("e", "Nada"),
        )
        assertEquals(listOf("a"), search(items, "HERBERT"))
        assertEquals(listOf("b"), search(items, "brick"))
        assertEquals(listOf("c"), search(items, "fundacion"))
        assertEquals(listOf("d"), search(items, "ficcion"))
        assertTrue(search(items, "audiobooks").isEmpty())
    }

    @Test
    fun everyWordMustMatchAndFiltersStillApply() {
        val items = listOf(
            item("x", "Children of Dune", author = "Frank Herbert", finished = true),
            item("z", "Dune Messiah", author = "Brian Herbert"),
        )
        assertEquals(listOf("x", "z"), search(items, "dune"))
        assertEquals(listOf("x"), search(items, "dune frank"))
        assertEquals(listOf("x"), search(items, "dune", setOf(LibraryFilter.FINISHED)))
    }

    @Test
    fun findsRemovedBooksWithoutTheUnavailableSetting() {
        val items = listOf(item("gone", "Dune", removed = true))
        assertTrue(search(items, "").isEmpty())
        assertEquals(listOf("gone"), search(items, "dune"))
    }

    @Test
    fun folderCardMatchesByItsNameOrAnyMember() {
        val rules = listOf(FolderRule("$root/nidra", WorkUnit.FILE, OnFinish.RESTART))
        val items = listOf(item("s1", "Relajación", path = "$root/nidra/a.mp3"), item("s2", "Sueño", path = "$root/nidra/b.mp3"))
        assertEquals(listOf("nidra"), search(items, "NIDRA", rules = rules))
        assertEquals(listOf("nidra"), search(items, "sueno", rules = rules))
        assertTrue(search(items, "dune", rules = rules).isEmpty())
    }

    @Test
    fun highlightsEveryOccurrence() {
        assertEquals(listOf(0..3, 7..10), searchHighlights("Dune y dune", searchTerms("DUNÉ")))
        assertTrue(searchHighlights("Dune", emptyList()).isEmpty())
    }
}
