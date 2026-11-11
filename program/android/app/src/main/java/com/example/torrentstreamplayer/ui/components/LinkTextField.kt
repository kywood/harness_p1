package com.example.torrentstreamplayer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.theme.Shapes

/** 링크 형식 예시. 문장이 아니라 입력 형식이라 번역하지 않는다. */
private const val MAGNET_PLACEHOLDER = "magnet:?xt=urn:btih:..."

/** 링크 입력창 최대 줄 수. 긴 마그넷도 카드가 지나치게 커지지 않게 한다. */
private const val LINK_MAX_LINES = 3

/** 마그넷·.torrent 링크 입력창. 메인 입력 카드와 링크 추가 대화상자가 같이 쓴다. */
@Composable
fun LinkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = stringResource(R.string.label_link_input)
) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        placeholder = { Text(MAGNET_PLACEHOLDER) },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { message -> { Text(message) } },
        maxLines = LINK_MAX_LINES,
        shape = Shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainer,
            unfocusedContainerColor = colors.surfaceContainer,
            errorContainerColor = colors.surfaceContainer,
            disabledContainerColor = colors.surfaceContainer,
            unfocusedBorderColor = colors.outline,
            focusedBorderColor = colors.tertiary,
            focusedLabelColor = colors.tertiary,
            cursorColor = colors.tertiary
        ),
        modifier = modifier.fillMaxWidth()
    )
}
