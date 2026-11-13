package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.meta.PlayerSpec
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

    /** Asks the player to open [file] as an extra subtitle track without showing it. False when it refused. */
    fun addSubtitle(file: File): Boolean
}

/**
 * What a [SubtitleSession] step leaves for the screen. [autoPicked] is set when an automatic pick turns a subtitle
 * on. [recheckAfterMs] asks for another [SubtitleSession.refresh] after that long, while an added file waits for
 * its track.
 */
internal data class SubtitleSnapshot(
    val sheet: SubtitleSheetState,
    val autoPicked: TrackName? = null,
    val recheckAfterMs: Long? = null,
)

/**
 * Subtitle and audio tracks of one playback (`design/specs/subtitles.md`): hands the engine's subtitle files to the
 * player, keeps the sheet state, and makes the automatic pick ([SubtitleAutoPolicy]) when [autoEnabled].
 * With [autoEnabled] off, subtitles stay off until the viewer picks one.
 *
 * Not thread-safe: the screen calls it from one thread (the engine command queue), like the player calls.
 *
 * The player names an added file's track nothing recognisable, so files are added one at a time: after adding one,
 * the next new subtitle track is that file's. A file whose track does not appear within
 * [PlayerSpec.SUBTITLE_TRACK_TIMEOUT_MS] of [now] is marked failed and kept as a late file, and the next file is
 * added. A new track no file explains goes to the oldest late file first (its "받지 못함" is lifted), then to the
 * waiting file. Files are added only after the player has listed the video's own tracks ([PlayerTracks.listed]).
 *
 * @param prepare the file to hand to the player for a subtitle file (a UTF-8 copy of a CP949 file, `SubtitleCopies`).
 * @param now clock in milliseconds that runs only while the video plays and is not buffering (`PlaybackClock`):
 * VLC opens an added file between reads, so a stalled stream delays it. Tests pass a fake one.
 */
internal class SubtitleSession(
    private val player: TrackPlayer,
    private val autoEnabled: Boolean,
    private val preference: SubtitlePreference,
    private val now: () -> Long = System::currentTimeMillis,
    private val prepare: (File) -> File = { it },
) {
    private var tracks: PlayerTracks? = null
    private val knownIds = mutableSetOf<Int>()
    private var awaiting: Pair<File, Long>? = null
    private val late = ArrayDeque<File>()
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

    /** Reads the tracks again (the player added, removed or switched one, or a recheck is due). */
    fun refresh(): SubtitleSnapshot {
        if (!started) return SubtitleSnapshot(SubtitleMenuPlan.build(null, externals()))
        var current = player.tracks()
        if (tracks == null) knownIds += current.subtitles.map { it.id }
        val newIds = current.subtitles.map { it.id }.filter { it !in knownIds }
        knownIds += newIds
        matchNewTracks(newIds)
        if (current.listed) addNextFile()
        if (!settled && !autoEnabled && current.subtitleId != PlayerTracks.NO_TRACK &&
            player.selectSubtitle(PlayerTracks.NO_TRACK)) {
            current = current.copy(subtitleId = PlayerTracks.NO_TRACK)
        }
        var picked: Int? = null
        val choice = autoChoice(current)
        if (choice != null && (choice.trackId == current.subtitleId || player.selectSubtitle(choice.trackId))) {
            settled = choice.final
            if (choice.trackId != current.subtitleId) picked = choice.trackId
            current = current.copy(subtitleId = choice.trackId)
        }
        tracks = current
        val sheet = SubtitleMenuPlan.build(current.takeIf { it.listed }, externals())
        val pickedName = picked?.let { id -> sheet.subtitles.firstOrNull { it.id == id }?.name }
        return SubtitleSnapshot(sheet, pickedName, recheckAfterMs())
    }

    private fun autoChoice(current: PlayerTracks): AutoSubtitleChoice? =
        if (settled || !autoEnabled) null else SubtitleAutoPolicy.choose(preference, embedded(current), externals())

    private fun recheckAfterMs(): Long? =
        awaiting?.let { (_, since) ->
            (since + PlayerSpec.SUBTITLE_TRACK_TIMEOUT_MS - now()).coerceAtLeast(PlayerSpec.SUBTITLE_RECHECK_MIN_MS)
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

    /** Gives new tracks to late files first, then to the waiting file; a waiting file past its time becomes late. */
    private fun matchNewTracks(newIds: List<Int>) {
        for (id in newIds) {
            val file = late.removeFirstOrNull() ?: awaiting?.first?.also { awaiting = null } ?: break
            added[file] = added.getValue(file).copy(state = ExternalSubtitleState.ADDED, trackId = id)
        }
        val (file, since) = awaiting ?: return
        if (now() - since < PlayerSpec.SUBTITLE_TRACK_TIMEOUT_MS) return
        added[file] = added.getValue(file).copy(state = ExternalSubtitleState.FAILED)
        late.addLast(file)
        awaiting = null
    }

    /** Adds the next ready file when none is waiting for its track. Files the player refuses are skipped. */
    private fun addNextFile() {
        while (awaiting == null) {
            val subtitle = ready.firstOrNull { it.file !in added } ?: return
            val opened = player.addSubtitle(prepare(subtitle.file))
            val state = if (opened) ExternalSubtitleState.ADDED else ExternalSubtitleState.FAILED
            added[subtitle.file] = ExternalSubtitle(subtitle.file, subtitle, state)
            if (opened) awaiting = subtitle.file to now()
        }
    }

    private fun externals(): List<ExternalSubtitle> {
        val waiting = ready.filter { it.file !in added }
            .map { ExternalSubtitle(it.file, it, ExternalSubtitleState.ADDED) }
        val downloading = pending.filter { it !in added }
            .map { ExternalSubtitle(it, null, ExternalSubtitleState.DOWNLOADING) }
        return added.values.toList() + waiting + downloading
    }

    private fun embedded(current: PlayerTracks): List<PlayerTrack> {
        val externalIds = added.values.mapNotNull { it.trackId }.toSet()
        return current.subtitles.filter { it.id !in externalIds }
    }
}
