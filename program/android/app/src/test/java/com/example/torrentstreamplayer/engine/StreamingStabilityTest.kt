package com.example.torrentstreamplayer.engine
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.io.File
import java.io.IOException

class StreamingStabilityTest {
    @Test fun extendedSizeMp4IndexIsSupported() {
        val bytes = ByteBuffer.allocate(24).putInt(1).put("moov".toByteArray()).putLong(24).putLong(0).array()
        assertEquals(Mp4IndexProbe.Found(0, 24), Mp4IndexProbe.inspect(24) { pos, count ->
            bytes.copyOfRange(pos.toInt(), pos.toInt() + count)
        })
    }
    private fun box(type: String, payload: Int = 0): ByteArray =
        ByteBuffer.allocate(8 + payload).putInt(8 + payload).put(type.toByteArray()).put(ByteArray(payload)).array()
    @Test fun fastStartMp4FindsHeadIndexWithoutReadingTail() {
        val bytes = box("ftyp") + box("moov", 8) + box("mdat", 100)
        val reads = mutableListOf<Long>()
        val result = Mp4IndexProbe.inspect(bytes.size.toLong()) { pos, count ->
            reads.add(pos); bytes.copyOfRange(pos.toInt(), pos.toInt() + count)
        }
        assertEquals(Mp4IndexProbe.Found(8, 16), result)
        assertEquals(listOf(0L, 8L), reads)
    }
    @Test fun tailIndexSkipsMediaPayloadAndRequestsOnlyActualIndex() {
        val bytes = box("ftyp") + box("mdat", 56) + box("moov", 8)
        assertEquals(Mp4IndexProbe.Need(72, 8), Mp4IndexProbe.inspect(bytes.size.toLong()) { pos, count ->
            if (pos >= 72) null else bytes.copyOfRange(pos.toInt(), pos.toInt() + count)
        })
        val file = File.createTempFile("startup-index", ".mp4")
        try {
            file.writeBytes(bytes)
            val state = StartupIndexState(file, bytes.size.toLong(), 0, 16, "mp4")
            val have = mutableSetOf(0)
            assertEquals(setOf(4), state.update(have::contains)); assertFalse(state.ready)
            have.add(4)
            assertEquals(setOf(4, 5), state.update(have::contains)); assertFalse(state.ready)
            have.add(5)
            state.update(have::contains); assertTrue(state.ready)
        } finally { file.delete() }
    }
    @Test fun malformedBoxesFallBackInsteadOfWaitingForever() {
        val malformed = ByteBuffer.allocate(8).putInt(4).put("moov".toByteArray()).array()
        assertEquals(Mp4IndexProbe.Unknown, Mp4IndexProbe.inspect(8) { _, _ -> malformed })
        assertEquals(Mp4IndexProbe.Unknown, Mp4IndexProbe.inspect(2) { _, _ -> fail("No header"); null })
    }
    @Test fun simultaneousReadDemandsRemainIndependentAndStaleCloseIsSafe() {
        val registry = ReadDemandRegistry()
        val video = registry.open(10)
        val index = registry.open(900)
        registry.update(video, 20)
        assertEquals(listOf(20L, 900L), registry.offsets())
        registry.clear()
        val newVideo = registry.open(500)
        registry.update(video, 30); registry.close(index)
        assertEquals(listOf(500L), registry.offsets())
        registry.close(newVideo); assertTrue(registry.offsets().isEmpty())
    }
    @Test fun cachedSeekKeepsReadingButMissingOldRangeDoesNotWait() {
        val file = File.createTempFile("cached-seek", ".bin")
        try {
            file.writeBytes(byteArrayOf(1, 2, 3, 4))
            VerifiedTorrentInputStream(file, 0, 4, { true }, { fail("No download needed") },
                { true }, { _, count -> count }, canWait = { false }).use {
                assertArrayEquals(file.readBytes(), it.readBytes())
            }
            VerifiedTorrentInputStream(file, 0, 4, { false }, { fail("Old request must not download") },
                { true }, { _, count -> count }, canWait = { false }).use {
                assertThrows(IOException::class.java) { it.read() }
            }
        } finally { file.delete() }
    }
}
