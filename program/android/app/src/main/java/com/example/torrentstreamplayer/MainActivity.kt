package com.example.torrentstreamplayer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import com.example.torrentstreamplayer.engine.EngineFailureLog
import com.example.torrentstreamplayer.ui.AdConsent
import com.example.torrentstreamplayer.ui.CompletionInterstitial
import com.example.torrentstreamplayer.ui.IncomingLinkParser
import com.example.torrentstreamplayer.ui.IncomingLinkPolicy
import com.example.torrentstreamplayer.ui.MainViewModel
import com.example.torrentstreamplayer.ui.StreamUiState
import com.example.torrentstreamplayer.ui.TestAdsRuntime
import com.example.torrentstreamplayer.ui.screens.AppRoot
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme
import kotlinx.coroutines.CancellationException

/** 테마, 광고 동의·전면 광고 연결, [AppRoot]만 둔다. 화면 선택은 [MainViewModel.screen]이 맡는다. */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Debug builds log any blocking engine call made on the main thread (see TorrentEngine noteSlowCall).
        if (BuildConfig.DEBUG) StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
            .detectCustomSlowCalls().penaltyLog().build())
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 다크 테마 하나라 시스템 바 아이콘은 기기 설정과 관계없이 밝게 둔다.
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        val launchedFromHistory = (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        if (IncomingLinkPolicy.shouldHandle(savedInstanceState != null, launchedFromHistory)) receiveLink(intent)
        val adConsent = AdConsent.get(this)
        adConsent.gather(this) // UMP: refresh consent on every launch, before any ad request.
        setContent {
            TorrentStreamPlayerTheme {
                val uiState by viewModel.uiState.collectAsState()
                val completionAd = remember { CompletionInterstitial(this) }
                DisposableEffect(completionAd) { onDispose { completionAd.dispose() } }
                val adsAllowed by adConsent.canRequestAds.collectAsState()
                val privacyOptionsRequired by adConsent.privacyOptionsRequired.collectAsState()
                LaunchedEffect(uiState is StreamUiState.Playing, adsAllowed) {
                    if (uiState is StreamUiState.Playing && adsAllowed) {
                        try {
                            if (TestAdsRuntime.initialize(this@MainActivity)) completionAd.preload()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            Log.w("CompletionAd", "Load failed: ${EngineFailureLog.describe(failure)}")
                        }
                    }
                }
                AppRoot(
                    viewModel = viewModel,
                    onPrivacyOptions = if (privacyOptionsRequired) ({ adConsent.showPrivacyOptions(this@MainActivity) })
                        else null,
                    onPlaybackFinished = { watchedMs ->
                        completionAd.showIfReady(watchedMs, lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                    }
                )
            }
        }
    }

    /** singleTask: a link tapped or shared while the app runs arrives here instead of a second activity. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveLink(intent)
    }

    /** Passes a valid link to the ViewModel; the link text is never logged. */
    private fun receiveLink(intent: Intent) {
        IncomingLinkParser.parse(intent.action, intent.dataString, intent.getStringExtra(Intent.EXTRA_TEXT))
            ?.let(viewModel::receiveLink)
    }
}
