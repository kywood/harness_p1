package com.example.torrentstreamplayer.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** 위 가림막이 끝나는 투명도(아래로 갈수록 투명). */
private const val SCRIM_ALPHA = 0.85f

/** 재생 화면 오버레이 색. 영상 위 흰색과 영상 바탕 검정을 빼면 모두 [BrandColors]에서 만든다(`design/renewal-plan.md` 2절). */
object PlaybackColors {
    /** 영상 바탕(레터박스). 영상은 순수 검정 위에 둔다. */
    val Video = Color.Black

    /** 영상 위 아이콘·글자. 가림막 위에서 읽히게 흰색을 쓴다. */
    val OnVideo = BrandColors.OnPrimary

    /** 영상 위 보조 글자(받은 양 등). */
    val OnVideoMuted = BrandColors.OnSurfaceVariant

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

    /** 가운데 재생 버튼들의 원형 바탕. */
    val ControlButton = BrandColors.OnPrimary.copy(alpha = 0.16f)

    /** 속도·피어 칩 바탕. */
    val Chip = BrandColors.Surface.copy(alpha = 0.7f)

    /** 오류 아이콘. */
    val Error = BrandColors.Error

    /** 상단 가림막: 위는 짙고 아래로 투명해진다. */
    val ScrimTop: Brush = Brush.verticalGradient(listOf(BrandColors.Background.copy(alpha = SCRIM_ALPHA), Color.Transparent))

    /** 하단 가림막: 아래로 갈수록 짙어진다. */
    val ScrimBottom: Brush = Brush.verticalGradient(listOf(Color.Transparent, BrandColors.Background.copy(alpha = SCRIM_ALPHA)))
}
