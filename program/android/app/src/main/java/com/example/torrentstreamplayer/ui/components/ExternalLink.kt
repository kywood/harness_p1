package com.example.torrentstreamplayer.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 주소를 외부 브라우저(`ACTION_VIEW`)로 여는 함수를 돌려준다. 함수는 열었으면 `true`,
 * 열 앱이 없거나(브라우저 없음·비활성) 막혔으면 `false`를 돌려주고 앱은 계속 동작한다.
 */
@Composable
fun rememberExternalLinkOpener(): (String) -> Boolean {
    val context = LocalContext.current
    return remember(context) { { url -> openExternalLink(context, url) } }
}

private fun openExternalLink(context: Context, url: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
