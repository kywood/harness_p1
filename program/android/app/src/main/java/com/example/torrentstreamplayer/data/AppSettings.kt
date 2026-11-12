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
)

/**
 * Versioned binary format of [AppSettings]. To add a field, raise [VERSION], write the field last and keep reading
 * older versions with the new field's default.
 */
internal object AppSettingsCodec {
    const val VERSION = 1

    fun encode(settings: AppSettings): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeInt(VERSION)
            out.writeBoolean(settings.wifiOnly)
            out.writeBoolean(settings.autoSubtitles)
        }
    }.toByteArray()

    /** Saved settings, or defaults for missing, unknown or damaged data. */
    fun decode(bytes: ByteArray?): AppSettings {
        if (bytes == null) return AppSettings()
        return runCatching {
            DataInputStream(ByteArrayInputStream(bytes)).use { input ->
                require(input.readInt() == VERSION)
                AppSettings(wifiOnly = input.readBoolean(), autoSubtitles = input.readBoolean())
            }
        }.getOrDefault(AppSettings())
    }
}
