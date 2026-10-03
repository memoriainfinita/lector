package codelab.lector.playback

import codelab.lector.data.db.Book
import codelab.lector.data.db.FolderRule
import codelab.lector.data.db.OnFinish
import codelab.lector.data.db.WorkUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackRulesTest {

    @Test
    fun chainedJumpsUndoToPositionBeforeTheFirst() {
        val undo = JumpUndo(chainMs = 5_000)
        undo.onJump(fromBookMs = 1_000, now = 0)
        undo.onJump(fromBookMs = 50_000, now = 3_000)
        undo.onJump(fromBookMs = 80_000, now = 7_000)
        assertEquals(1_000L, undo.take())
        assertNull(undo.take())
    }

    @Test
    fun jumpAfterTheChainWindowStartsANewChain() {
        val undo = JumpUndo(chainMs = 5_000)
        undo.onJump(fromBookMs = 1_000, now = 0)
        undo.onJump(fromBookMs = 50_000, now = 6_000)
        assertEquals(50_000L, undo.take())
    }

    @Test
    fun finishDependsOnFolderClass() {
        assertEquals(FinishAction.FINISH_THEN_NEXT, finishActionFor(null))
        assertEquals(FinishAction.FINISH, finishActionFor(FolderRule("/p", WorkUnit.FILE, OnFinish.MARK_FINISHED)))
        assertEquals(FinishAction.RESTART, finishActionFor(FolderRule("/m", WorkUnit.FOLDER, OnFinish.RESTART)))
        assertEquals(FinishAction.RESTART, finishActionFor(FolderRule("/s", WorkUnit.FILE, OnFinish.RESTART)))
    }

    @Test
    fun skipCanBeDividedBySpeed() {
        assertEquals(10_000, skipAmountMs(10, 2f, divideBySpeed = false))
        assertEquals(5_000, skipAmountMs(10, 2f, divideBySpeed = true))
    }

    @Test
    fun previousBookmarkSkipsTheOneJustReached() {
        val marks = listOf(10_000L, 40_000L, 90_000L)
        assertEquals(40_000L, previousBookmarkTarget(marks, 60_000))
        assertEquals(10_000L, previousBookmarkTarget(marks, 41_000))
        assertNull(previousBookmarkTarget(marks, 5_000))
    }

    @Test
    fun nextBookFollowsNaturalPathOrderAndSkipsInaccessible() {
        fun book(id: String, path: String, inaccessible: Boolean = false) =
            Book(id = id, identityKey = id, totalDurationMs = 1, path = path, title = id, addedAt = 0, speed = 1f, inaccessible = inaccessible)
        val all = listOf(book("10", "/a/Saga 10"), book("2", "/a/Saga 2"), book("3", "/a/Saga 3", inaccessible = true), book("1", "/a/Saga 1"))
        assertEquals("10", nextBook(all.first { it.id == "2" }, all)?.id)
        assertNull(nextBook(all.first { it.id == "10" }, all))
    }
}
