package com.example.torrentstreamplayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.torrentstreamplayer.ui.theme.BrandColors
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme

/** 비활성 글자 버튼의 투명도(Material 기본값과 같다). */
private const val DISABLED_ALPHA = 0.38f

/** 글자 버튼의 종류. 종류마다 글자 색을 등록한다. */
enum class TextActionTone(val color: Color) {
    /** 어두운 표면 위 일반 동작: `primaryLight`(표면 대비 6.4). */
    DEFAULT(BrandColors.PrimaryLight),

    /** 삭제처럼 되돌릴 수 없는 동작. */
    DESTRUCTIVE(BrandColors.Error),

    /** 덜 중요한 동작. */
    MUTED(BrandColors.OnSurfaceVariant),
}

/** 어두운 표면 위 글자 버튼. 기본 `TextButton`의 `primary`(#2F66F0)는 표면 대비가 낮아 `primaryLight`를 쓴다. */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tone: TextActionTone = TextActionTone.DEFAULT
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = tone.color,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
        ),
        modifier = modifier.heightIn(min = Sizes.touchTarget)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(Sizes.iconSmall))
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Preview
@Composable
private fun TextActionPreview() {
    TorrentStreamPlayerTheme {
        Row {
            TextAction("링크 추가", {})
            TextAction("삭제", {}, tone = TextActionTone.DESTRUCTIVE)
            TextAction("다운로드", {}, enabled = false)
        }
    }
}
