package com.example.torrentstreamplayer.meta

import java.util.Locale

/** Enum entries cannot read their own companion while being built, so the fallback lives here. */
private const val FALLBACK_MIME_TYPE = "video/mp4"

/**
 * Video file formats the app plays. Register a new format here; video detection, the stream
 * server's MIME type and the startup index check all look it up, so callers need no changes.
 *
 * [mimeType] keeps the values the stream server sent before this table existed: formats it had
 * no entry for (m4v, ts, flv) were served as [DEFAULT_MIME_TYPE].
 */
internal enum class VideoFormat(
    val extension: String,
    val mimeType: String,
    /** True when playback must wait for the MP4 `moov` index (see `StartupIndexState`). */
    val needsMoovIndex: Boolean = false,
) {
    MP4("mp4", "video/mp4", needsMoovIndex = true),
    MKV("mkv", "video/x-matroska"),
    AVI("avi", "video/x-msvideo"),
    WEBM("webm", "video/webm"),
    MOV("mov", "video/quicktime", needsMoovIndex = true),
    M4V("m4v", FALLBACK_MIME_TYPE, needsMoovIndex = true),
    TS("ts", FALLBACK_MIME_TYPE),
    FLV("flv", FALLBACK_MIME_TYPE),
    ;

    companion object {
        /** MIME type for a file whose extension is not registered. */
        const val DEFAULT_MIME_TYPE = FALLBACK_MIME_TYPE

        private val byExtension = entries.associateBy { it.extension }

        /** The format registered for [extension] (without the dot, any case), or null. */
        fun ofExtension(extension: String): VideoFormat? = byExtension[extension.lowercase(Locale.ROOT)]

        /** The format of the file at [path] by its extension, or null. */
        fun of(path: String): VideoFormat? = ofExtension(path.substringAfterLast('.', ""))

        /** MIME type for the file at [path]; unregistered extensions get [DEFAULT_MIME_TYPE]. */
        fun mimeTypeOf(path: String): String = of(path)?.mimeType ?: DEFAULT_MIME_TYPE
    }
}
