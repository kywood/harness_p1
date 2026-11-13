package com.example.torrentstreamplayer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.engine.SubtitleLanguage
import com.example.torrentstreamplayer.ui.MainViewModel
import com.example.torrentstreamplayer.ui.PlayerTracks
import com.example.torrentstreamplayer.ui.StreamUiState
import com.example.torrentstreamplayer.ui.SubtitlePreference
import com.example.torrentstreamplayer.ui.SubtitleSession
import com.example.torrentstreamplayer.ui.VideoPlaybackPolicy
import com.example.torrentstreamplayer.ui.player.SubtitleSheet
import com.example.torrentstreamplayer.ui.player.SubtitleCopies
import com.example.torrentstreamplayer.ui.player.VlcTrackPlayer
import com.example.torrentstreamplayer.ui.SubtitleMenuPlan
import com.example.torrentstreamplayer.ui.TrackOption
import com.example.torrentstreamplayer.ui.PlayerTrack
import com.example.torrentstreamplayer.ui.theme.TorrentStreamPlayerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 자막 시트와 LibVLC 자막 연결(`design/specs/subtitles.md`, #125·#126).
 *
 * - [sheetListsTracksAndStaysOpenAfterAPick]: 네트워크 없이 시트만 그린다.
 * - [sintelSubtitleCanBeOpenedAndTurnedOff]: 공개 영상 Sintel(Blender Foundation, CC BY)을 실제로 받는다. 피어나 웹 시드가
 *   필요하고, 네트워크가 없으면 건너뛴다(`assumeTrue`). 대기는 단계별이다: Playing까지 300초(공개 스웜이라 편차가 크다),
 *   그 뒤 자막 파일 받기와 자동 켜기까지 60초, 시트 확인·끄기는 단계마다 10초. 실패 메시지에 단계와 걸린 시간이 나오고,
 *   통과한 단계의 시간은 logcat `SubtitleUiTest`에 남는다. 설정·재생 목록·받은 파일은 `finally`에서 되돌리고 지운다.
 * - [koreanSmiAndSrtOpenInVlc]: 직접 만든 한글 `.srt`(UTF-8)·`.smi`(CP949, UTF-8 사본으로 바꿔) 파일을 이미지 영상 위에 붙인다. 글자가 바르게
 *   보이는지는 자동으로 판정하지 않는다. 저장한 화면 캡처를 사람이 본다(경로는 logcat `SubtitleUiTest`).
 */
@RunWith(AndroidJUnit4::class)
class SubtitleUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private fun await(timeoutMs: Long, message: String, check: () -> Boolean) {
        val limit = System.currentTimeMillis() + timeoutMs
        while (!check()) {
            check(System.currentTimeMillis() < limit) { message }
            Thread.sleep(100)
        }
    }

    /**
     * Waits up to [limitMs] for one Sintel stage and logs how long it took (logcat `SubtitleUiTest`). Uses the compose
     * wait, which keeps the compose clock running: the player screen, and the subtitle list it collects, only update
     * with it.
     */
    private fun sintelStage(name: String, limitMs: Long, done: () -> Boolean) {
        val start = System.currentTimeMillis()
        try {
            compose.waitUntil(name, limitMs, done)
        } catch (e: ComposeTimeoutException) {
            val spent = (System.currentTimeMillis() - start) / 1000
            // What was on screen, for the failure report: the compose tree, a screenshot, the view hierarchy outside
            // compose and the app's windows (files under the app's external files folder, paths in logcat).
            runCatching { compose.onAllNodes(isRoot(), useUnmergedTree = true).printToLog("SubtitleUiTest") }
            runCatching { saveFailureState(name) }
            throw IllegalStateException("Sintel stage '$name' did not finish in ${spent}s (limit ${limitMs / 1000}s)", e)
        }
        Log.i("SubtitleUiTest", "Sintel stage '$name' took ${System.currentTimeMillis() - start}ms")
    }

    private fun saveFailureState(stage: String) {
        val dir = File(context.getExternalFilesDir(null), "sintel-failure").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot()?.let { shot ->
            File(dir, "screen.png").outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        fun shell(command: String) = instrumentation.uiAutomation.executeShellCommand(command)
            .let { fd -> android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use { it.readText() } }
        shell("uiautomator dump ${File(dir, "views.xml").absolutePath}")
        File(dir, "windows.txt").writeText(shell("dumpsys window windows").lines()
            .filter { it.contains("pieceplay", ignoreCase = true) || it.contains("Window #") }.joinToString("\n"))
        Log.i("SubtitleUiTest", "Sintel stage '$stage' failure state saved in ${dir.absolutePath}")
    }

    private fun hasDescriptionStarting(prefix: String) = SemanticsMatcher("description starts with $prefix") { node ->
        node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }.any { it.startsWith(prefix) }
    }

    @Test fun sheetListsTracksAndStaysOpenAfterAPick() {
        val picked = AtomicInteger(Int.MIN_VALUE)
        val tracks = PlayerTracks(listOf(PlayerTrack(1, "Track 1 - [Korean]"), PlayerTrack(2, "Track 2 - [English]")),
            listOf(PlayerTrack(10, "Track 1 - [Japanese]"), PlayerTrack(11, "Track 2 - [Korean]")), audioId = 10)
        val state = SubtitleMenuPlan.build(tracks, emptyList())
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                TorrentStreamPlayerTheme {
                    SubtitleSheet(state, onDismiss = {}, onSelectSubtitle = { picked.set(it) }, onSelectAudio = {})
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text(R.string.lang_en)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text(R.string.subtitle_off)).assertExists()
        compose.onNodeWithText(text(R.string.label_audio)).assertExists()
        compose.onNodeWithText(text(R.string.lang_en)).performClick()
        compose.runOnIdle { assertEquals(2, picked.get()) }
        compose.onNodeWithText(text(R.string.label_subtitles)).assertExists()
    }

    @Test fun sintelSubtitleCanBeOpenedAndTurnedOff() {
        assumeTrue("needs a network connection", hasInternet())
        val model = ViewModelProvider(compose.activity)[MainViewModel::class.java]
        val saved = model.settings.value
        val onPrefix = text(R.string.cd_subtitles_on, "")
        val offPrefix = text(R.string.cd_subtitles_off)
        fun shown(prefix: String) =
            compose.onAllNodes(hasDescriptionStarting(prefix), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        try {
            compose.runOnUiThread {
                model.setWifiOnly(false)
                model.setAutoSubtitles(true)
                model.startStreaming(SINTEL)
            }
            sintelStage("Playing (needs Sintel peers or its web seed)", STAGE_PLAYING_MS) {
                model.uiState.value is StreamUiState.Playing
            }
            var lastTap = 0L
            // Hidden controls leave the tree; a tap shows them (the buttons exist only while they are shown).
            // At most one tap a second, so a second tap does not hide them again before they draw.
            fun showControls(visible: Boolean) {
                val now = System.currentTimeMillis()
                if (!visible && now - lastTap > 1_000) {
                    lastTap = now
                    compose.onRoot().performTouchInput { click(center) }
                }
            }
            fun sintelRowShown() =
                shown("Sintel.en.srt") || compose.onAllNodesWithText("Sintel.en.srt").fetchSemanticsNodes().isNotEmpty()
            // The sheet opened right away must fill in while it stays open, without closing and reopening it
            // (#125: it was seen stuck on "트랙을 확인하는 중…" on a finished Sintel).
            val buttonPrefix = onPrefix.commonPrefixWith(offPrefix)
            sintelStage("subtitle button shown", STAGE_OFF_MS) { shown(buttonPrefix).also(::showControls) }
            compose.onAllNodes(hasDescriptionStarting(buttonPrefix), useUnmergedTree = true)[0].performClick()
            sintelStage("open sheet lists Sintel.en.srt by itself", STAGE_SUBTITLE_ON_MS) { sintelRowShown() }
            // One Back only lowers a fully open sheet to half; press again (at most once a second) until it is gone,
            // so the taps that follow land on the player and not on a sheet row such as "끄기".
            var lastBack = 0L
            sintelStage("sheet closed", STAGE_OFF_MS) {
                !sintelRowShown().also { open ->
                    val now = System.currentTimeMillis()
                    if (open && now - lastBack > 1_000) {
                        lastBack = now
                        Espresso.pressBack()
                    }
                }
            }
            // Sintel has no Korean file, so the English file is picked: the button reads "자막, 켜짐: 영어(파일)".
            sintelStage("subtitle files downloaded and one turned on automatically", STAGE_SUBTITLE_ON_MS) {
                model.subtitleFiles.value.isNotEmpty() &&
                    shown(onPrefix).also { on -> showControls(on || shown(offPrefix)) }
            }
            compose.onAllNodes(hasDescriptionStarting(onPrefix), useUnmergedTree = true)[0].performClick()
            // The picked row is read only by its description ("Sintel.en.srt, 자막 파일"); RadioListItem clears its text.
            sintelStage("sheet lists Sintel.en.srt", STAGE_OFF_MS) { sintelRowShown() }
            compose.onNodeWithText(text(R.string.subtitle_off)).performClick()
            sintelStage("subtitles turned off", STAGE_OFF_MS) { shown(offPrefix) }
        } finally {
            compose.runOnUiThread {
                model.closePlayback()
                model.setWifiOnly(saved.wifiOnly)
                model.setAutoSubtitles(saved.autoSubtitles)
            }
            // Stops the download, waits for its writes (awaitStoppedWrites) and deletes its files and history entry.
            runBlocking { model.deleteSavedMagnet(SINTEL) }
        }
    }

    private fun hasInternet(): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    @Test fun koreanSmiAndSrtOpenInVlc() {
        val dir = File(context.cacheDir, "subtitle-ui-" + System.nanoTime()).apply { mkdirs() }
        try {
            // In its own folder: VLC would pick up frame.*.srt beside it by itself and list a third track.
            val image = File(dir, "video/frame.png").apply { parentFile?.mkdirs() }
            image.outputStream().use { out ->
                Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888).also { Canvas(it).drawColor(Color.DKGRAY) }
                    .compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val srt = File(dir, "frame.ko.srt").apply {
                writeText("1\n00:00:00,000 --> 00:00:30,000\n한글 자막 SRT 확인\n", Charsets.UTF_8)
            }
            val smi = File(dir, "frame.kor.smi").apply {
                // The closing SYNC gives the line an end time, like the .srt line.
                writeBytes(("<SAMI><BODY><SYNC Start=0><P Class=KRCC>한글 자막 SMI 확인</P>" +
                    "<SYNC Start=30000><P Class=KRCC>&nbsp;</P></BODY></SAMI>\n")
                    .toByteArray(Charset.forName("x-windows-949")))
            }
            val vlc = LibVLC(context, VideoPlaybackPolicy.options(debug = false))
            val player = MediaPlayer(vlc)
            val failed = AtomicBoolean(false)
            val playing = AtomicBoolean(false)
            player.setEventListener { event ->
                if (event.type == MediaPlayer.Event.EncounteredError) failed.set(true)
                if (event.type == MediaPlayer.Event.Playing) playing.set(true)
            }
            val layout = AtomicReference<VLCVideoLayout>()
            compose.activity.runOnUiThread {
                compose.activity.setContent {
                    AndroidView({ ctx ->
                        VLCVideoLayout(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT)
                            layout.set(this)
                        }
                    }, Modifier.fillMaxSize())
                }
            }
            // The compose rule drives the frame clock; a plain sleep loop would never let AndroidView compose.
            compose.waitUntil(5_000) { layout.get() != null }
            instrumentation.runOnMainSync {
                player.attachViews(layout.get(), null, false, false)
                Media(vlc, Uri.fromFile(image)).also {
                    it.addOption(":image-duration=60")
                    player.media = it
                    it.release()
                }
                player.play()
            }
            await(15_000, "VLC could not play the still image") { playing.get() || failed.get() }
            assertFalse("VLC reported an error for the still image", failed.get())
            val tracks = VlcTrackPlayer(player)
            // The CP949 .smi goes through a UTF-8 copy, as on the player screen (SubtitleEncodingPolicy).
            val copies = SubtitleCopies(File(dir, "copies"))
            copies.prepare(srt)
            copies.prepare(smi)
            assertTrue("the .smi is re-encoded", copies.copyOf(smi).readText(Charsets.UTF_8).contains("한글 자막 SMI"))
            assertEquals("the UTF-8 .srt goes as it is", srt, copies.copyOf(srt))
            val session = SubtitleSession(tracks, autoEnabled = true, preference = SubtitlePreference.saved("ko"),
                prepare = copies::copyOf)
            session.start()
            val files = listOf(SubtitleFile(srt, SubtitleLanguage.KOREAN, true, true),
                SubtitleFile(smi, SubtitleLanguage.KOREAN, true, true))
            session.updateFiles(files, emptyList())
            // Files are added one at a time; each refresh matches the newest track and adds the next file.
            var fileRows = emptyList<TrackOption>()
            await(20_000, "VLC did not list the two subtitle files") {
                fileRows = session.refresh().sheet.subtitles.filter { it.enabled }
                fileRows.size >= 2
            }
            assertEquals(2, fileRows.size)
            for (row in fileRows) {
                assertTrue("VLC refused subtitle track ${row.name}", session.selectSubtitle(row.id))
                // A still image barely moves the clock, so a track picked later gets no line until it reads again.
                player.time = 0
                Thread.sleep(1_500)
                val shot = instrumentation.uiAutomation.takeScreenshot()
                assertNotNull(shot)
                val out = File(context.getExternalFilesDir(null), "subtitle-${row.id}.png")
                out.outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                Log.i("SubtitleUiTest", "Check Korean subtitle text in ${out.absolutePath}")
            }
            assertFalse(failed.get())
            instrumentation.runOnMainSync {
                player.stop()
                player.detachViews()
                player.release()
                vlc.release()
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    private companion object {
        /** Sintel from the WebTorrent demo set, with its web seed; it contains `Sintel.en.srt` and other subtitles. */
        const val SINTEL = "magnet:?xt=urn:btih:08ada5a7a6183aae1e09d831df6748d566095a10&dn=Sintel" +
            "&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337" +
            "&tr=wss%3A%2F%2Ftracker.webtorrent.dev&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2F"
        /** Start to Playing: a public swarm, so it varies widely with the network. */
        const val STAGE_PLAYING_MS = 300_000L
        /** Playing to a subtitle file downloaded and turned on automatically. */
        const val STAGE_SUBTITLE_ON_MS = 60_000L
        /** Each sheet step: listing the file, turning subtitles off. */
        const val STAGE_OFF_MS = 10_000L
    }
}
