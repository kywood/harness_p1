package com.example.torrentstreamplayer.engine

import java.io.EOFException
import java.io.IOException
import java.security.KeyStoreException
import java.security.ProviderException
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryReadStateTest {
    @Test fun failuresThatRepeatOnEveryReadAreBroken() {
        assertEquals(HistoryReadState.BROKEN, HistoryReadState.after(AEADBadTagException()))
        assertEquals(HistoryReadState.BROKEN, HistoryReadState.after(IllegalArgumentException()))
        assertEquals(HistoryReadState.BROKEN, HistoryReadState.after(EOFException()))
    }

    @Test fun failuresThatMayPassLaterKeepTheFile() {
        assertEquals(HistoryReadState.UNREADABLE, HistoryReadState.after(KeyStoreException()))
        assertEquals(HistoryReadState.UNREADABLE, HistoryReadState.after(ProviderException()))
        assertEquals(HistoryReadState.UNREADABLE, HistoryReadState.after(IOException()))
        assertEquals(HistoryReadState.UNREADABLE, HistoryReadState.after(IllegalStateException()))
    }

    @Test fun onlyAnUnreadableFileRefusesWrites() {
        assertTrue(HistoryReadState.READ.writable)
        assertFalse(HistoryReadState.UNREADABLE.writable)
        assertTrue(HistoryReadState.BROKEN.writable && HistoryReadState.BROKEN.keepAside)
        assertFalse(HistoryReadState.READ.keepAside || HistoryReadState.UNREADABLE.keepAside)
    }
}
