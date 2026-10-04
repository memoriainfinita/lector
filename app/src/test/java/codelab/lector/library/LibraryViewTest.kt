package codelab.lector.library

import codelab.lector.data.db.Book
import codelab.lector.data.db.FolderRule
import codelab.lector.data.db.LibraryItem
import codelab.lector.data.db.OnFinish
import codelab.lector.data.db.WorkUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryViewTest {
    private val root = "/storage/emulated/0/Audiobooks"

    private fun item(
        id: String,
        path: String = "$root/$id",
        added: Long = 0,
        played: Long? = null,
        hidden: Boolean = false,
        finished: Boolean = false,
        position: Long = 0,
        total: Long = 10_000,
        author: String? = null,
        removed: Boolean = false,
    ) = LibraryItem(
        Book(
            id = id, identityKey = id, totalDurationMs = total, path = path, title = id, author = author,
            addedAt = added, lastPlayedAt = played, hiddenFromRecents = hidden, finished = finished, removed = removed,
            positionFile = if (position > 0) "01.mp3" else null, positionMs = position, speed = 1f,
        ),
        positionInBookMs = position,
        bookmarkCount = 0,
    )

    private fun ids(entries: List<LibraryEntry>) = entries.map {
        when (it) {
            is LibraryEntry.BookEntry -> it.item.book.id
            is LibraryEntry.FolderEntry -> it.name
        }
    }

    @Test
    fun recentPutsNeverPlayedAndHiddenLastByAddedDate() {
        val items = listOf(
            item("old", played = 100),
            item("new", played = 200),
            item("hidden", played = 300, hidden = true, added = 5),
            item("fresh", added = 9),
        )
        assertEquals(listOf("new", "old", "fresh", "hidden"), ids(buildLibrary(items, emptyList(), emptySet(), LibrarySort.RECENT)))
    }

    @Test
    fun filtersAreAUnionAndNoneShowsAll() {
        val items = listOf(item("a", position = 10, played = 1), item("b"), item("c", finished = true))
        assertEquals(setOf("a", "c"), ids(buildLibrary(items, emptyList(), setOf(LibraryFilter.IN_PROGRESS, LibraryFilter.FINISHED), LibrarySort.TITLE)).toSet())
        assertEquals(3, buildLibrary(items, emptyList(), emptySet(), LibrarySort.TITLE).size)
    }

    @Test
    fun sortsByAuthorWithoutAuthorLastAndByRemaining() {
        val items = listOf(item("x", author = "Zweig"), item("y"), item("z", author = "Austen", position = 9_000))
        assertEquals(listOf("z", "x", "y"), ids(buildLibrary(items, emptyList(), emptySet(), LibrarySort.AUTHOR)))
        assertEquals(listOf("z", "x", "y"), ids(buildLibrary(items, emptyList(), emptySet(), LibrarySort.REMAINING)))
    }

    @Test
    fun groupsFileClassFoldersByContainingFolder() {
        val rules = listOf(FolderRule("$root/Podcasts", WorkUnit.FILE, OnFinish.MARK_FINISHED), FolderRule("$root/nidra", WorkUnit.FILE, OnFinish.RESTART))
        val items = listOf(
            item("e1", path = "$root/Podcasts/Uno/01.mp3"),
            item("e2", path = "$root/Podcasts/Uno/02.mp3", finished = true),
            item("e3", path = "$root/Podcasts/Dos/01.mp3"),
            item("s1", path = "$root/nidra/a.mp3"),
            item("book"),
        )
        val entries = buildLibrary(items, rules, emptySet(), LibrarySort.TITLE)
        assertEquals(listOf("book", "Dos", "nidra", "Uno"), ids(entries))
        val uno = entries.filterIsInstance<LibraryEntry.FolderEntry>().single { it.name == "Uno" }
        assertEquals(FolderKind.EPISODES, uno.kind)
        assertEquals(2, uno.items.size)
        assertEquals(FolderKind.SESSIONS, entries.filterIsInstance<LibraryEntry.FolderEntry>().single { it.name == "nidra" }.kind)
        // Una carpeta sale si alguno de sus libros pasa el filtro.
        assertEquals(listOf("Uno"), ids(buildLibrary(items, rules, setOf(LibraryFilter.FINISHED), LibrarySort.TITLE)))
    }

    @Test
    fun continueListeningPrefersLoadedThenLastPlayedNotHiddenNorFinished() {
        val items = listOf(item("a", played = 100), item("b", played = 300, finished = true), item("c", played = 400, hidden = true), item("d"))
        assertEquals("d", continueListening(items, "d")?.book?.id)
        assertEquals("a", continueListening(items, null)?.book?.id)
        assertNull(continueListening(listOf(item("d")), null))
    }

    @Test
    fun removedBooksOnlyWithUnavailableAndSubjectToFilters() {
        val items = listOf(item("a"), item("gone", removed = true, finished = true, played = 500))
        assertEquals(listOf("a"), ids(buildLibrary(items, emptyList(), emptySet(), LibrarySort.TITLE)))
        assertEquals(listOf("a", "gone"), ids(buildLibrary(items, emptyList(), emptySet(), LibrarySort.TITLE, showUnavailable = true)))
        assertEquals(listOf("gone"), ids(buildLibrary(items, emptyList(), setOf(LibraryFilter.FINISHED), LibrarySort.TITLE, showUnavailable = true)))
        assertNull(continueListening(listOf(item("gone", removed = true, played = 500)), null))
    }

    @Test
    fun displayPathIsRelativeToStorage() {
        val roots = listOf("/storage/emulated/0", "/storage/1234-ABCD")
        assertEquals("Audiobooks / Frank Herbert", displayPath("/storage/emulated/0/Audiobooks/Frank Herbert", roots))
        assertEquals("Music / AUDIOBOOKS", displayPath("/storage/1234-ABCD/Music/AUDIOBOOKS", roots))
    }
}
