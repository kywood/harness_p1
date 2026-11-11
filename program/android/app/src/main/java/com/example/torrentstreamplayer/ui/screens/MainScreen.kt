package com.example.torrentstreamplayer.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.ui.components.LinkTextField
import com.example.torrentstreamplayer.ui.components.PrimaryButton
import com.example.torrentstreamplayer.ui.components.SecondaryButton
import com.example.torrentstreamplayer.ui.components.SectionCard
import com.example.torrentstreamplayer.ui.theme.Shapes
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import kotlinx.coroutines.launch

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
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenGutter, vertical = Spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.widthIn(max = Sizes.contentMaxWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandHeader()
            SectionCard {
                LinkTextField(magnetLink, { magnetLink = it }, errorMessage)
                SecondaryButton(
                    text = stringResource(R.string.action_paste),
                    icon = ImageVector.vectorResource(R.drawable.ic_content_paste),
                    onClick = {
                        scope.launch {
                            val clip = clipboard.getClipEntry()?.clipData
                            val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)
                            if (!text.isNullOrBlank()) magnetLink = text.toString().trim()
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                )
            }
            PrimaryButton(
                text = stringResource(R.string.action_stream_play),
                onClick = { onStartStream(magnetLink) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                ShortcutCard(Icons.AutoMirrored.Filled.List, stringResource(R.string.action_open_playlist),
                    stringResource(R.string.main_playlist_hint), onOpenHistory, Modifier.weight(1f))
                ShortcutCard(ImageVector.vectorResource(R.drawable.ic_folder),
                    stringResource(R.string.action_open_stored_files), stringResource(R.string.main_files_hint),
                    onOpenFiles, Modifier.weight(1f))
            }
            Footer(versionName, onPrivacyOptions)
        }
    }
}

@Composable
private fun BrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Image(
            painter = painterResource(R.drawable.pieceplay_icon_foreground),
            contentDescription = stringResource(R.string.cd_app_logo),
            modifier = Modifier.size(Sizes.logo).clip(Shapes.large)
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.main_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** 메인 아래 바로가기 카드(재생 목록·저장 파일 관리). */
@Composable
private fun ShortcutCard(icon: ImageVector, title: String, hint: String, onClick: () -> Unit, modifier: Modifier) {
    SectionCard(onClick = onClick, modifier = modifier.fillMaxHeight()) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(Sizes.icon))
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Footer(versionName: String, onPrivacyOptions: (() -> Unit)?) {
    Column(
        Modifier.fillMaxWidth().padding(top = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(versionName, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall)
        if (onPrivacyOptions != null) TextButton(onClick = onPrivacyOptions) {
            Text(stringResource(R.string.action_ad_privacy), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Spacing.xs))
    }
}
