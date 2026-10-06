package codelab.lector.data.backup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import codelab.lector.data.db.Book
import codelab.lector.data.db.Bookmark
import codelab.lector.data.db.BookmarkKind
import codelab.lector.data.db.CorrectionType
import codelab.lector.data.db.LectorDatabase
import codelab.lector.data.db.LectorDatabase_Impl
import codelab.lector.data.db.SegmentPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImportTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var db: LectorDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder { LectorDatabase_Impl() }
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun book(id: String, identityKey: String = "key-$id", durationMs: Long = 3_600_000, positionAt: Long? = null) = Book(
        id = id,
        identityKey = identityKey,
        totalDurationMs = durationMs,
        path = "/storage/emulated/0/Audiobooks/$id",
        title = id,
        addedAt = 0,
        positionFile = positionAt?.let { "01.mp3" },
        positionMs = if (positionAt != null) 1_000 else 0,
        positionUpdatedAt = positionAt,
        speed = 1f,
    )

    private fun backupBook(
        identityKey: String,
        durationMs: Long = 3_600_000,
        positionAt: Long? = null,
        bookmarks: List<BackupBookmark> = emptyList(),
        speed: Float = 1f,
    ) = BackupBook(
        identityKey = identityKey,
        title = "Libro $identityKey",
        durationMs = durationMs,
        path = "/storage/emulated/0/Audiobooks/$identityKey",
        files = listOf(BackupFile("01.mp3", durationMs / 2), BackupFile("02.mp3", durationMs / 2)),
        positionFile = positionAt?.let { "02.mp3" },
        positionMs = if (positionAt != null) 5_000 else 0,
        positionUpdatedAt = positionAt,
        speed = speed,
        bookmarks = bookmarks,
    )

    private fun mark(id: String, kind: BookmarkKind = BookmarkKind.NORMAL, updatedAt: Long = 10, tags: List<String> = emptyList()) =
        BackupBookmark(id = id, file = "01.mp3", positionMs = 500, kind = kind, createdAt = updatedAt, updatedAt = updatedAt, tags = tags)

    private fun backup(books: List<BackupBook>, corrections: List<BackupCorrection> = emptyList(), settings: Map<String, BackupSetting> = emptyMap()) =
        Backup(exportedAt = 0, appVersion = "test", books = books, corrections = corrections, settings = settings, language = "")

    private fun importer(scope: TestScope) =
        BackupImporter(db, PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { tmp.newFile("settings.preferences_pb") })

    @Test
    fun readBackupRejectsOtherFilesAndNewerVersions() {
        val ok = BackupJson.encodeToString(Backup.serializer(), backup(listOf(backupBook("a"))))
        assertTrue(readBackup(ok) is BackupRead.Ok)
        assertEquals(BackupRead.NotBackup, readBackup("{\"hello\": 1}"))
        assertEquals(BackupRead.NotBackup, readBackup("not json"))
        assertEquals(BackupRead.NotBackup, readBackup(ok.replace("lector-backup", "other")))
        assertEquals(BackupRead.Newer, readBackup(ok.replace("\"version\": 1", "\"version\": 2")))
    }

    @Test
    fun planMatchesBySignatureThenDurationAndCounts() {
        val local = listOf(
            book("a", identityKey = "sig-a", positionAt = 100),
            book("b", identityKey = "other", durationMs = 7_200_400, positionAt = 500),
        )
        val plan = planImport(
            backup(
                listOf(
                    backupBook("sig-a", positionAt = 200, bookmarks = listOf(mark("m1"), mark("m2"), mark("p", BookmarkKind.PAUSE))),
                    backupBook("renamed", durationMs = 7_200_000, positionAt = 300),
                    backupBook("absent", durationMs = 1_000, bookmarks = listOf(mark("m3"))),
                ),
            ),
            local,
            bookmarkIds = setOf("m2"),
        )
        assertEquals(listOf("a", "b", null), plan.matches.map { it.local?.id })
        assertEquals(2, plan.newBookmarks)
        assertEquals(1, plan.existingBookmarks)
        // "a" tiene una más reciente en la copia; la de "b" es más antigua.
        assertEquals(1, plan.newerPositions)
        assertEquals(1, plan.notFound)
    }

    @Test
    fun durationMatchSkipsBooksAlreadyTakenBySignature() {
        val local = listOf(book("a", identityKey = "sig-a"))
        val plan = planImport(backup(listOf(backupBook("sig-x"), backupBook("sig-a"))), local, emptySet())
        // "sig-x" tiene la misma duración, pero "a" es de "sig-a" por firma.
        assertEquals(listOf(null, "a"), plan.matches.map { it.local?.id })
    }

    @Test
    fun combineAddsMissingBooksAsRemovedWithFilesAndBookmarks() = runTest {
        val imp = importer(this)
        val b = backup(listOf(backupBook("absent", positionAt = 50, bookmarks = listOf(mark("m1", tags = listOf("Idea"))))))
        imp.combine(b, imp.plan(b), now = 1_000)

        val added = db.books().all().single()
        assertTrue(added.removed)
        assertEquals("absent", added.identityKey)
        assertEquals("/storage/emulated/0/Audiobooks/absent", added.path)
        assertEquals("02.mp3", added.positionFile)
        assertEquals(listOf("01.mp3", "02.mp3"), db.books().files(added.id).map { it.relativePath })
        assertEquals(1_800_000, db.books().files(added.id).first().durationMs)
        assertEquals(added.id, db.bookmarks().get("m1")?.bookId)
        assertEquals(listOf("Idea"), db.bookmarks().observeTags("m1").first().map { it.name })
    }

    @Test
    fun combineKeepsTheMostRecentPositionWithItsSettings() = runTest {
        db.books().upsert(book("new", identityKey = "sig-new", positionAt = 900))
        db.books().upsert(book("old", identityKey = "sig-old", positionAt = 100))
        val imp = importer(this)
        val b = backup(listOf(backupBook("sig-new", positionAt = 500, speed = 2f), backupBook("sig-old", positionAt = 500, speed = 1.5f)))
        imp.combine(b, imp.plan(b), now = 1_000)

        val kept = db.books().get("new")!!
        assertEquals(900L, kept.positionUpdatedAt)
        assertEquals(1f, kept.speed)
        val taken = db.books().get("old")!!
        assertEquals(500L, taken.positionUpdatedAt)
        assertEquals("02.mp3", taken.positionFile)
        assertEquals(1.5f, taken.speed)
        assertFalse(taken.removed)
    }

    @Test
    fun combineSkipsExistingBookmarksReusesTagsAndKeepsTheNewestPauseMarker() = runTest {
        db.books().upsert(book("a", identityKey = "sig-a"))
        db.bookmarks().upsert(Bookmark("m1", "a", "01.mp3", 0, title = "mine", createdAt = 1, updatedAt = 1))
        db.bookmarks().upsert(Bookmark("p-here", "a", "01.mp3", 0, kind = BookmarkKind.PAUSE, createdAt = 50, updatedAt = 50))
        val imp = importer(this)
        val b = backup(
            listOf(
                backupBook(
                    "sig-a",
                    bookmarks = listOf(
                        mark("m1").copy(title = "theirs"),
                        mark("m2", tags = listOf("idea")),
                        mark("m3", tags = listOf("IDEA", "Otro")),
                        mark("p-old", BookmarkKind.PAUSE, updatedAt = 20),
                    ),
                ),
            ),
        )
        imp.combine(b, imp.plan(b), now = 1_000)

        assertEquals("mine", db.bookmarks().get("m1")?.title)
        assertEquals(setOf("idea", "Otro"), db.tags().observeByUse().first().map { it.name }.toSet())
        assertEquals(listOf("p-here"), db.bookmarks().ofKind("a", BookmarkKind.PAUSE).map { it.id })

        val newer = backup(listOf(backupBook("sig-a", bookmarks = listOf(mark("p-new", BookmarkKind.PAUSE, updatedAt = 90)))))
        imp.combine(newer, imp.plan(newer), now = 1_000)
        assertEquals(listOf("p-new"), db.bookmarks().ofKind("a", BookmarkKind.PAUSE).map { it.id })
    }

    @Test
    fun combineTwiceDoesNotDuplicate() = runTest {
        db.books().upsert(book("a", identityKey = "sig-a"))
        val imp = importer(this)
        val b = backup(
            listOf(backupBook("sig-a", bookmarks = listOf(mark("m1"))), backupBook("absent", durationMs = 1_000, bookmarks = listOf(mark("m2")))),
            corrections = listOf(BackupCorrection(CorrectionType.MERGE, listOf("x", "y"), createdAt = 5, label = "x + y")),
        )
        assertEquals(1, imp.combine(b, imp.plan(b), now = 1_000))
        assertEquals(0, imp.combine(b, imp.plan(b), now = 2_000))
        assertEquals(2, db.books().all().size)
        assertEquals(2, db.bookmarks().count())
        assertEquals(1, db.corrections().all().size)
    }

    @Test
    fun combineMergesSegmentPositionsByDate() = runTest {
        db.books().upsert(book("a", identityKey = "sig-a"))
        db.segmentPositions().save(SegmentPosition("a", "01.mp3", 0, 100, updatedAt = 50))
        db.segmentPositions().save(SegmentPosition("a", "02.mp3", 0, 100, updatedAt = 50))
        val imp = importer(this)
        val b = backup(
            listOf(
                backupBook("sig-a").copy(
                    segments = listOf(BackupSegment("01.mp3", 0, 999, updatedAt = 10), BackupSegment("02.mp3", 0, 999, updatedAt = 90)),
                ),
            ),
        )
        imp.combine(b, imp.plan(b), now = 1_000)
        val byFile = db.segmentPositions().forBook("a").associate { it.file to it.positionMs }
        assertEquals(mapOf("01.mp3" to 100L, "02.mp3" to 999L), byFile)
    }

    @Test
    fun settingsRoundTripWithoutDeviceKeys() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { tmp.newFile("source.preferences_pb") }
        store.edit {
            it[stringPreferencesKey("theme_mode")] = "LIGHT"
            it[floatPreferencesKey("new_book_speed")] = 1.25f
            it[booleanPreferencesKey("show_covers")] = false
            it[stringPreferencesKey("last_book_id")] = "x"
        }
        val exported = settingsOf(store.data.first())
        assertNull(exported["last_book_id"])

        val target = PreferenceDataStoreFactory.create(scope = backgroundScope) { tmp.newFile("check.preferences_pb") }
        BackupImporter(db, target).importSettings(backup(emptyList(), settings = exported))
        val read = target.data.first()
        assertEquals("LIGHT", read[stringPreferencesKey("theme_mode")])
        assertEquals(1.25f, read[floatPreferencesKey("new_book_speed")])
        assertEquals(false, read[booleanPreferencesKey("show_covers")])
        assertNull(read[stringPreferencesKey("last_book_id")])
    }
}
