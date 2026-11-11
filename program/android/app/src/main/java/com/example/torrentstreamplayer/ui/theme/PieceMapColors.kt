package com.example.torrentstreamplayer.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.torrentstreamplayer.engine.PieceCell

/** 조각 맵 색: 완료 `accent`, 받는 중 `primaryLight`(35·65·100%), 없음 `outline` (`design/renewal-plan.md` 2절). */
object PieceMapColors {
    val Verified = BrandColors.Accent
    val Low = BrandColors.PrimaryLight.copy(alpha = 0.35f)
    val Medium = BrandColors.PrimaryLight.copy(alpha = 0.65f)
    val High = BrandColors.PrimaryLight
    val Missing = BrandColors.Outline

    fun of(cell: PieceCell): Color = when (cell) {
        PieceCell.VERIFIED -> Verified
        PieceCell.PARTIAL -> High
        PieceCell.LOW -> Low
        PieceCell.MEDIUM -> Medium
        PieceCell.HIGH -> High
        PieceCell.MISSING -> Missing
    }
}
