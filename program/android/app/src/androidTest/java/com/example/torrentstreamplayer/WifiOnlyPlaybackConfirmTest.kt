package com.example.torrentstreamplayer

import android.app.Application
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.data.ConnectionSource
import com.example.torrentstreamplayer.data.ConnectionType
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.TorrentCache
import com.example.torrentstreamplayer.engine.TorrentCacheCipher
import com.example.torrentstreamplayer.engine.TorrentStorage
import com.example.torrentstreamplayer.meta.TorrentStorageDir
import com.example.torrentstreamplayer.ui.MainViewModel
import com.example.torrentstreamplayer.ui.navigation.AppScreen
import com.example.torrentstreamplayer.ui.screens.MobilePlaybackConfirmDialog
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.swig.libtorrent
import java.io.File

/**
 * Wi-Fi 전용 + 모바일 데이터에서 덜 받은 영상을 메인에서 재생하면 확인 창이 뜨고(`design/specs/settings-wifi.md` 5절),
 * "이번만 재생"은 재생을 시작하고 "취소"·뒤로·바깥 누름은 아무것도 시작하지 않는다. 연결은 가짜 [ConnectionSource]로 MOBILE에 고정한다.
 */
@RunWith(AndroidJUnit4::class)
class WifiOnlyPlaybackConfirmTest {
    @get:Rule val compose = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var root: File
    private lateinit var model: MainViewModel
    private lateinit var link: String

    private fun text(id: Int): String = instrumentation.targetContext.getString(id)

    private fun await(check: () -> Boolean) {
        compose.waitUntil(15_000) { check() }
    }

    @Before fun setUp() {
        root = File(instrumentation.targetContext.cacheDir, "wifi-confirm-test-" + System.nanoTime()).apply { mkdirs() }
        val app = object : Application() {
            override fun getApplicationContext() = this
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
        }
        val mobile = object : ConnectionSource {
            override val connection: StateFlow<ConnectionType> = MutableStateFlow(ConnectionType.MOBILE)
            override fun stop() = Unit
        }
        instrumentation.runOnMainSync { model = MainViewModel(app, mobile) }
        link = seedIncomplete(app, "wifi-confirm.mkv")
        instrumentation.runOnMainSync { model.setWifiOnly(true) }
        await { model.settings.value.wifiOnly }
        compose.setContent {
            TorrentStreamPlayerTheme {
                val request by model.mobilePlaybackConfirm.collectAsState()
                if (request != null) MobilePlaybackConfirmDialog(model::confirmMobilePlayback, model::dismissMobilePlayback)
            }
        }
    }

    @After fun tearDown() {
        try {
            instrumentation.runOnMainSync {
                model.stopStream()
                model.downloads.stop()
            }
            // 정지는 비동기라 엔진이 아직 쓰는 중일 수 있다. 다른 계측 테스트처럼 쓰기가 끝난 뒤에 지운다.
            model.downloads.awaitStoppedWrites(model.downloads.stoppedWriters())
        } finally {
            root.deleteRecursively()
        }
    }

    /** 메타데이터만 있는(덜 받은) 토렌트를 격리된 저장소에 둔다. */
    private fun seedIncomplete(app: Application, name: String): String {
        val bytes = ("d4:infod6:lengthi32768e4:name" + name.length + ":" + name +
            "12:piece lengthi16384e6:pieces40:" + "\u0000".repeat(40) + "7:privatei1eee").toByteArray()
        val params = AddTorrentParams().apply { setTorrentInfo(TorrentInfo.bdecode(bytes)) }
        val magnet = libtorrent.make_magnet_uri(params.swig())
        val cache = TorrentCache(TorrentStorage.of(app).dir(TorrentStorageDir.STATE), MagnetIdentity.key(magnet),
            TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt)
        cache.write("metadata", bytes)
        return magnet
    }

    private fun askFromMain() {
        instrumentation.runOnMainSync { model.playFromMain(link) }
        await { model.mobilePlaybackConfirm.value != null }
        await { compose.onAllNodesWithText(text(R.string.mobile_play_confirm_title)).fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { assertTrue(model.screen.value is AppScreen.Main) }
    }

    @Test fun playOnceStartsPlaybackAndClosesTheDialog() {
        askFromMain()
        compose.onNodeWithText(text(R.string.action_play_once)).performClick()
        await { model.screen.value is AppScreen.Preparing || model.screen.value is AppScreen.Player }
        compose.runOnIdle {
            assertNull(model.mobilePlaybackConfirm.value)
            assertTrue(model.settings.value.wifiOnly)
        }
        assertTrue(compose.onAllNodesWithText(text(R.string.mobile_play_confirm_title)).fetchSemanticsNodes().isEmpty())
    }

    @Test fun backDismissesWithoutStarting() {
        askFromMain()
        Espresso.pressBack()
        compose.runOnIdle {
            assertNull(model.mobilePlaybackConfirm.value)
            assertEquals(AppScreen.Main(), model.screen.value)
        }
        assertTrue(compose.onAllNodesWithText(text(R.string.mobile_play_confirm_title)).fetchSemanticsNodes().isEmpty())
    }

    @Test fun tapOutsideDismissesWithoutStarting() {
        askFromMain()
        // 확인 창 바깥(화면 왼쪽 위 모서리)을 누른다. 새 의존성(UiAutomator) 없이 터치를 주입한다.
        val time = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            val event = MotionEvent.obtain(time, SystemClock.uptimeMillis(), action, 1f, 1f, 0)
            instrumentation.uiAutomation.injectInputEvent(event, true)
            event.recycle()
        }
        compose.waitUntil(5_000) { model.mobilePlaybackConfirm.value == null }
        compose.runOnIdle { assertEquals(AppScreen.Main(), model.screen.value) }
        assertTrue(compose.onAllNodesWithText(text(R.string.mobile_play_confirm_title)).fetchSemanticsNodes().isEmpty())
    }

    @Test fun cancelStartsNothing() {
        askFromMain()
        compose.onNodeWithText(text(R.string.action_cancel)).performClick()
        compose.runOnIdle {
            assertNull(model.mobilePlaybackConfirm.value)
            assertEquals(AppScreen.Main(), model.screen.value)
        }
        assertTrue(compose.onAllNodesWithText(text(R.string.mobile_play_confirm_title)).fetchSemanticsNodes().isEmpty())
    }
}
