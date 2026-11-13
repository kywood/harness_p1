package com.example.torrentstreamplayer.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/** User settings. Defaults apply on first launch and whenever the saved file cannot be read. */
internal data class AppSettings(
    /** Download only on Wi-Fi (unmetered networks); mobile data waits unless allowed once. */
    val wifiOnly: Boolean = false,
    /** Turn on a downloaded subtitle file automatically when playback starts. */
    val autoSubtitles: Boolean = true,
    /**
     * Language of the subtitle the viewer picked last (`TrackLanguage.keyOf`, e.g. `ko`), or null before any pick.
     * The automatic pick prefers it (PM decision, PR #131: one preferred language, not one per video).
     */
    val subtitleLanguage: String? = null,
)

/**
 * Versioned binary format of [AppSettings]. To add a field, raise [VERSION], write the field last and keep reading
 * older versions with the new field's default.
 *
 * - 1: wifiOnly, autoSubtitles
 * - 2: + subtitleLanguage (UTF, "" for none)
 */
internal object AppSettingsCodec {
    const val VERSION = 2

    /** Longest saved language key; longer values are dropped as damaged. */
    private const val MAX_LANGUAGE_CHARS = 8

    fun encode(settings: AppSettings): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeInt(VERSION)
            out.writeBoolean(settings.wifiOnly)
            out.writeBoolean(settings.autoSubtitles)
            out.writeUTF(settings.subtitleLanguage.orEmpty())
        }
    }.toByteArray()

    /** Saved settings, or defaults for missing, unknown or damaged data. */
    fun decode(bytes: ByteArray?): AppSettings {
        if (bytes == null) return AppSettings()
        return runCatching {
            DataInputStream(ByteArrayInputStream(bytes)).use { input ->
                val version = input.readInt()
                require(version in 1..VERSION)
                val wifiOnly = input.readBoolean()
                val autoSubtitles = input.readBoolean()
                val language = if (version >= 2) input.readUTF() else ""
                val saved = language.takeIf { it.isNotEmpty() && it.length <= MAX_LANGUAGE_CHARS }
                AppSettings(wifiOnly, autoSubtitles, saved)
            }
        }.getOrDefault(AppSettings())
    }
}
