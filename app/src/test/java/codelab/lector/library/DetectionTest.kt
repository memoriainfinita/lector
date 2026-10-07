package codelab.lector.library

import codelab.lector.data.db.Book
import codelab.lector.data.db.ChapterInfo
import codelab.lector.data.db.ChapterList
import codelab.lector.data.db.Correction
import codelab.lector.data.db.CorrectionType
import codelab.lector.data.db.CoverSource
import codelab.lector.data.db.FileMeta
import codelab.lector.data.db.FolderRule
import codelab.lector.data.db.OnFinish
import codelab.lector.data.db.WorkUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Casos sacados de la biblioteca real del usuario (2026-10-03). */
class DetectionTest {

    private fun file(dir: String, name: String, album: String? = null, chapters: Int = 0, durationMs: Long = 3_600_000, size: Long = 1) =
        ScannedFile(
            "$dir/$name", name,
            FileMeta(
                path = "$dir/$name", sizeBytes = size, modifiedAt = 0, durationMs = durationMs, album = album,
                chapters = ChapterList(List(chapters) { ChapterInfo(it * 1000L, it * 1000L + 999, "c$it") }),
            ),
        )

    private fun folder(path: String, files: List<ScannedFile> = emptyList(), subs: List<ScannedFolder> = emptyList(), images: List<String> = emptyList()) =
        ScannedFolder(path, path.substringAfterLast('/'), files, images, subs)

    private val root = "/storage/emulated/0/Music/AUDIOBOOKS"

    @Test
    fun looseSingleFileBooksStaySeparate() {
        val d = "$root/readed"
        val readed = folder(
            d,
            listOf(
                file(d, "Andrew Holecek - Dreams of Light.mp3", album = "Dreams of Light", chapters = 91),
                file(d, "Annaka Harris - Lights On Audiobook.mp3", album = "Lights On"),
                file(d, "2 Shift  [Silo -2].mp3"),
                file(d, "Shift The Silo Saga, Book 2.m4b"),
                file(d, "Kamini Desai PhD - Yoga Nidra.mp3", album = "Yoga Nidra", chapters = 22),
            ),
            images = listOf("EmbeddedCover.jpg"),
        )
        val books = detectBooks(readed, emptyList())
        assertEquals(5, books.size)
        assertTrue(books.all { it.isSingleFile })
        assertTrue("folder image not shared by several books", books.all { it.folderCover() == null })
    }

    @Test
    fun folderCoverPrefersNamedCoverAndSkipsBackAndThumbnails() {
        val d = "$root/Abiding in Mindfulness, Volume 1"
        fun coverOf(vararg images: String) =
            detectBooks(folder(d, listOf(file(d, "01.mp3"), file(d, "02.mp3")), images = images.toList()), emptyList()).single().folderCover()
        assertEquals("Folder.jpg", coverOf("AlbumArtSmall.jpg", "Folder.jpg", "Abiding in Mindfulness, Volume 1.jpg"))
        assertEquals("Abiding in Mindfulness, Volume 1-Cover.jpg", coverOf("Abiding in Mindfulness, Volume 1.jpg", "Abiding in Mindfulness, Volume 1-Cover.jpg"))
        assertEquals("Abiding in Mindfulness, Volume 1.jpg", coverOf("booklet.jpg", "Abiding in Mindfulness, Volume 1.jpg"))
        assertEquals("AlbumArt_{B502}_Large.jpg", coverOf("back.jpg", "Back Cover.jpg", "AlbumArt_{B502}_Small.jpg", "AlbumArt_{B502}_Large.jpg"))
        assertNull(coverOf("back.jpg", "AlbumArtSmall.jpg"))
    }

    @Test
    fun folderCoverLooksInDiscsAndImageOnlySubfolders() {
        val k = "$root/PRACTICE/Kornfield"
        val discs = (1..3).map { n ->
            val disc = "$k/disc $n"
            folder(disc, listOf(file(disc, "01.mp3")), images = if (n == 3) listOf("front.jpg") else emptyList())
        }
        assertEquals("disc 3/front.jpg", detectBooks(folder(k, subs = discs), emptyList()).single().folderCover())

        val s = "$root/Ripped"
        val scans = folder("$s/Scans", images = listOf("inlay.jpg", "Cover.jpg"))
        assertEquals("Scans/Cover.jpg", detectBooks(folder(s, listOf(file(s, "01.mp3")), subs = listOf(scans)), emptyList()).single().folderCover())
    }

    @Test
    fun sharedFolderCoverOnlyByBookName() {
        val d = "$root/readed"
        val books = detectBooks(
            folder(
                d,
                listOf(file(d, "The Dharma Bums.mp3"), file(d, "Dust.mp3", album = "Dust The Silo Saga"), file(d, "Shift.mp3")),
                images = listOf("cover.jpg", "The Dharma Bums.jpg", "Dust The Silo Saga.png"),
            ),
            emptyList(),
        )
        assertEquals("The Dharma Bums.jpg", books.single { it.parts[0].file.name == "The Dharma Bums.mp3" }.folderCover())
        assertEquals("Dust The Silo Saga.png", books.single { it.parts[0].file.name == "Dust.mp3" }.folderCover())
        assertNull(books.single { it.parts[0].file.name == "Shift.mp3" }.folderCover())

        val s = "$root/yoga nidra"
        val sessions = detectBooks(
            folder(s, (1..2).map { file(s, "Session $it.mp3") }, images = listOf("cover.jpg")),
            listOf(FolderRule(s, WorkUnit.FILE, OnFinish.RESTART)),
        )
        assertTrue(sessions.all { it.folderCover() == null })
    }

    @Test
    fun partsWithSameAlbumAreOneBook() {
        val d = "$root/Jack Kerouac - The Dharma Bums"
        val books = detectBooks(folder(d, (1..4).map { file(d, "The Dharma Bums-Part0$it.mp3", album = "The Dharma Bums") }), emptyList())
        assertEquals(1, books.size)
        assertEquals(d, books[0].path)
        assertEquals(listOf("The Dharma Bums-Part01.mp3", "The Dharma Bums-Part02.mp3", "The Dharma Bums-Part03.mp3", "The Dharma Bums-Part04.mp3"), books[0].parts.map { it.relativePath })
    }

    @Test
    fun numberedPartsWithoutAlbumAreOneBookInNaturalOrder() {
        val d = "$root/Untagged"
        val books = detectBooks(folder(d, listOf(10, 2, 1).map { file(d, "Book part $it.mp3") }), emptyList())
        assertEquals(1, books.size)
        assertEquals(listOf("Book part 1.mp3", "Book part 2.mp3", "Book part 10.mp3"), books[0].parts.map { it.relativePath })
    }

    @Test
    fun m4bPartsWithoutChaptersAreOneBook() {
        val d = "$root/Shift into Freedom"
        val books = detectBooks(folder(d, (1..12).map { file(d, "Shift into Freedom (Unabridged) - %03d.m4b".format(it), album = "Shift into Freedom", durationMs = 600_000) }), emptyList())
        assertEquals(1, books.size)
        assertEquals(12, books[0].parts.size)
    }

    @Test
    fun partMissingAlbumStillJoinsByName() {
        val d = "$root/Abiding in Mindfulness, Volume 2"
        val files = listOf(file(d, "Abiding In Mindfulness- 11.mp3")) +
            (12..15).map { file(d, "Abiding In Mindfulness- $it.mp3", album = "Abiding in Mindfulness, Volume 2") }
        assertEquals(1, detectBooks(folder(d, files), emptyList()).size)
    }

    @Test
    fun longBooksSharingSeriesAlbumStaySeparate() {
        val d = "$root/dune"
        val names = listOf("00.01 The Butlerian Jihad.m4b", "00.02 The Machine Crusade.m4b", "00.03 The Battle of Corrin.m4b")
        val files = names.map { file(d, it, album = "The New Dune Chronicles", durationMs = 85_000_000) } +
            listOf(file(d, "Mentats of Dune, Part 1.m4b", durationMs = 40_000_000), file(d, "Mentats of Dune, Part 2.m4b", durationMs = 40_000_000))
        val books = detectBooks(folder(d, files), emptyList())
        assertEquals(4, books.size)
        assertEquals(2, books.single { !it.isSingleFile }.parts.size)
    }

    @Test
    fun chapterTitledPartsJoinByAlbum() {
        val d = "$root/Santideva - The Bodhicaryavatara"
        val names = listOf("00. Background.mp3", "01. Praise of the Awakening Mind.mp3", "13. Praise of the Awakening Mind.mp3", "22. Dedication.mp3")
        val dune = "$root/Dune - 1965"
        val cds = listOf("Dune CD01.mp3", "Dune CD02.mp3", "Dune CD21a.mp3", "Dune CD21b-Appendix.mp3")
        assertEquals(1, detectBooks(folder(d, names.map { file(d, it, album = "The Bodhicaryavatara", durationMs = 600_000) }), emptyList()).size)
        assertEquals(1, detectBooks(folder(dune, cds.map { file(dune, it, album = "Dune (Unabridged)") }), emptyList()).size)
    }

    @Test
    fun sagaBooksDoNotTakeAlbumAsTitle() {
        val d = "$root/dune"
        val books = detectBooks(
            folder(d, listOf("00.01 The Butlerian Jihad.m4b", "00.02 The Machine Crusade.m4b").map { file(d, it, album = "The New Dune Chronicles", durationMs = 85_000_000) }),
            emptyList(),
        )
        assertTrue(books.none { it.albumIsTitle })
        val title = books[0].toBook(null, 0, { "id" }, 1f, codelab.lector.data.db.CoverSource.NONE).title
        assertEquals("00.01 The Butlerian Jihad", title)
    }

    @Test
    fun discSubfoldersJoinVolumesStaySeparate() {
        val k = "$root/PRACTICE/Kornfield"
        val kornfield = folder(k, subs = (1..3).map { n ->
            val disc = "$k/guided meditations disc $n"
            folder(disc, listOf(file(disc, "01.mp3"), file(disc, "02.mp3")))
        })
        val a = "$root/Abiding"
        val abiding = folder(a, subs = (1..3).map { n ->
            val v = "$a/Abiding in Mindfulness, Volume $n"
            folder(v, listOf(file(v, "01.mp3", album = "Vol $n"), file(v, "02.mp3", album = "Vol $n")))
        })
        val books = detectBooks(folder(root, subs = listOf(kornfield, abiding)), emptyList())
        val korn = books.single { it.path == k }
        assertEquals(6, korn.parts.size)
        assertEquals("guided meditations disc 1/01.mp3", korn.parts.first().relativePath)
        assertEquals(3, books.count { it.path.startsWith(a) })
        assertTrue(isDiscFolder("Introduction to Prana - CD1"))
        assertTrue(!isDiscFolder("Abiding in Mindfulness, Volume 1"))
    }

    @Test
    fun folderClassesOverrideDetection() {
        val s = "$root/yoga nidra"
        val sessions = folder(s, (1..3).map { file(s, "Session $it.mp3", album = "Nidra") })
        val al = "$root/Album"
        val album = folder(al, listOf(file(al, "Track A.mp3"), file(al, "Other.mp3")))
        val rules = listOf(
            FolderRule(s, WorkUnit.FILE, OnFinish.RESTART),
            FolderRule(al, WorkUnit.FOLDER, OnFinish.RESTART),
        )
        val books = detectBooks(folder(root, subs = listOf(sessions, album)), rules)
        assertEquals(3, books.count { it.folderPath == s && it.isSingleFile })
        assertEquals(1, books.count { it.path == al })
        assertNull(ruleFor("$root/yoga nidra 2", rules))
    }

    @Test
    fun identityIgnoresLocationAndOrder() {
        val a = identityKey("Dune", listOf("02.mp3", "01.mp3"))
        assertEquals(a, identityKey("Dune", listOf("01.mp3", "02.mp3")))
        assertNotEquals(a, identityKey("Dune 2", listOf("01.mp3", "02.mp3")))
    }

    @Test
    fun correctionsMergeAndSplit() {
        val d = "$root/Saga"
        val books = detectBooks(folder(d, listOf(file(d, "Part A.m4b"), file(d, "Part B.m4b"))), emptyList())
        assertEquals(2, books.size)
        val merged = applyCorrections(books, listOf(Correction(type = CorrectionType.MERGE, identityKeys = books.map { it.identityKey }, createdAt = 1)))
        assertEquals(1, merged.size)
        assertEquals(listOf("Part A.m4b", "Part B.m4b"), merged[0].parts.map { it.relativePath })

        val split = applyCorrections(
            merged,
            listOf(Correction(type = CorrectionType.SPLIT, identityKeys = listOf(merged[0].identityKey), splitStartFiles = listOf("Part B.m4b"), createdAt = 2)),
        )
        assertEquals(2, split.size)
        assertTrue(split.all { it.isSingleFile })
    }

    @Test
    fun splitPiecesSharingTheAlbumAreNumbered() {
        val d = "$root/Vol 3"
        val books = detectBooks(folder(d, (1..4).map { file(d, "0$it.mp3", album = "Vol 3 ") }), emptyList())
        assertEquals(1, books.size)
        val split = applyCorrections(
            books,
            listOf(Correction(type = CorrectionType.SPLIT, identityKeys = listOf(books[0].identityKey), splitStartFiles = listOf("03.mp3"), createdAt = 1)),
        )
        val titles = split.map { it.toBook(null, 0, { "id" }, 1f, CoverSource.NONE).title }
        assertEquals(listOf("Vol 3 (1/2)", "Vol 3 (2/2)"), titles)
    }

    @Test
    fun segmentStartIsTheChapterOrTheFile() {
        val chapters = listOf(ChapterInfo(0, 1_000, "a"), ChapterInfo(1_000, 5_000, "b"))
        assertEquals(1_000, segmentStart(chapters, 2_500))
        assertEquals(0, segmentStart(emptyList(), 2_500))
    }

    @Test
    fun reconcileByIdentityThenDuration() {
        val d = "$root/Moved"
        val detected = detectBooks(folder(d, listOf(file(d, "a.m4b", durationMs = 50_000_000), file(d, "b.m4b", durationMs = 70_000_000))), emptyList())
        fun book(id: String, key: String, duration: Long) =
            Book(id = id, identityKey = key, totalDurationMs = duration, path = "/old", title = id, addedAt = 0, speed = 1f)
        val existing = listOf(
            book("same", detected[0].identityKey, 50_000_000),
            book("renamed", "old-key", 70_000_400),
            book("gone", "gone-key", 1_000),
        )
        val r = reconcile(detected, existing, listOf("/old"))
        assertEquals("same", r.matches[0].existing?.id)
        assertEquals("renamed", r.matches[1].existing?.id)
        assertEquals(listOf("gone"), r.missing.map { it.id })
    }

    @Test
    fun movedBooksReconnectByContentBeforeDuration() {
        // "new/A.mp3" pasa a "finished/A.mp3": la firma cambia (lleva el nombre de la carpeta).
        val f = "$root/finished"
        val detected = detectBooks(
            folder(f, listOf(file(f, "A.mp3", durationMs = 50_000_000, size = 700), file(f, "B.mp3", durationMs = 60_000_000, size = 800))),
            emptyList(),
        )
        fun book(id: String, path: String, duration: Long) =
            Book(id = id, identityKey = "$id-key", totalDurationMs = duration, path = path, title = id, addedAt = 0, speed = 1f)
        val existing = listOf(
            // Otro libro desaparecido con la misma duración que A: sin el contenido, se emparejaría por duración.
            book("lookalike", "$root/old/Lookalike.mp3", 50_000_000),
            book("a", "$root/new/A.mp3", 50_000_000),
            // Fuera de las carpetas de la biblioteca: por contenido también se reconecta.
            book("b", "/removed/B.mp3", 60_000_000),
        )
        val contents = mapOf(
            "lookalike" to contentKey(listOf("Lookalike.mp3" to 999L))!!,
            "a" to contentKey(listOf("A.mp3" to 700L))!!,
            "b" to contentKey(listOf("B.mp3" to 800L))!!,
        )
        val r = reconcile(detected, existing, listOf(root), contents)
        assertEquals(listOf("a", "b"), r.matches.map { it.existing?.id })
        assertEquals(listOf("lookalike"), r.missing.map { it.id })
        // Sin tamaños (libro importado) no hay huella.
        assertNull(contentKey(listOf("A.mp3" to 0L)))
        // Las carpetas no cuentan: un libro de discos movido o con la carpeta renombrada da la misma huella.
        assertEquals(contentKey(listOf("CD1/01.mp3" to 5L)), contentKey(listOf("01.mp3" to 5L)))
    }

    @Test
    fun booksOfARemovedFolderAreLeftAlone() {
        val d = "$root/New"
        val detected = detectBooks(folder(d, listOf(file(d, "a.m4b", durationMs = 50_000_000), file(d, "b.m4b", durationMs = 70_000_000))), emptyList())
        fun book(id: String, path: String, key: String, duration: Long) =
            Book(id = id, identityKey = key, totalDurationMs = duration, path = path, title = id, addedAt = 0, speed = 1f)
        val existing = listOf(
            // Movido desde la carpeta quitada a la biblioteca: se reconecta por firma.
            book("moved", "/removed/Moved", detected[0].identityKey, 50_000_000),
            // Misma duración que uno nuevo, pero de la carpeta quitada: no se empareja.
            book("other", "/removed/Other", "other-key", 70_000_000),
            book("lost", "$root/Lost", "lost-key", 1_000),
            book("sdOut", "/storage/SD/Book", "sd-key", 2_000),
        )
        val r = reconcile(detected, existing, listOf(root, "/storage/SD"))
        assertEquals("moved", r.matches[0].existing?.id)
        assertEquals(null, r.matches[1].existing)
        assertEquals(listOf("lost", "sdOut"), r.missing.map { it.id })
    }
}
