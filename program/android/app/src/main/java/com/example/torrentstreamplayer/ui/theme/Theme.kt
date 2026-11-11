package com.example.torrentstreamplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** 다크 테마 하나. 기기 배경화면 색(dynamic color)은 쓰지 않는다. */
internal val PiecePlayColorScheme = darkColorScheme(
    primary = BrandColors.Primary,
    onPrimary = BrandColors.OnPrimary,
    // 선택된 칩·토글처럼 Material이 컨테이너 색을 쓰는 곳도 브랜드 색을 따른다(기본값은 보라 계열).
    primaryContainer = BrandColors.Primary,
    onPrimaryContainer = BrandColors.OnPrimary,
    secondaryContainer = BrandColors.Primary,
    onSecondaryContainer = BrandColors.OnPrimary,
    tertiaryContainer = BrandColors.SurfaceContainer,
    onTertiaryContainer = BrandColors.OnSurface,
    inverseSurface = BrandColors.OnSurface,
    inverseOnSurface = BrandColors.Background,
    inversePrimary = BrandColors.Primary,
    surfaceTint = BrandColors.Surface,
    scrim = BrandColors.Background,
    secondary = BrandColors.Accent,
    onSecondary = BrandColors.OnAccent,
    tertiary = BrandColors.PrimaryLight,
    onTertiary = BrandColors.Background,
    background = BrandColors.Background,
    onBackground = BrandColors.OnSurface,
    surface = BrandColors.Surface,
    onSurface = BrandColors.OnSurface,
    surfaceVariant = BrandColors.SurfaceContainer,
    onSurfaceVariant = BrandColors.OnSurfaceVariant,
    surfaceContainerLowest = BrandColors.Background,
    surfaceContainerLow = BrandColors.Surface,
    surfaceContainer = BrandColors.SurfaceContainer,
    surfaceContainerHigh = BrandColors.SurfaceContainer,
    surfaceContainerHighest = BrandColors.SurfaceContainer,
    outline = BrandColors.Outline,
    outlineVariant = BrandColors.Outline,
    error = BrandColors.Error,
    onError = BrandColors.Background
)

@Composable
fun TorrentStreamPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PiecePlayColorScheme,
        typography = Typography,
        shapes = Shapes.material,
        content = content
    )
}
