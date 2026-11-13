package com.example.torrentstreamplayer.ui

import androidx.annotation.StringRes
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.engine.SubtitleLanguage
import java.io.File

/**
 * Embedded tracks the player reports. Ids are VLC track ids; [NO_TRACK] means none is selected (subtitles off).
 * [videoCount] counts video tracks only, so a silent video without subtitles still shows the player has opened it.
 */
internal data class PlayerTracks(
    val subtitles: List<PlayerTrack> = emptyList(),
    val audio: List<PlayerTrack> = emptyList(),
    val subtitleId: Int = NO_TRACK,
    val audioId: Int = NO_TRACK,
    val videoCount: Int = 0,
) {
    /** True once the player has listed any of the video's own tracks. */
    val listed: Boolean get() = videoCount > 0 || subtitles.isNotEmpty() || audio.isNotEmpty()

    companion object {
        const val NO_TRACK = -1
    }
}

/** Where an external subtitle file stands for the player. */
internal enum class ExternalSubtitleState(val tag: OptionTag) {
    /** The engine is still downloading it. */
    DOWNLOADING(OptionTag.DOWNLOADING),

    /** Handed to the player; selectable once the player lists its track. */
    ADDED(OptionTag.FILE),

    /** The player could not open it. */
    FAILED(OptionTag.FAILED),
}

/**
 * A subtitle file the engine chose for the video.
 *
 * @param subtitle the verified file, or null while it is still downloading.
 * @param trackId the player's track for the file once it lists one.
 */
internal data class ExternalSubtitle(
    val file: File,
    val subtitle: SubtitleFile?,
    val state: ExternalSubtitleState,
    val trackId: Int? = null,
) {
    val language: TrackLanguage?
        get() = (subtitle?.language ?: SubtitleLanguage.of(file.name))?.let(TrackLanguage::of)

    val name: TrackName.File get() = TrackName.File(file.name, language)
}

/** Tag at the end of a row (`design/specs/subtitles.md` 2절). */
internal enum class OptionTag(@StringRes val label: Int) {
    FILE(R.string.tag_subtitle_file),
    DOWNLOADING(R.string.tag_downloading),
    FAILED(R.string.tag_download_failed),
}

/** One row of the subtitle sheet. [id] is the player track to select. */
internal data class TrackOption(
    val id: Int,
    val name: TrackName,
    val tag: OptionTag? = null,
    val enabled: Boolean = true,
)

/**
 * What the subtitle sheet shows.
 *
 * @param loading true until the player has reported its tracks ("트랙을 확인하는 중…").
 * @param subtitles embedded tracks, then external files by file name. Empty shows "자막 없음"; otherwise "끄기" goes
 * first.
 * @param audio audio tracks, only when there are two or more.
 * @param selectedName the subtitle being shown, or null when subtitles are off.
 */
internal data class SubtitleSheetState(
    val loading: Boolean = true,
    val subtitles: List<TrackOption> = emptyList(),
    val audio: List<TrackOption> = emptyList(),
    val selectedSubtitle: Int = PlayerTracks.NO_TRACK,
    val selectedAudio: Int = PlayerTracks.NO_TRACK,
    val selectedName: TrackName? = null,
)

/** Builds [SubtitleSheetState] from the player's tracks and the engine's subtitle files. Pure. */
internal object SubtitleMenuPlan {
    fun build(tracks: PlayerTracks?, externals: List<ExternalSubtitle>): SubtitleSheetState {
        if (tracks == null) return SubtitleSheetState(loading = true)
        val externalIds = externals.mapNotNull { it.trackId }.toSet()
        val embedded = tracks.subtitles.filter { it.id !in externalIds }
        val embeddedOptions = embedded.zip(SubtitleTrackLabel.names(embedded)) { track, name -> TrackOption(track.id, name) }
        val externalOptions = externals.sortedBy { it.file.name }.map { external ->
            TrackOption(external.trackId ?: PlayerTracks.NO_TRACK, external.name, external.state.tag,
                enabled = external.state == ExternalSubtitleState.ADDED && external.trackId != null)
        }
        val subtitles = embeddedOptions + externalOptions
        val audio = tracks.audio.zip(SubtitleTrackLabel.names(tracks.audio)) { track, name -> TrackOption(track.id, name) }
        val selected = subtitles.firstOrNull { it.enabled && it.id == tracks.subtitleId }
        return SubtitleSheetState(
            loading = false,
            subtitles = subtitles,
            audio = audio.takeIf { it.size > 1 }.orEmpty(),
            selectedSubtitle = selected?.id ?: PlayerTracks.NO_TRACK,
            selectedAudio = tracks.audioId,
            selectedName = selected?.name,
        )
    }
}
