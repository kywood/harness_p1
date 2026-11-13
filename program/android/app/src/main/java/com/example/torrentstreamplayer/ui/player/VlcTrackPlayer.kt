package com.example.torrentstreamplayer.ui.player

import android.net.Uri
import com.example.torrentstreamplayer.ui.PlayerTrack
import com.example.torrentstreamplayer.ui.PlayerTracks
import com.example.torrentstreamplayer.ui.TrackPlayer
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import java.io.File

/**
 * LibVLC adapter of [TrackPlayer]. VLC lists tracks as `{id, name}` (with a "Disable" entry of id -1, left out
 * here); the language code and title come from the media's own track list when VLC has it.
 *
 * Calls take VLC's player lock, so the screen runs them on the engine command queue, before the player is released.
 */
internal class VlcTrackPlayer(private val player: MediaPlayer) : TrackPlayer {
    override fun tracks(): PlayerTracks {
        val spu = player.spuTracks
        val audio = player.audioTracks
        // The media caches its track list on first read, so it is read only once VLC has listed some tracks.
        val details = if (spu.isNullOrEmpty() && audio.isNullOrEmpty()) emptyList() else mediaTracks()
        fun read(list: Array<MediaPlayer.TrackDescription>?, type: Int) = list.orEmpty()
            .filter { it.id >= 0 }
            .map { track ->
                val detail = details.firstOrNull { it.id == track.id && it.type == type }
                PlayerTrack(track.id, track.name, detail?.language, detail?.description)
            }
        return PlayerTracks(
            subtitles = read(spu, IMedia.Track.Type.Text),
            audio = read(audio, IMedia.Track.Type.Audio),
            subtitleId = player.spuTrack,
            audioId = player.audioTrack,
        )
    }

    override fun selectSubtitle(id: Int): Boolean = player.setSpuTrack(id)

    override fun selectAudio(id: Int): Boolean = player.setAudioTrack(id)

    override fun addSubtitle(file: File): Boolean =
        player.addSlave(IMedia.Slave.Type.Subtitle, Uri.fromFile(file), false)

    /** The media's tracks; [MediaPlayer.getMedia] retains the media, so it is released here. */
    private fun mediaTracks(): List<IMedia.Track> {
        val media = player.media ?: return emptyList()
        return try {
            (0 until media.trackCount).mapNotNull { media.getTrack(it) }
        } finally {
            media.release()
        }
    }
}
