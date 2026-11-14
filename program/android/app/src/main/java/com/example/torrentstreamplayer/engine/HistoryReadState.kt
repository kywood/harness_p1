package com.example.torrentstreamplayer.engine

import java.io.EOFException
import java.io.UTFDataFormatException
import javax.crypto.AEADBadTagException

/**
 * What the last read of the saved playlist found, so a write never replaces a list it could not read.
 *
 * @param writable a write may replace the file.
 * @param keepAside the file is moved to `.bak` before the write, because it can never be read again.
 */
internal enum class HistoryReadState(val writable: Boolean, val keepAside: Boolean) {
    /** Read, or there is no file yet. */
    READ(writable = true, keepAside = false),

    /** Failed in a way that may pass later (Keystore busy, I/O error): the file stays and is not written. */
    UNREADABLE(writable = false, keepAside = false),

    /**
     * Can never be read: the GCM tag fails (the Keystore key is gone or the bytes are damaged) or the decrypted
     * bytes are not a playlist. Without moving it aside the user could never build a new list.
     */
    BROKEN(writable = true, keepAside = true);

    companion object {
        /** Failures that give the same result on every retry. Anything else is treated as passing. */
        private val PERMANENT = listOf(AEADBadTagException::class.java, IllegalArgumentException::class.java,
            EOFException::class.java, UTFDataFormatException::class.java)

        fun after(failure: Exception): HistoryReadState =
            if (PERMANENT.any { it.isInstance(failure) }) BROKEN else UNREADABLE
    }
}
