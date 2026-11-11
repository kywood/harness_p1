package com.example.torrentstreamplayer
import com.example.torrentstreamplayer.ui.BulkSelection
import org.junit.Assert.*
import org.junit.Test

class BulkSelectionTest {
    @Test fun individualSelectionDoesNotAffectOtherItems() {
        val selected = BulkSelection.toggle(setOf("one"), "two", true)
        assertEquals(setOf("one", "two"), selected)
        assertEquals(setOf("two"), BulkSelection.toggle(selected, "one", false))
    }
    @Test fun selectAllThenClearAllAndPartialSelection() {
        val keys = listOf("one", "two")
        assertEquals(keys.toSet(), BulkSelection.all(keys, setOf("one")))
        assertTrue(BulkSelection.all(keys, keys.toSet()).isEmpty())
        assertTrue(BulkSelection.all(emptyList(), emptySet()).isEmpty())
    }
    @Test fun deletesOnlySelectedItemsAndOnlyOnce() {
        val called = mutableListOf<String>()
        val selected = setOf("one", "two", "one")
        val removed = BulkSelection.delete(selected) { called.add(it); true }
        assertEquals(selected, removed)
        assertEquals(listOf("one", "two"), called)
    }
    @Test fun failuresDoNotPreventOtherDeletesAndRemainSelected() {
        val selected = setOf("one", "two", "three", "four")
        val removed = BulkSelection.delete(selected) {
            when (it) { "two" -> false; "three" -> throw IllegalStateException(); else -> true }
        }
        assertEquals(setOf("one", "four"), removed)
        assertEquals(setOf("two", "three"), selected - removed)
    }
    @Test fun emptySelectionDoesNotDeleteAnything() {
        assertTrue(BulkSelection.delete(emptySet()) { fail("No item selected"); true }.isEmpty())
    }
    @Test fun cancellationIsNotReportedAsOrdinaryDeletionFailure() {
        assertThrows(java.util.concurrent.CancellationException::class.java) {
            BulkSelection.delete(setOf("one")) { throw java.util.concurrent.CancellationException() }
        }
    }
}
