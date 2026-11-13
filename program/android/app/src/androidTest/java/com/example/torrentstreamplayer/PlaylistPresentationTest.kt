package com.example.torrentstreamplayer

import com.example.torrentstreamplayer.engine.TorrentStorage
import com.example.torrentstreamplayer.meta.TorrentStorageDir
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.*
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.torrentstreamplayer.engine.*
import com.example.torrentstreamplayer.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.libtorrent4j.*
import org.libtorrent4j.swig.libtorrent
import java.io.File
import kotlinx.coroutines.runBlocking
class PlaylistPresentationTest {
    private fun verifyQuickActions(link: String, position: java.util.concurrent.atomic.AtomicLong, added: java.util.concurrent.atomic.AtomicBoolean) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> host = activity; activity.setContent {
                MagnetHistoryScreen({ listOf(SavedMagnet(link, "Resume video", 1, lastPositionMs = position.get())) }, {}, {}, {},
                    onAddDownload = { added.set(true) }, onResetPosition = { position.set(0) })
            } }
            await { nodes(s(R.string.playlist_resume, "").trim()).isNotEmpty() }
            val resumeBounds = nodes(s(R.string.playlist_resume, "").trim()).first().boundsInRoot
            val resetBounds = nodes(s(R.string.action_reset_position)).first().boundsInRoot
            assertEquals(resumeBounds.center.y, resetBounds.center.y, 2f)
            assertTrue(resetBounds.left >= resumeBounds.right)
            click(s(R.string.action_add_link)); assertTrue(added.get())
            click(s(R.string.action_reset_position)); await { nodes(s(R.string.playlist_resume, "").trim()).isEmpty() }
        }
    }
    @Test fun playlistQuickActionsKeepScreenAndResetOnlyResumePosition() {
        val link = "magnet:?xt=urn:btih:" + "e".repeat(40)
        val position = java.util.concurrent.atomic.AtomicLong(42000)
        val added = java.util.concurrent.atomic.AtomicBoolean()
        verifyQuickActions(link, position, added)
    }
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var host: MainActivity
    private fun s(id: Int, vararg args: Any): String = instrumentation.targetContext.getString(id, *args)
    private fun nodes(text: String): List<SemanticsNode> {
        var result = emptyList<SemanticsNode>()
        instrumentation.runOnMainSync {
            fun roots(view: View): List<RootForTest> = when (view) {
                is RootForTest -> listOf(view)
                is ViewGroup -> (0 until view.childCount).flatMap { roots(view.getChildAt(it)) }
                else -> emptyList()
            }
            result = roots(host.window.decorView).flatMap { it.semanticsOwner.getAllSemanticsNodes(true) }
                .filter { it.config.getOrNull(SemanticsProperties.Text)?.any { value -> text in value.text } == true }
        }
        return result
    }
    private fun await(check: () -> Boolean) {
        val limit = SystemClock.uptimeMillis() + 10000
        while (!check()) { check(SystemClock.uptimeMillis() < limit) { "Playlist UI did not update" }; Thread.sleep(50) }
    }
    private fun click(text: String) {
        fun candidates() = nodes(text).filter { !it.config.contains(SemanticsProperties.Disabled) &&
            it.config.getOrNull(SemanticsProperties.Text)?.any { value -> value.text == text } == true }
        await { candidates().isNotEmpty() }
        var node = candidates().first()
        repeat(5) {
            val action = node.config.getOrNull(SemanticsActions.OnClick)?.action
            if (action != null) { instrumentation.runOnMainSync { check(action()) }; return }
            node = node.parent ?: error("No clickable parent for " + text)
        }
        error("No clickable node for " + text)
    }
    @Test fun simpleModeHidesAddressAndPieceDescriptionButKeepsProgress() {
        verifySimpleMode()
    }
    @Test fun sortingFiltersAndLiveCompletionUpdateDisplayedPlaylist() {
        verifySortingAndFilters()
    }
    private fun verifySortingAndFilters() {
        verifyPlaylistSorting()
    }
    @Test fun multipleJobsShowIndependentSpeedAndStopControls() {
        verifyMultipleJobs()
    }
    private fun verifyMultipleJobs() {
        verifyIndependentJobs()
    }
    @Test fun selectionActionsStayInOneRowAndToggleOnlySelectedJobs() {
        verifySelectionToggle()
    }
    private fun verifySelectionToggle() {
        val links = listOf("a", "b").map { "magnet:?xt=urn:btih:" + it.repeat(40) }
        val keys = links.map(MagnetIdentity::key).toSet()
        val entries = links.mapIndexed { index, link -> SavedMagnet(link, "Selection $index", index.toLong(), 100, 25) }
        val outside = "other"
        val jobs = mutableStateOf(mapOf(outside to DownloadEntry(outside, DownloadPhase.RUNNING)))
        val stopped = java.util.concurrent.atomic.AtomicReference<Set<String>>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> host = activity; activity.setContent {
                MagnetHistoryScreen({ entries }, {}, {}, {}, simple = true, downloadJobs = jobs.value,
                    onDownload = { val key = MagnetIdentity.key(it); jobs.value = jobs.value + (key to DownloadEntry(key, DownloadPhase.RUNNING)) },
                    onStopItems = { stopped.set(it); jobs.value = jobs.value - it },
                    downloadCount = jobs.value.values.count { it.phase == DownloadPhase.RUNNING })
            } }
            await { nodes("Selection 0").isNotEmpty() }
            click(s(R.string.bulk_select_all))
            await { nodes(s(R.string.action_delete)).isNotEmpty() }
            val center = nodes(s(R.string.bulk_select_all)).first().boundsInRoot.center.y
            assertEquals(center, nodes(s(R.string.action_delete)).first().boundsInRoot.center.y, 2f)
            val downloadButton = nodes(s(R.string.action_download)).first { it.config.getOrNull(SemanticsProperties.Text)?.any { value -> value.text == s(R.string.action_download) } == true }
            assertEquals(center, downloadButton.boundsInRoot.center.y, 2f)
            assertTrue(nodes("선택 다운로드").isEmpty()); assertTrue(nodes(s(R.string.bulk_delete_selected, 0).substringBefore(" (")).isEmpty())
            click(s(R.string.action_download))
            await { jobs.value.size == 3 && nodes(s(R.string.action_stop)).isNotEmpty() }
            click(s(R.string.action_stop))
            await { jobs.value.keys == setOf(outside) }
            assertEquals(keys, stopped.get())
        }
    }
    private fun verifyIndependentJobs() {
        val links = (1..3).map { "magnet:?xt=urn:btih:" + it.toString().repeat(40) }
        val keys = links.map(MagnetIdentity::key)
        val entries = links.mapIndexed { index, link -> SavedMagnet(link, "UI download $index", (3 - index).toLong(), 100, 25) }
        val jobs = mutableStateOf(keys.mapIndexed { index, key -> key to DownloadEntry(key,
            if (index < 2) DownloadPhase.RUNNING else DownloadPhase.QUEUED,
            if (index < 2) DownloadSnapshot(100, 25, "fake-$index") else null, speed = 2048) }.toMap())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> host = activity; activity.setContent {
                MagnetHistoryScreen({ entries }, {}, {}, {}, simple = true, downloadJobs = jobs.value,
                    onStopItem = { jobs.value = jobs.value - it }, onStopDownload = { jobs.value = emptyMap() },
                    downloadCount = jobs.value.values.count { it.phase == DownloadPhase.RUNNING })
            } }
            await { nodes(s(R.string.playlist_summary_queued, 2, 1)).isNotEmpty() && nodes("/s").size == 2 }
            click(s(R.string.action_stop))
            await { nodes(s(R.string.playlist_summary_queued, 1, 1)).isNotEmpty() && nodes("/s").size == 1 }
            assertTrue(nodes("UI download 1").isNotEmpty())
            click(s(R.string.action_stop_all))
            await { nodes(s(R.string.playlist_summary, 0)).isNotEmpty() && nodes(s(R.string.action_stop_all)).isEmpty() }
        }
    }
    private fun verifyPlaylistSorting() {
        val sort = mutableStateOf(PlaylistSort.RECENT_PLAY)
        val filter = mutableStateOf(PlaylistFilter.ALL)
        val received = mutableStateOf(25L)
        val old = SavedMagnet("magnet:?xt=urn:btih:" + "6".repeat(40), "Older complete", 10, 100, 100, lastPlayedAt = 300)
        val newer = SavedMagnet("magnet:?xt=urn:btih:" + "5".repeat(40), "Newer partial", 20, 100, 25, lastPlayedAt = 200)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> host = activity; activity.setContent {
                MagnetHistoryScreen({ listOf(newer, old) }, {}, {}, {}, simple = true,
                    activeKey = MagnetIdentity.key(newer.magnet), activeProgress = 100L to received.value,
                    sort = sort.value, onSortChange = { sort.value = it },
                    filter = filter.value, onFilterChange = { filter.value = it })
            } }
            await { nodes(old.title).isNotEmpty() && nodes(newer.title).isNotEmpty() }
            assertTrue(nodes(old.title).first().boundsInRoot.top < nodes(newer.title).first().boundsInRoot.top)
            instrumentation.runOnMainSync { sort.value = PlaylistSort.RECEIVED }
            await { nodes(newer.title).first().boundsInRoot.top < nodes(old.title).first().boundsInRoot.top }
            instrumentation.runOnMainSync { filter.value = PlaylistFilter.COMPLETE }
            await { nodes(newer.title).isEmpty() && nodes(old.title).isNotEmpty() }
            instrumentation.runOnMainSync { filter.value = PlaylistFilter.INCOMPLETE }
            await { nodes(old.title).isEmpty() && nodes(newer.title).isNotEmpty() }
            instrumentation.runOnMainSync { received.value = 100L }
            await { nodes(newer.title).isEmpty() && nodes(s(R.string.playlist_filter_empty)).isNotEmpty() }
        }
    }
    @Test fun stoppingDownloadUpdatesCountWithoutFalseReadError() {
        verifyDownloadCount()
    }
    @Test fun actualReadFailureCanBeRetriedAndErrorClears() {
        verifyLoadRetry()
    }
    private fun verifySimpleMode() {
        verifyPlaylistSimpleMode()
    }
    private fun verifyDownloadCount() {
        val link = "magnet:?xt=urn:btih:" + "8".repeat(40)
        val entry = SavedMagnet(link, "Download count sample", 1, 100, 25)
        val calls = java.util.concurrent.atomic.AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> host = activity; activity.setContent {
                var task by remember { mutableStateOf(DownloadTaskState()) }
                MagnetHistoryScreen({ calls.incrementAndGet(); listOf(entry) }, {}, {}, {},
                    onDownload = { task = DownloadTaskState(key = MagnetIdentity.key(it)) },
                    onStopDownload = { task = DownloadTaskState() }, activeKey = task.key,
                    downloadCount = task.activeCount)
            } }
            await { nodes(s(R.string.playlist_summary, 0)).isNotEmpty() && calls.get() >= 1 }
            click(s(R.string.action_download))
            await { nodes(s(R.string.playlist_summary, 1)).isNotEmpty() && calls.get() >= 2 }
            click(s(R.string.action_stop_download))
            await { nodes(s(R.string.playlist_summary, 0)).isNotEmpty() && calls.get() >= 3 }
            assertTrue(nodes(s(R.string.playlist_load_error)).isEmpty())
        }
    }
    private fun verifyLoadRetry() {
        val first = java.util.concurrent.atomic.AtomicBoolean(true)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> host = activity; activity.setContent {
                MagnetHistoryScreen({ if (first.getAndSet(false)) throw java.io.IOException("read failed")
                    else emptyList() }, {}, {}, {})
            } }
            await { nodes(s(R.string.playlist_load_error)).isNotEmpty() }
            click(s(R.string.action_retry))
            await { nodes(s(R.string.playlist_load_error)).isEmpty() }
        }
    }
    private fun verifyPlaylistSimpleMode() {
        val link = "magnet:?xt=urn:btih:" + "7".repeat(40)
        val entry = SavedMagnet(link, "Presentation sample", 1, 100, 25, PieceMapSnapshot.capture(0, 9) { it < 3 })
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> host = activity; activity.setContent {
                var simple by remember { mutableStateOf(false) }
                MagnetHistoryScreen({ listOf(entry) }, {}, {}, {}, simple = simple, onSimpleChange = { simple = it })
            } }
            await { nodes(s(R.string.piece_map_info, 0, 0).substringBefore(" · ")).isNotEmpty() && nodes("magnet:?").isNotEmpty() }
            val density = instrumentation.targetContext.resources.displayMetrics.density
            val intro = nodes(s(R.string.playlist_storage_note)).first().boundsInRoot
            val count = nodes(s(R.string.playlist_summary, 0)).first().boundsInRoot
            assertTrue("Download count must precede storage notice", count.bottom <= intro.top)
            assertTrue("Playlist header spacing is too large", intro.top - count.bottom <= 6 * density)
            click(s(R.string.action_simple))
            await { nodes(s(R.string.piece_map_info, 0, 0).substringBefore(" · ")).isEmpty() && nodes("magnet:?").isEmpty() }
            assertTrue(nodes("Presentation sample").isNotEmpty())
            assertTrue(nodes("25.0%").isNotEmpty())
            click(s(R.string.action_detailed))
            await { nodes(s(R.string.piece_map_info, 0, 0).substringBefore(" · ")).isNotEmpty() && nodes("magnet:?").isNotEmpty() }
        }
    }
    @Test fun cancelAndSystemBackFromPlaylistPlaybackReturnToPlaylist() {
        val context = instrumentation.targetContext
        val name = "playlist-return-" + System.nanoTime() + ".mkv"
        val metadata = ("d4:infod6:lengthi32768e4:name" + name.length + ":" + name +
            "12:piece lengthi16384e6:pieces40:" + "\u0000".repeat(40) + "7:privatei1eee").toByteArray()
        val params = AddTorrentParams().apply { setTorrentInfo(TorrentInfo.bdecode(metadata)) }
        val link = libtorrent.make_magnet_uri(params.swig())
        val key = MagnetIdentity.key(link)
        val history = MagnetHistory(TorrentCache(File(context.noBackupFilesDir, "magnet_history"),
            "0".repeat(64), TorrentCacheCipher::encrypt, TorrentCacheCipher::decrypt))
        TorrentCache(TorrentStorage.of(context).dir(TorrentStorageDir.STATE), key, TorrentCacheCipher::encrypt,
            TorrentCacheCipher::decrypt).write("metadata", metadata)
        history.add(link); history.updateTitle(link, name)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var model: MainViewModel
            scenario.onActivity { host = it; model = ViewModelProvider(it)[MainViewModel::class.java] }
            try {
                click(s(R.string.action_open_playlist))
                await { nodes(name).isNotEmpty() }
                click(s(R.string.action_play))
                await { nodes(s(R.string.action_go_main)).isNotEmpty() }
                click(s(R.string.action_open_playlist))
                await { nodes(s(R.string.action_open_playlist)).isNotEmpty() && nodes(name).isNotEmpty() }
                assertEquals(StreamUiState.Idle, model.uiState.value)
                await { model.downloads.entry(key)?.phase == DownloadPhase.RUNNING }
                // The engine starts on the engine command queue shortly after the task is marked running.
                await { model.downloads.engineFor(key)?.isDownloading == true }
                click(s(R.string.action_play))
                click(s(R.string.action_go_main))
                await { nodes(s(R.string.action_open_stored_files)).isNotEmpty() }
                assertEquals(StreamUiState.Idle, model.uiState.value)
                assertNull(model.downloads.entry(key))
                click(s(R.string.action_open_playlist))
                await { nodes(name).isNotEmpty() }
                click(s(R.string.action_play))
                await { model.uiState.value !is StreamUiState.Idle && nodes(s(R.string.action_go_main)).isNotEmpty() }
                scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                await { nodes(s(R.string.action_open_playlist)).isNotEmpty() && nodes(name).isNotEmpty() }
                assertEquals(StreamUiState.Idle, model.uiState.value)
                click(s(R.string.action_close))
                await { nodes(s(R.string.action_open_stored_files)).isNotEmpty() }
            } finally { runBlocking { model.deleteSavedMagnet(link) } }
        }
    }
}
