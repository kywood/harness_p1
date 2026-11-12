package com.example.torrentstreamplayer.ui.screens

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.meta.LicenseDocument
import com.example.torrentstreamplayer.meta.LicenseText
import com.example.torrentstreamplayer.meta.OpenSourceComponent
import com.example.torrentstreamplayer.ui.LicenseParagraphs
import com.example.torrentstreamplayer.ui.components.AppTopBar
import com.example.torrentstreamplayer.ui.components.EmptyState
import com.example.torrentstreamplayer.ui.components.SectionCard
import com.example.torrentstreamplayer.ui.components.TextAction
import com.example.torrentstreamplayer.ui.theme.Sizes
import com.example.torrentstreamplayer.ui.theme.Spacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 전문 파일을 읽는 상태. */
private sealed interface LicenseTextState {
    data object Loading : LicenseTextState
    data class Loaded(val paragraphs: List<String>) : LicenseTextState
    data object Failed : LicenseTextState
}

/** 목록 값을 화면 문구로: 고유명사는 그대로, 한국어가 붙는 값은 strings.xml 문구로. */
@Composable
private fun LicenseText.resolve(): String = when (this) {
    is LicenseText.Literal -> value
    is LicenseText.Localized -> if (arg == null) stringResource(format) else stringResource(format, arg)
}

/**
 * 오픈소스 라이선스 화면(`design/specs/open-source-licenses.md`): 목록 → 항목을 누르면 전문 보기.
 * 뒤로(상단 바·시스템)는 전문 → 목록 → [onBack]. 광고는 넣지 않는다.
 */
@Composable
internal fun OpenSourceLicensesScreen(onBack: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf<OpenSourceComponent?>(null) }
    val back = { if (selected != null) selected = null else onBack() }
    BackHandler(onBack = back)
    val snackbar = remember { SnackbarHostState() }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            val current = selected
            val text = current?.let { rememberLicenseText(it.document) }
            AppTopBar(current?.displayName?.resolve() ?: stringResource(R.string.action_open_source_licenses), onBack = back)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                // 목록과 전문은 스크롤 위치를 따로 둔다. 전문은 항목마다 새로 만들어 처음 열면 맨 위다.
                val listState = rememberLazyListState()
                val detailState = remember(current) { LazyListState() }
                LazyColumn(
                    state = if (current == null) listState else detailState,
                    modifier = Modifier.widthIn(max = Sizes.contentMaxWidth).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = Spacing.screenGutter, vertical = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    if (current == null) licenseList { selected = it }
                    else licenseDetail(current, text ?: LicenseTextState.Loading, snackbar)
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private fun LazyListScope.licenseList(onOpen: (OpenSourceComponent) -> Unit) {
    item(key = "intro") {
        Text(stringResource(R.string.licenses_intro), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    items(OpenSourceComponent.entries, key = { it.name }) { component ->
        SectionCard(onClick = { onOpen(component) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(component.displayName.resolve(), style = MaterialTheme.typography.titleMedium, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.licenses_item_meta, component.version.resolve(),
                        component.licenseName.resolve()),
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (component.sourceOffer) Text(stringResource(R.string.licenses_source_available),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Sizes.icon))
            }
        }
    }
    item(key = "others") {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.licenses_others_title), style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.licenses_others_body), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun LazyListScope.licenseDetail(component: OpenSourceComponent, text: LicenseTextState,
    snackbar: SnackbarHostState) {
    item(key = "notice") { LicenseNoticeCard(component, snackbar) }
    when (text) {
        LicenseTextState.Loading -> Unit
        LicenseTextState.Failed -> item(key = "error") {
            EmptyState(Icons.Filled.Warning, stringResource(R.string.licenses_load_error))
        }
        // 문단마다 목록 항목: 긴 전문(LGPL 약 500줄)을 Text 하나로 그리지 않는다.
        is LicenseTextState.Loaded -> itemsIndexed(text.paragraphs, key = { index, _ -> "p$index" }) { _, paragraph ->
            SelectionContainer {
                Text(paragraph, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun LicenseNoticeCard(component: OpenSourceComponent, snackbar: SnackbarHostState) {
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val browserUnavailable = stringResource(R.string.error_browser_unavailable)
    SectionCard {
        Text(component.copyright, style = MaterialTheme.typography.bodyMedium)
        if (component.lgplNotice) Text(stringResource(R.string.licenses_lgpl_notice),
            style = MaterialTheme.typography.bodyMedium)
        TextAction(stringResource(R.string.licenses_source_code), onClick = {
            try { uriHandler.openUri(component.url) }
            catch (_: ActivityNotFoundException) { scope.launch { snackbar.showSnackbar(browserUnavailable) } }
            catch (_: IllegalArgumentException) { scope.launch { snackbar.showSnackbar(browserUnavailable) } }
        })
    }
}

/** [document]를 IO 스레드에서 읽어 문단으로 나눈다. 다른 항목을 열면 새로 읽는다. */
@Composable
private fun rememberLicenseText(document: LicenseDocument): LicenseTextState {
    val context = LocalContext.current
    val state by produceState<LicenseTextState>(LicenseTextState.Loading, document) {
        value = try {
            val text = withContext(Dispatchers.IO) {
                context.resources.openRawResource(document.resource).bufferedReader().use { it.readText() }
            }
            LicenseTextState.Loaded(LicenseParagraphs.split(text))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            LicenseTextState.Failed
        }
    }
    return state
}
