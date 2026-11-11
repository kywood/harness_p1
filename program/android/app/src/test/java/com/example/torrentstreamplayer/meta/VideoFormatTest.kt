package com.example.torrentstreamplayer.meta

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class VideoFormatTest {
    @Test fun extensionsAreLowercaseAndUnique() {
        val extensions = VideoFormat.entries.map { it.extension }
        extensions.forEach { assertEquals(it.lowercase(Locale.ROOT), it) }
        extensions.forEach { assertFalse(it.startsWith(".")) }
        assertEquals(extensions.size, extensions.toSet().size)
    }

    @Test fun registeredExtensionsMatchPreviousVideoList() {
        assertEquals(setOf("mp4", "mkv", "avi", "webm", "mov", "m4v", "ts", "flv"),
            VideoFormat.entries.map { it.extension }.toSet())
    }

    @Test fun mimeTypesMatchPreviousStreamServerMapping() {
        val previous = mapOf("mp4" to "video/mp4", "mkv" to "video/x-matroska", "avi" to "video/x-msvideo",
            "webm" to "video/webm", "mov" to "video/quicktime", "m4v" to "video/mp4", "ts" to "video/mp4",
            "flv" to "video/mp4", "zip" to "video/mp4", "" to "video/mp4")
        previous.forEach { (extension, mime) -> assertEquals(extension, mime, VideoFormat.mimeTypeOf("a.$extension")) }
        assertEquals("video/x-matroska", VideoFormat.mimeTypeOf("Movie.MKV"))
        assertEquals("video/mp4", VideoFormat.mimeTypeOf("no-extension"))
    }

    @Test fun moovIndexIsNeededOnlyForPreviousMp4Family() {
        assertEquals(setOf("mp4", "mov", "m4v"),
            VideoFormat.entries.filter { it.needsMoovIndex }.map { it.extension }.toSet())
        assertEquals(VideoFormat.MOV, VideoFormat.ofExtension("MOV"))
        assertNull(VideoFormat.ofExtension("zip"))
    }

    @Test fun lookupUsesLastExtensionAnyCase() {
        assertEquals(VideoFormat.MKV, VideoFormat.of("dir/movie.part.MkV"))
        assertNull(VideoFormat.of("movie.mp4.zip"))
        assertNull(VideoFormat.of("mp4"))
    }
}
