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
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.engine.SubtitleFile
import com.example.torrentstreamplayer.engine.SubtitleLanguage
import com.example.torrentstreamplayer.ui.MainViewModel
import com.example.torrentstreamplayer.ui.PlayerTracks
import com.example.torrentstreamplayer.ui.StreamUiState
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
 *   필요하고, 네트워크가 없으면 건너뛴다(`assumeTrue`). 전체 대기는 120초까지. 설정·재생 목록·받은 파일은 `finally`에서
 *   되돌리고 지운다.
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
        // One deadline for the whole test, so a missing network or peer holds the run for at most this long.
        val deadline = System.currentTimeMillis() + PLAYBACK_TIMEOUT_MS
        fun remainingMs() = (deadline - System.currentTimeMillis()).coerceAtLeast(1)
        try {
            compose.runOnUiThread {
                model.setWifiOnly(false)
                model.setAutoSubtitles(true)
                model.startStreaming(SINTEL)
            }
            await(remainingMs(), "Sintel did not start; this test needs Sintel peers or its web seed") {
                model.uiState.value is StreamUiState.Playing
            }
            await(remainingMs(), "Sintel subtitle files were not downloaded") { model.subtitleFiles.value.isNotEmpty() }
            val onPrefix = text(R.string.cd_subtitles_on, "")
            // Sintel has no Korean file, so the English file is picked: the button reads "자막, 켜짐: 영어(파일)".
            while (compose.onAllNodes(hasDescriptionStarting(onPrefix), useUnmergedTree = true).fetchSemanticsNodes()
                    .isEmpty()) {
                check(System.currentTimeMillis() < deadline) { "No subtitle was turned on automatically" }
                // Hidden controls leave the tree; a tap shows them (the button exists only while they are shown).
                if (compose.onAllNodes(hasDescriptionStarting(text(R.string.cd_subtitles_off)), useUnmergedTree = true)
                        .fetchSemanticsNodes().isEmpty()) compose.onRoot().performTouchInput { click(center) }
                Thread.sleep(500)
            }
            compose.onAllNodes(hasDescriptionStarting(onPrefix), useUnmergedTree = true)[0].performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Sintel.en.srt").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(text(R.string.subtitle_off)).performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasDescriptionStarting(text(R.string.cd_subtitles_off)), useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
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
            val session = SubtitleSession(tracks, autoEnabled = true, preferredKey = "ko", prepare = copies::copyOf)
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
        const val PLAYBACK_TIMEOUT_MS = 120_000L
    }
}
