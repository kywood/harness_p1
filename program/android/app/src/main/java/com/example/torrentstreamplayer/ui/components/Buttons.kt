package com.example.torrentstreamplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.torrentstreamplayer.ui.theme.BrandColors
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme

/** 비활성 버튼의 투명도. */
private const val DISABLED_ALPHA = 0.4f

/** 그라데이션 가운데(밝은 청록) 위 흰 글자가 묻히지 않게 배경색 그림자를 깐다. */
private val LABEL_SHADOW = Shadow(color = BrandColors.Background.copy(alpha = 0.6f), blurRadius = 8f)

/** 주요 동작 버튼: 브랜드 그라데이션, 완전한 둥근 모서리, 높이 52dp. 화면마다 하나만 둔다. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = Shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(),
        modifier = modifier.height(Sizes.primaryButtonHeight)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .background(BrandColors.brandGradient, Shapes.pill)
                .padding(horizontal = Spacing.xl),
            contentAlignment = Alignment.Center
        ) {
            val style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, shadow = LABEL_SHADOW)
            ButtonLabel(text, icon, style)
        }
    }
}

/** 보조 동작 버튼: `surfaceContainer` 배경. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = Shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        modifier = modifier.heightIn(min = Sizes.touchTarget)
    ) {
        ButtonLabel(text, icon, MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ButtonLabel(text: String, icon: ImageVector?, style: TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(Sizes.icon))
        Text(text, style = style, maxLines = 1)
    }
}

@Preview
@Composable
private fun ButtonsPreview() {
    TorrentStreamPlayerTheme {
        Column(
            Modifier.background(MaterialTheme.colorScheme.background).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            PrimaryButton("바로 재생", {}, Modifier.fillMaxWidth())
            PrimaryButton("바로 재생", {}, Modifier.fillMaxWidth(), enabled = false)
            SecondaryButton("재생 목록", {}, Modifier.fillMaxWidth())
        }
    }
}
