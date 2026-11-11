package com.example.torrentstreamplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Shapes as MaterialShapes

/** 간격 토큰 (`design/renewal-plan.md` 4절). */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** 화면 가로 여백. */
    val screenGutter = 16.dp
}

/** 모서리 토큰. 주요 버튼은 [pill]. */
object Shapes {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(16.dp)
    val large = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(percent = 50)

    internal val material = MaterialShapes(small = small, medium = medium, large = large)
}

/** 크기 토큰: 터치 영역, 아이콘, 주요 버튼 높이. */
object Sizes {
    val touchTarget = 48.dp
    val icon = 24.dp
    val iconLarge = 48.dp
    val primaryButtonHeight = 52.dp
    val progressBarHeight = 8.dp
    val logo = 96.dp
}
