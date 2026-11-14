package com.example.torrentstreamplayer.ui.player

import com.example.torrentstreamplayer.engine.PieceCell

/**
 * Which cells of the piece map the seek bar paints as downloaded.
 *
 * One cell stands for several pieces. Only a cell whose pieces are all verified is painted: a partly
 * downloaded cell may still miss the piece playback needs there, so it stays in the track color.
 */
internal object SeekTrackPlan {
    fun downloaded(cell: PieceCell): Boolean = cell == PieceCell.VERIFIED
}
