package com.example.torrentstreamplayer

import androidx.compose.ui.graphics.Color
import com.example.torrentstreamplayer.engine.PieceCell
import com.example.torrentstreamplayer.ui.theme.BrandColors
import com.example.torrentstreamplayer.ui.theme.PieceMapColors
import com.example.torrentstreamplayer.ui.theme.PiecePlayColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.pow

class ThemeTokensTest {
    /** WCAG 2.1 상대 휘도(renewal-plan 2절의 대비 계산과 같은 식). */
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double =
            if (value <= 0.03928f) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    @Test fun textTokenPairsMeetWcagAa() {
        val pairs = mapOf(
            "onSurface/background" to (BrandColors.OnSurface to BrandColors.Background),
            "onSurface/surface" to (BrandColors.OnSurface to BrandColors.Surface),
            "onSurface/surfaceContainer" to (BrandColors.OnSurface to BrandColors.SurfaceContainer),
            "onSurfaceVariant/surface" to (BrandColors.OnSurfaceVariant to BrandColors.Surface),
            "onSurfaceVariant/surfaceContainer" to (BrandColors.OnSurfaceVariant to BrandColors.SurfaceContainer),
            "onPrimary/primary" to (BrandColors.OnPrimary to BrandColors.Primary),
            "primaryLight/surface" to (BrandColors.PrimaryLight to BrandColors.Surface),
            "accent/background" to (BrandColors.Accent to BrandColors.Background),
            "onAccent/accent" to (BrandColors.OnAccent to BrandColors.Accent),
            "error/surface" to (BrandColors.Error to BrandColors.Surface),
        )
        pairs.forEach { (name, pair) ->
            val ratio = contrast(pair.first, pair.second)
            assertTrue("$name contrast $ratio must be at least 4.5:1", ratio >= 4.5)
        }
    }

    @Test fun documentedContrastValuesMatchTheTokens() {
        assertEquals(16.5, contrast(BrandColors.OnSurface, BrandColors.Background), 0.1)
        assertEquals(4.9, contrast(BrandColors.OnPrimary, BrandColors.Primary), 0.1)
        assertEquals(11.9, contrast(BrandColors.Accent, BrandColors.Background), 0.1)
    }

    @Test fun colorSchemeIsBuiltFromBrandTokens() {
        assertEquals(BrandColors.Background, PiecePlayColorScheme.background)
        assertEquals(BrandColors.Surface, PiecePlayColorScheme.surface)
        assertEquals(BrandColors.SurfaceContainer, PiecePlayColorScheme.surfaceContainer)
        assertEquals(BrandColors.Outline, PiecePlayColorScheme.outline)
        assertEquals(BrandColors.OnSurface, PiecePlayColorScheme.onSurface)
        assertEquals(BrandColors.OnSurfaceVariant, PiecePlayColorScheme.onSurfaceVariant)
        assertEquals(BrandColors.Primary, PiecePlayColorScheme.primary)
        assertEquals(BrandColors.OnPrimary, PiecePlayColorScheme.onPrimary)
        assertEquals(BrandColors.Accent, PiecePlayColorScheme.secondary)
        assertEquals(BrandColors.OnAccent, PiecePlayColorScheme.onSecondary)
        assertEquals(BrandColors.Error, PiecePlayColorScheme.error)
    }

    @Test fun themeNeverUsesWallpaperDynamicColor() {
        val theme = File("src/main/java/com/example/torrentstreamplayer/ui/theme/Theme.kt").readText()
        assertFalse("dynamic color must stay off", Regex("""dynamic(Dark|Light)ColorScheme|dynamicColor""").containsMatchIn(theme))
        assertFalse("only the dark scheme is provided", theme.contains("lightColorScheme"))
    }

    @Test fun screensAndComponentsUseTokensInsteadOfColorLiterals() {
        val literal = Regex("""Color\(0x""")
        val offenders = listOf("ui/screens", "ui/components")
            .flatMap { File("src/main/java/com/example/torrentstreamplayer/$it").walk().filter(File::isFile).toList() }
            .filter { literal.containsMatchIn(it.readText()) }
            .map { it.name }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test fun pieceMapColorsFollowTheTokenRule() {
        assertEquals(BrandColors.Accent, PieceMapColors.of(PieceCell.VERIFIED))
        assertEquals(BrandColors.Outline, PieceMapColors.of(PieceCell.MISSING))
        assertEquals(0.35f, PieceMapColors.of(PieceCell.LOW).alpha, 0.01f)
        assertEquals(0.65f, PieceMapColors.of(PieceCell.MEDIUM).alpha, 0.01f)
        assertEquals(BrandColors.PrimaryLight, PieceMapColors.of(PieceCell.HIGH))
        assertTrue("receiving pieces must stand out from the empty track",
            contrast(PieceMapColors.High, PieceMapColors.Missing) >= 3.0)
    }
}
