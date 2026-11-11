package com.example.torrentstreamplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

/** 숫자 열이 흔들리지 않게 고정폭 숫자를 쓴다. */
private const val TABULAR_NUMBERS = "tnum"

private val Base = Typography()

/** Material 3 기본 타입 스케일에 굵기만 조정한다(`design/renewal-plan.md` 3절). 글꼴은 시스템 글꼴. */
val Typography = Base.copy(
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.Medium),
    labelMedium = Base.labelMedium.copy(fontFeatureSettings = TABULAR_NUMBERS)
)
