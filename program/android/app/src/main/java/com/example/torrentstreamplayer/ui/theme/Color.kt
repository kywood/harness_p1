package com.example.torrentstreamplayer.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** 브랜드 색 토큰. 값은 `design/renewal-plan.md` 2절 표와 같다. 화면 파일은 이 값이나 `MaterialTheme.colorScheme`만 쓴다. */
object BrandColors {
    val Background = Color(0xFF0A0F1E)
    val Surface = Color(0xFF111A2E)
    val SurfaceContainer = Color(0xFF18233D)
    val Outline = Color(0xFF2A3757)
    val OnSurface = Color(0xFFE8EEFF)
    val OnSurfaceVariant = Color(0xFFA7B3D1)
    val Primary = Color(0xFF2F66F0)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryLight = Color(0xFF6E9BFF)
    val Accent = Color(0xFF2EE6B0)
    val OnAccent = Color(0xFF00281C)
    val Error = Color(0xFFFF6B7A)

    /** 주요 버튼, 진행 막대의 받은 부분, 완료 조각에만 쓴다. */
    val brandGradient: Brush = Brush.horizontalGradient(listOf(Primary, Accent))
}
