package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.SubtitleFile
import java.io.File

/**
 * Port to the video player's subtitle and audio tracks. The app uses the LibVLC adapter
 * (`ui/player/VlcTrackPlayer`); tests pass a fake. Calls may wait on the player, so they run off the main thread.
 */
internal interface TrackPlayer {
    fun tracks(): PlayerTracks

    /** Shows track [id], or hides subtitles for [PlayerTracks.NO_TRACK]. False when the player refused. */
    fun selectSubtitle(id: Int): Boolean
    fun selectAudio(id: Int): Boolean

    /** Opens [file] as an extra subtitle track without showing it. False when the player refused. */
    fun addSubtitle(file: File): Boolean
}

/** What a [SubtitleSession] step leaves for the screen. [autoPicked] is set by the one automatic pick only. */
internal data class SubtitleSnapshot(val sheet: SubtitleSheetState, val autoPicked: TrackName? = null)

/**
 * Subtitle and audio tracks of one playback (`design/specs/subtitles.md`): hands the engine's subtitle files to the
 * player, keeps the sheet state, and makes the one automatic pick ([SubtitleAutoPolicy]) when [autoEnabled].
 * With [autoEnabled] off, subtitles stay off until the viewer picks one.
 *
 * Not thread-safe: the screen calls it from one thread (the engine command queue), like the player calls.
 *
 * The player gives an added file's track no name to recognise it by, so a subtitle track that appears after a file
 * was added is taken as that file's track (in the order the files were added). Files are added only after the player
 * has listed the video's own tracks ([start] and a non-empty track list), so embedded tracks are not mistaken.
 */
internal class SubtitleSession(
    private val player: TrackPlayer,
    private val autoEnabled: Boolean,
    private val preferredKey: String?,
) {
    private var tracks: PlayerTracks? = null
    private val knownIds = mutableSetOf<Int>()
    private val awaitingTrack = ArrayDeque<File>()
    private val added = linkedMapOf<File, ExternalSubtitle>()
    private var ready: List<SubtitleFile> = emptyList()
    private var pending: List<File> = emptyList()
    private var started = false
    private var settled = false

    /** The player started playing: reads its tracks and adds the subtitle files that are ready. */
    fun start(): SubtitleSnapshot {
        started = true
        return refresh()
    }

    /** The engine's verified ([ready]) and still downloading ([pending]) subtitle files changed. */
    fun updateFiles(ready: List<SubtitleFile>, pending: List<File>): SubtitleSnapshot {
        this.ready = ready
        this.pending = pending
        return refresh()
    }

    /** Reads the tracks again (the player added, removed or switched one). */
    fun refresh(): SubtitleSnapshot {
        if (!started) return SubtitleSnapshot(SubtitleMenuPlan.build(null, externals()))
        var current = player.tracks()
        if (tracks == null) knownIds += current.subtitles.map { it.id }
        val newIds = current.subtitles.map { it.id }.filter { it !in knownIds }
        knownIds += newIds
        for (id in newIds) {
            val file = awaitingTrack.removeFirstOrNull() ?: break
            added[file] = added.getValue(file).copy(trackId = id)
        }
        if (current.subtitles.isNotEmpty() || current.audio.isNotEmpty()) addReadyFiles()
        var picked: Int? = null
        if (!settled && !autoEnabled && current.subtitleId != PlayerTracks.NO_TRACK &&
            player.selectSubtitle(PlayerTracks.NO_TRACK)) {
            current = current.copy(subtitleId = PlayerTracks.NO_TRACK)
        }
        if (!settled && autoEnabled) {
            val choice = SubtitleAutoPolicy.choose(preferredKey, embedded(current), externals())
            if (choice != null && (choice == current.subtitleId || player.selectSubtitle(choice))) {
                settled = true
                picked = choice
                current = current.copy(subtitleId = choice)
            }
        }
        tracks = current
        val sheet = SubtitleMenuPlan.build(current, externals())
        return SubtitleSnapshot(sheet, picked?.let { id -> sheet.subtitles.firstOrNull { it.id == id }?.name })
    }

    /** The viewer picked subtitle [id] (or [PlayerTracks.NO_TRACK]). No automatic pick follows. */
    fun selectSubtitle(id: Int): Boolean {
        settled = true
        return player.selectSubtitle(id)
    }

    fun selectAudio(id: Int): Boolean = player.selectAudio(id)

    /** Preference key of subtitle [id]'s language, or null when it has none (or is "끄기"). */
    fun languageKeyOf(id: Int): String? {
        added.values.firstOrNull { it.trackId == id }?.let { return it.language?.key }
        return tracks?.subtitles?.firstOrNull { it.id == id }?.let(SubtitleTrackLabel::languageKey)
    }

    private fun addReadyFiles() {
        ready.filter { it.file !in added }.forEach { subtitle ->
            val opened = player.addSubtitle(subtitle.file)
            val state = if (opened) ExternalSubtitleState.ADDED else ExternalSubtitleState.FAILED
            added[subtitle.file] = ExternalSubtitle(subtitle.file, subtitle, state)
            if (opened) awaitingTrack.addLast(subtitle.file)
        }
    }

    private fun externals(): List<ExternalSubtitle> {
        val waiting = ready.filter { it.file !in added }.map { ExternalSubtitle(it.file, it, ExternalSubtitleState.ADDED) }
        val downloading = pending.filter { it !in added }
            .map { ExternalSubtitle(it, null, ExternalSubtitleState.DOWNLOADING) }
        return added.values.toList() + waiting + downloading
    }

    private fun embedded(current: PlayerTracks): List<PlayerTrack> {
        val externalIds = added.values.mapNotNull { it.trackId }.toSet()
        return current.subtitles.filter { it.id !in externalIds }
    }
}
