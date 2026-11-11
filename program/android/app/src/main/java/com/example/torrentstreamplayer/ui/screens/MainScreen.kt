package com.example.torrentstreamplayer.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.torrentstreamplayer.R

/** 링크 형식 예시. 문장이 아니라 입력 형식이라 번역하지 않는다. */
private const val MAGNET_PLACEHOLDER = "magnet:?xt=urn:btih:..."

/** 메인: 링크를 넣고 바로 재생하거나 재생 목록·저장 파일 관리로 간다. */
@Composable
internal fun MagnetLinkInputScreen(
    onStartStream: (String) -> Unit,
    errorMessage: String?,
    onOpenFiles: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onPrivacyOptions: (() -> Unit)? = null
) {
    var magnetLink by remember { mutableStateOf("") }
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.pieceplay_icon_foreground),
            contentDescription = stringResource(R.string.cd_app_logo),
            modifier = Modifier.padding(bottom = 12.dp).size(96.dp)
                .clip(RoundedCornerShape(20.dp))
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(versionName, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall)

        OutlinedTextField(
            value = magnetLink,
            onValueChange = { magnetLink = it },
            label = { Text(stringResource(R.string.label_link_input)) },
            placeholder = { Text(MAGNET_PLACEHOLDER) },
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onStartStream(magnetLink) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(stringResource(R.string.action_stream_play))
        }
        TextButton(onClick = onOpenHistory, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.action_open_playlist))
        }
        TextButton(onClick = onOpenFiles) { Text(stringResource(R.string.action_open_stored_files)) }
        if (onPrivacyOptions != null) TextButton(onClick = onPrivacyOptions) {
            Text(stringResource(R.string.action_ad_privacy))
        }
    }
}
