package com.example.torrentstreamplayer.ui.theme

import androidx.compose.ui.graphics.Brush

/** 재생 화면 오버레이 색. 모두 [BrandColors]에서 만든다(`design/renewal-plan.md` 2절). */
object PlaybackColors {
    /** 재생 막대 손잡이·재생한 구간 단색, 강조 글자. */
    val Accent = BrandColors.PrimaryLight

    /** 재생한 구간(그라데이션). */
    val Played: Brush = BrandColors.brandGradient

    /** 받은 구간 표시. */
    val Downloaded = BrandColors.PrimaryLight.copy(alpha = 0.4f)

    /** 받은 양·속도 같은 보조 글자. */
    val Download = BrandColors.OnSurfaceVariant
    val Track = BrandColors.Outline
    val Panel = BrandColors.Surface.copy(alpha = 0.94f)
    val SeekPanel = BrandColors.Surface.copy(alpha = 0.45f)
    val ControlsScrim = BrandColors.Background.copy(alpha = 0.15f)
    val PiecePartial = BrandColors.PrimaryLight
}
