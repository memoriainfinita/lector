package codelab.lector.library

import codelab.lector.data.db.Book
import codelab.lector.data.db.BookFile
import codelab.lector.data.db.ChapterInfo
import codelab.lector.data.db.ChapterList
import codelab.lector.data.db.Correction
import codelab.lector.data.db.CorrectionType
import codelab.lector.data.db.CoverSource
import codelab.lector.data.db.FileMeta
import codelab.lector.data.db.FolderRule
import codelab.lector.data.db.OnFinish
import codelab.lector.data.db.WorkUnit
import codelab.lector.data.db.playable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Casos sacados de la biblioteca real del usuario (2026-10-03). */
class DetectionTest {

    private fun file(
        dir: String, name: String, album: String? = null, chapters: Int = 0, durationMs: Long = 3_600_000, size: Long = 1,
        track: Int? = null, disc: Int? = null, unsupported: String? = null,
    ) =
        ScannedFile(
            "$dir/$name", name,
            FileMeta(
                path = "$dir/$name", sizeBytes = size, modifiedAt = 0, durationMs = durationMs, album = album,
                chapters = ChapterList(List(chapters) { ChapterInfo(it * 1000L, it * 1000L + 999, "c$it") }),
                trackNumber = track, discNumber = disc, unsupported = unsupported,
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
    fun unknownDurationsDoNotReconcile() {
        // Un formato no admitido no se lee (duración 0); un libro desaparecido sin leer, tampoco.
        val d = "$root/ape"
        val detected = detectBooks(folder(d, listOf(file(d, "a.ape", durationMs = 0))), emptyList())
        fun book(id: String, duration: Long) =
            Book(id = id, identityKey = "$id-key", totalDurationMs = duration, path = "$root/old/$id.m4b", title = id, addedAt = 0, speed = 1f)
        val r = reconcile(detected, listOf(book("unread", 0), book("short", 400)), listOf(root))
        assertNull(r.matches.single().existing)
        assertEquals(listOf("unread", "short"), r.missing.map { it.id })
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
    fun folderRuleFollowsItsMovedFolder() {
        val old = "$root/yoga nidra"
        val rule = FolderRule(old, WorkUnit.FILE, OnFinish.RESTART)
        val before = mapOf(old to setOf(contentKey(listOf("Session 1.mp3" to 10L, "Session 2.mp3" to 20L))!!))
        fun sessions(dir: String) = folder(dir, (1..2).map { file(dir, "Session $it.mp3", size = it * 10L) })
        // Movida a una carpeta nueva que solo la contiene a ella: la más honda, no "finished".
        val moved = "$root/finished/yoga nidra"
        val tree = folder(root, subs = listOf(folder("$root/finished", subs = listOf(sessions(moved))), sessions("$root/Other")))
        assertEquals(mapOf(old to "$root/Other"), movedRules(listOf(rule), listOf(folder(root, subs = listOf(sessions("$root/Other")))), before) { false })
        val withCopy = movedRules(listOf(rule), listOf(tree), before) { false }
        assertTrue("two copies: none", withCopy.isEmpty())
        val single = folder(root, subs = listOf(folder("$root/finished", subs = listOf(sessions(moved)))))
        assertEquals(mapOf(old to moved), movedRules(listOf(rule), listOf(single), before) { false })
        // La carpeta sigue en su sitio: la regla no se toca.
        assertTrue(movedRules(listOf(rule), listOf(single), before) { true }.isEmpty())
        // Destino con regla propia: no se pisa.
        assertTrue(movedRules(listOf(rule, FolderRule(moved, WorkUnit.FOLDER, OnFinish.RESTART)), listOf(single), before) { it == moved }.isEmpty())
    }

    @Test
    fun correctionsFollowMovedBooks() {
        fun book(id: String, key: String) = Book(id = id, identityKey = key, totalDurationMs = 1, path = "/x", title = id, addedAt = 0, speed = 1f)
        fun files(id: String, vararg f: Pair<String, Long>) = f.mapIndexed { i, (p, s) -> BookFile(bookId = id, relativePath = p, sortIndex = i, durationMs = 1, sizeBytes = s) }

        // Unir: "new/A.mp3" y "new/B.mp3" unidos; B pasa a "finished/" y la firma de B cambia.
        val n = "$root/new"
        val f = "$root/finished"
        val oldA = detectBooks(folder(n, listOf(file(n, "A.mp3", size = 7), file(n, "B.mp3", size = 8))), emptyList())
        val merge = Correction(id = 1, type = CorrectionType.MERGE, identityKeys = oldA.map { it.identityKey }, createdAt = 1)
        val merged = applyCorrections(oldA, listOf(merge)).single()
        val raw = detectBooks(folder(root, subs = listOf(folder(n, listOf(file(n, "A.mp3", size = 7), file(n, "Z.mp3", size = 9))), folder(f, listOf(file(f, "B.mp3", size = 8))))), emptyList())
        val db = listOf(book("m", merged.identityKey))
        val followed = followCorrections(raw, listOf(merge), db, mapOf("m" to files("m", "A.mp3" to 7L, "../finished/B.mp3" to 8L)))
        assertEquals(1, followed.size)
        val again = applyCorrections(raw, followed)
        assertEquals(listOf("A.mp3", "B.mp3"), again.single { it.parts.size == 2 }.parts.map { it.file.name })
        // Una copia de A en otra carpeta, que la base ya conoce como otro libro, no estorba.
        val c = "$root/copy"
        val copy = detectBooks(folder(c, listOf(file(c, "A.mp3", size = 7))), emptyList())
        val withCopy = followCorrections(raw + copy, listOf(merge), db + book("c", copy.single().identityKey), mapOf("m" to files("m", "A.mp3" to 7L, "../finished/B.mp3" to 8L)))
        assertEquals(followed, withCopy)

        // Separar: "Vol 3" separado en 03.mp3; la carpeta pasa a llamarse "Volume 3".
        val v = "$root/Vol 3"
        val old = detectBooks(folder(v, (1..4).map { file(v, "0$it.mp3", size = it.toLong()) }), emptyList()).single()
        val split = Correction(id = 2, type = CorrectionType.SPLIT, identityKeys = listOf(old.identityKey), splitStartFiles = listOf("03.mp3"), createdAt = 2)
        val pieces = applyCorrections(listOf(old), listOf(split))
        val w = "$root/Volume 3"
        val renamed = detectBooks(folder(w, (1..4).map { file(w, "0$it.mp3", size = it.toLong()) }), emptyList())
        val piecesDb = pieces.mapIndexed { i, p -> book("p$i", p.identityKey) }
        val piecesFiles = mapOf("p0" to files("p0", "01.mp3" to 1L, "02.mp3" to 2L), "p1" to files("p1", "03.mp3" to 3L, "04.mp3" to 4L))
        val followedSplit = followCorrections(renamed, listOf(split), piecesDb, piecesFiles)
        assertEquals(listOf(renamed.single().identityKey), followedSplit.single().identityKeys)
        assertEquals(2, applyCorrections(renamed, followedSplit).size)

        // Falta un archivo: la corrección se queda como está.
        val partial = detectBooks(folder(w, (1..3).map { file(w, "0$it.mp3", size = it.toLong()) }), emptyList())
        assertTrue(followCorrections(partial, listOf(split), piecesDb, piecesFiles).isEmpty())
        // Sin mover: nada que cambiar.
        assertTrue(followCorrections(oldA, listOf(merge), db, mapOf("m" to files("m", "A.mp3" to 7L, "B.mp3" to 8L))).isEmpty())
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

    @Test
    fun discFoldersWithTextAfterTheNumber() {
        listOf(
            "CD1", "CD 01", "Disc 1 of 2", "Disco 2 de 3", "CD1 - El comienzo", "Disc 3: The End", "(Disc 2)", "[CD3]",
            "Parte 2", "Part 1 of 3", "guided meditations disc 1", "Introduction to Prana - CD1",
        ).forEach { assertTrue(it, isDiscFolder(it)) }
        listOf(
            "Parte de guerra 3", "Abiding in Mindfulness, Volume 1", "Part 2 - El regreso", "Disco 1980s", "Discovery 2", "CD Collection",
        ).forEach { assertTrue(it, !isDiscFolder(it)) }
        assertEquals(2, discNumber("Disc 2 of 3 - Segunda parte"))
        // Por número de disco, no por nombre: "Alfa - CD2" va detrás de "CD1".
        val b = "$root/Libro"
        val book = detectBooks(
            folder(b, subs = listOf(folder("$b/Alfa - CD2", listOf(file("$b/Alfa - CD2", "01.mp3"))), folder("$b/CD1", listOf(file("$b/CD1", "01.mp3"))))),
            emptyList(),
        ).single()
        assertEquals(listOf("CD1/01.mp3", "Alfa - CD2/01.mp3"), book.parts.map { it.relativePath })
    }

    @Test
    fun filesWithoutNumbersFollowTrackTags() {
        val d = "$root/Sin numeros"
        fun names(vararg files: ScannedFile) = detectBooks(folder(d, files.toList()), emptyList()).single().parts.map { it.relativePath }
        assertEquals(
            listOf("Prólogo.mp3", "El regreso.mp3", "Final.mp3"),
            names(file(d, "El regreso.mp3", album = "X", track = 2), file(d, "Final.mp3", album = "X", track = 3), file(d, "Prólogo.mp3", album = "X", track = 1)),
        )
        // El mismo número en todos los nombres no ordena: discos y pistas.
        assertEquals(
            listOf("1984 - Uno.mp3", "1984 - Dos.mp3", "1984 - Tres.mp3"),
            names(
                file(d, "1984 - Dos.mp3", album = "X", track = 2, disc = 1),
                file(d, "1984 - Tres.mp3", album = "X", track = 1, disc = 2),
                file(d, "1984 - Uno.mp3", album = "X", track = 1, disc = 1),
            ),
        )
        // Nombres numerados mandan aunque las etiquetas digan otra cosa.
        assertEquals(
            listOf("01 Uno.mp3", "02 Dos.mp3"),
            names(file(d, "02 Dos.mp3", album = "X", track = 1), file(d, "01 Uno.mp3", album = "X", track = 2)),
        )
        // Etiquetas incompletas o repetidas: por nombre.
        assertEquals(listOf("Alfa.mp3", "Beta.mp3"), names(file(d, "Beta.mp3", album = "X", track = 1), file(d, "Alfa.mp3", album = "X")))
        assertEquals(listOf("Alfa.mp3", "Beta.mp3"), names(file(d, "Beta.mp3", album = "X", track = 1), file(d, "Alfa.mp3", album = "X", track = 1)))
    }

    @Test
    fun unsupportedFormatsBecomeBooksThatDoNotPlay() {
        val d = "$root/Antiguos"
        val books = detectBooks(
            folder(
                d,
                listOf(
                    file(d, "Viejo libro.wma", unsupported = "WMA"),
                    file(d, "Otro.m4b", chapters = 3, unsupported = "ALAC"),
                    file(d, "Bueno.mp3", chapters = 3),
                ),
            ),
            emptyList(),
        ).map { it.toBook(null, 0, { it.path }, 1f, CoverSource.NONE) }
        assertEquals(mapOf("Bueno" to null, "Otro" to "ALAC", "Viejo libro" to "WMA"), books.associate { it.title to it.unsupportedFormat })
        assertEquals(listOf("Bueno"), books.filter { it.playable }.map { it.title })
        // El siguiente libro salta los que no se reproducen.
        assertNull(codelab.lector.playback.nextBook(books.single { it.title == "Bueno" }, books))
        assertTrue(isDiscFolder("CD1") && "wma" in AudioExtensions && "mka" in AudioExtensions)
    }
}
