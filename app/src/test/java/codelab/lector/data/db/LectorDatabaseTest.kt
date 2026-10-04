package codelab.lector.data.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class LectorDatabaseTest {
    private lateinit var db: LectorDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder { LectorDatabase_Impl() }
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun book(id: String, identityKey: String = "key-$id", durationMs: Long = 3_600_000) = Book(
        id = id,
        identityKey = identityKey,
        totalDurationMs = durationMs,
        path = "/storage/emulated/0/Audiobooks/$id",
        title = id,
        addedAt = 0,
        speed = 1f,
    )

    private fun bookmark(id: String, bookId: String, kind: BookmarkKind = BookmarkKind.NORMAL) = Bookmark(
        id = id,
        bookId = bookId,
        file = "01.mp3",
        positionMs = 1_000,
        kind = kind,
        createdAt = 0,
        updatedAt = 0,
    )

    @Test
    fun findsBookByIdentityAndByDurationWithinOneSecond() = runTest {
        db.books().upsert(book("a", identityKey = "k1", durationMs = 10_000_000))
        db.books().upsert(book("b", identityKey = "k2", durationMs = 10_002_000))

        assertEquals(listOf("a"), db.books().findByIdentity("k1").map { it.id })
        assertEquals(listOf("a"), db.books().findByDuration(10_000_900).map { it.id })
        assertEquals(listOf("b"), db.books().findByDuration(10_001_500).map { it.id })
        assertTrue(db.books().findByDuration(10_004_000).isEmpty())
    }

    @Test
    fun keepsOnlyLastPauseMarkerPerBook() = runTest {
        db.books().upsert(book("a"))
        db.books().upsert(book("b"))
        db.bookmarks().upsert(bookmark("normal", "a"))
        db.bookmarks().replacePauseMarker(bookmark("p1", "a"))
        db.bookmarks().replacePauseMarker(bookmark("p2", "a"))
        db.bookmarks().replacePauseMarker(bookmark("pb", "b"))

        assertEquals(listOf("p2"), db.bookmarks().ofKind("a", BookmarkKind.PAUSE).map { it.id })
        assertEquals(listOf("pb"), db.bookmarks().ofKind("b", BookmarkKind.PAUSE).map { it.id })
        assertEquals(3, db.bookmarks().count())
    }

    @Test
    fun deletingTagKeepsBookmarks() = runTest {
        db.books().upsert(book("a"))
        db.bookmarks().upsert(bookmark("m", "a"))
        val tagId = db.tags().insert(Tag(name = "idea"))
        db.bookmarks().addTag(BookmarkTag("m", tagId))
        assertEquals(1, db.bookmarks().withTag(tagId).size)

        db.tags().delete(tagId)

        assertEquals(1, db.bookmarks().count())
        assertTrue(db.tags().observeByUse().first().isEmpty())
    }

    @Test
    fun bookWithBookmarksCannotBeDeleted() = runTest {
        db.books().upsert(book("a"))
        db.bookmarks().upsert(bookmark("m", "a"))
        try {
            db.books().delete("a")
            fail("book with bookmarks was deleted")
        } catch (_: Exception) {
        }
        assertEquals("a", db.books().get("a")?.id)
    }

    @Test
    fun folderRuleIsInheritedByNearestAncestorOnly() = runTest {
        val root = "/storage/emulated/0/Audio"
        db.folders().setRule(FolderRule("$root/Meditación", WorkUnit.FILE, OnFinish.RESTART))
        db.folders().setRule(FolderRule("$root/Meditación/Cursos", WorkUnit.FILE, OnFinish.MARK_FINISHED))

        assertEquals(OnFinish.RESTART, db.folders().ruleFor("$root/Meditación")?.onFinish)
        assertEquals(OnFinish.RESTART, db.folders().ruleFor("$root/Meditación/Nidra")?.onFinish)
        assertEquals(OnFinish.MARK_FINISHED, db.folders().ruleFor("$root/Meditación/Cursos/Uno")?.onFinish)
        assertNull(db.folders().ruleFor("$root/Meditación 2"))
        assertNull(db.folders().ruleFor(root))
    }

    @Test
    fun libraryAddsEarlierFilesToPositionAndCountsNormalBookmarks() = runTest {
        db.folders().addFolder(LibraryFolder("/storage/emulated/0/Audiobooks"))
        db.books().upsert(book("a").copy(positionFile = "02.mp3", positionMs = 5_000))
        listOf(1_000_000L, 2_000_000L, 3_000_000L).forEachIndexed { i, ms ->
            db.books().insertFile(BookFile(bookId = "a", relativePath = "0${i + 1}.mp3", sortIndex = i, durationMs = ms, sizeBytes = 0))
        }
        db.books().upsert(book("b"))
        db.bookmarks().upsert(bookmark("m1", "a"))
        db.bookmarks().upsert(bookmark("m2", "a"))
        db.bookmarks().replacePauseMarker(bookmark("p", "a"))

        val items = db.books().observeLibrary().first().associateBy { it.book.id }
        assertEquals(1_005_000L, items.getValue("a").positionInBookMs)
        assertEquals(2, items.getValue("a").bookmarkCount)
        assertEquals(0L, items.getValue("b").positionInBookMs)
        assertEquals(0, items.getValue("b").bookmarkCount)
    }

    @Test
    fun libraryShowsOnlyBooksInsideCurrentFolders() = runTest {
        db.folders().addFolder(LibraryFolder("/storage/emulated/0/Audiobooks"))
        db.books().upsert(book("inside"))
        db.books().upsert(book("file").copy(path = "/storage/emulated/0/Audiobooks/suelto.m4b"))
        db.books().upsert(book("sibling").copy(path = "/storage/emulated/0/Audiobooks 2/x"))
        db.books().upsert(book("removed").copy(path = "/storage/emulated/0/Music/AUDIOBOOKS/y"))

        assertEquals(setOf("inside", "file"), db.books().observeLibrary().first().map { it.book.id }.toSet())
    }

    @Test
    fun storesListColumns() = runTest {
        db.books().upsert(book("a").copy(ownSound = true, preampDb = 3f, eqBands = listOf(1.5f, -2f, 0f)))
        assertEquals(listOf(1.5f, -2f, 0f), db.books().get("a")?.eqBands)

        val id = db.corrections().insert(
            Correction(type = CorrectionType.SPLIT, identityKeys = listOf("k"), splitStartFiles = listOf("05.mp3"), createdAt = 0)
        )
        assertEquals(listOf("05.mp3"), db.corrections().all().single { it.id == id }.splitStartFiles)
    }
}
