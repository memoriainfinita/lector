package codelab.lector.library

import codelab.lector.data.db.Book
import codelab.lector.data.db.FolderRule
import codelab.lector.data.db.LibraryItem
import codelab.lector.data.db.OnFinish
import codelab.lector.data.db.WorkUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderTreeTest {
    private val root = "/storage/emulated/0/Audiobooks"

    private fun item(id: String, path: String) = LibraryItem(
        Book(id = id, identityKey = id, totalDurationMs = 1, path = path, title = id, addedAt = 0, speed = 1f),
        positionInBookMs = 0,
        bookmarkCount = 0,
    )

    private val items = listOf(
        // Carpeta que es un libro, dentro de una carpeta de autor.
        item("dune", "$root/Frank Herbert/Dune"),
        // Carpeta con varios libros (grupo y archivo suelto): se abre y los muestra dentro.
        item("saga1", "$root/readed"),
        item("saga2", "$root/readed/suelto.m4b"),
        // Libro de un archivo en la raíz.
        item("solo", "$root/Artemis.m4b"),
        // Sesiones.
        item("s1", "$root/nidra/a.mp3"),
        item("s2", "$root/nidra/b.mp3"),
    )
    private val rules = listOf(FolderRule("$root/nidra", WorkUnit.FILE, OnFinish.RESTART))

    @Test
    fun rootWithOneLibraryFolderOpensItDirectly() {
        val content = folderContent(null, items, listOf(root), rules)
        assertEquals(root, content.path)
        assertEquals(listOf("Frank Herbert", "nidra", "readed"), content.folders.map { it.name })
        assertEquals(listOf("solo"), content.books.map { it.book.id })
    }

    @Test
    fun subtitlesTellAuthorClassAndBookCount() {
        val rows = folderContent(root, items, listOf(root), rules).folders.associateBy { it.name }
        assertEquals(FolderSubtitle.Author, rows.getValue("Frank Herbert").subtitle)
        assertEquals(FolderSubtitle.Class(FolderClass.SESSIONS, 2), rows.getValue("nidra").subtitle)
        assertEquals(FolderSubtitle.Books(2), rows.getValue("readed").subtitle)
    }

    @Test
    fun aFolderThatIsABookIsListedInItsParent() {
        val author = folderContent("$root/Frank Herbert", items, listOf(root), rules)
        assertEquals(emptyList<FolderRow>(), author.folders)
        assertEquals(listOf("dune"), author.books.map { it.book.id })
        assertEquals(listOf("saga1", "saga2"), folderContent("$root/readed", items, listOf(root), rules).books.map { it.book.id })
    }

    @Test
    fun goToFolderOpensWhereTheBookIsListed() {
        assertEquals("$root/Frank Herbert", folderToShow("$root/Frank Herbert/Dune", items))
        assertEquals(root, folderToShow("$root/Artemis.m4b", items))
        assertEquals("$root/readed", folderToShow("$root/readed", items))
        assertEquals("$root/nada", folderToShow("$root/nada", items))
    }

    @Test
    fun severalRootsAreListedAtTheTop() {
        val other = "/storage/emulated/0/Music/AUDIOBOOKS"
        val content = folderContent(null, items + item("x", "$other/x.mp3"), listOf(root, other), rules)
        assertNull(content.path)
        assertEquals(listOf("Audiobooks", "AUDIOBOOKS"), content.folders.map { it.name })
    }

    @Test
    fun choosingTheInheritedClassClearsTheOwnRule() {
        val sub = "$root/nidra/cursos"
        assertNull(ruleToStore(sub, FolderClass.SESSIONS, rules))
        assertEquals(FolderRule(sub, WorkUnit.DETECT, OnFinish.MARK_FINISHED), ruleToStore(sub, FolderClass.BOOKS, rules))
        assertNull(ruleToStore("$root/readed", FolderClass.BOOKS, rules))
        assertEquals(FolderClass.BOOKS, classOf(FolderRule(sub, WorkUnit.DETECT, OnFinish.MARK_FINISHED)))
        assertEquals(FolderClass.ALBUMS, classOf(FolderRule(sub, WorkUnit.FOLDER, OnFinish.RESTART)))
    }
}
