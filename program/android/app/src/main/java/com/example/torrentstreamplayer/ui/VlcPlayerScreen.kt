package com.example.torrentstreamplayer.ui

import android.util.Log
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.torrentstreamplayer.engine.TorrentEngine
import com.example.torrentstreamplayer.engine.TorrentStreamServer
import com.example.torrentstreamplayer.ui.player.PlayerBottomPanel
import com.example.torrentstreamplayer.ui.player.PlayerCenterControls
import com.example.torrentstreamplayer.ui.player.PlayerErrorState
import com.example.torrentstreamplayer.ui.player.PlayerMoreSheet
import com.example.torrentstreamplayer.ui.player.PlayerTimePill
import com.example.torrentstreamplayer.ui.player.PlayerTopBar
import com.example.torrentstreamplayer.ui.theme.PlaybackColors
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.example.torrentstreamplayer.R

private class PlayerHolder(val libVlc: LibVLC, val mediaPlayer: MediaPlayer) {
    /**
     * VLC stop/release waits for its input thread, which can be blocked on a stream read for seconds.
     * Views are detached on the main thread; the native teardown runs on a background thread.
     */
    fun releaseAsync(streamServer: TorrentStreamServer) {
        try { mediaPlayer.detachViews() } catch (e: Exception) { Log.e("VlcPlayerScreen", "Error detaching VLC views", e) }
        // Closing the stream first unblocks VLC's pending read so its stop() returns quickly.
        RELEASER.execute { runCatching { streamServer.stop() }.onFailure { Log.e("VlcPlayerScreen", "Error stopping stream", it) } }
        // Seeks posted earlier still touch mediaPlayer; schedule the release only after they have run.
        EngineCommandQueue.shared.post { RELEASER.execute(::release) }
    }

    private fun release() {
        try {
            mediaPlayer.stop()
            mediaPlayer.release()
            libVlc.release()
        } catch (e: Exception) {
            Log.e("VlcPlayerScreen", "Error releasing VLC player", e)
        }
    }

    private companion object {
        val RELEASER: java.util.concurrent.ExecutorService = java.util.concurrent.Executors.newSingleThreadExecutor { task ->
            Thread(task, "vlc-release").apply { isDaemon = true }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VlcPlayerScreen(
    fileName: String,
    torrentEngine: TorrentEngine,
    onBackClicked: () -> Unit, onAddDownload: (() -> Unit)? = null,
    initialPositionMs: Long = 0, onPositionChanged: (Long, Long) -> Unit = { _, _ -> },
    onPlaybackCompleted: (Long) -> Unit = {}, onPlaybackExited: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val activity = remember(context) { VideoOrientation.activity(context) }
    DisposableEffect(activity) {
        val previous = activity?.requestedOrientation
        onDispose { if (previous != null) activity?.requestedOrientation = previous }
    }
    val streamServer = remember(torrentEngine) { TorrentStreamServer(0, torrentEngine) }
    val pieces by torrentEngine.pieceMap.collectAsState()
    var isPlaying by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentTimeMs by remember { mutableLongStateOf(0L) }
    var totalLengthMs by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var swipeStartMs by remember { mutableLongStateOf(0) }
    var swipePreviewMs by remember { mutableStateOf<Long?>(null) }
    var seekUi by remember { mutableStateOf(SeekUiState()) }
    var resumeApplied by remember(torrentEngine) { mutableStateOf(initialPositionMs <= 0) }
    val positionCallback by rememberUpdatedState(onPositionChanged)
    val completionCallback by rememberUpdatedState(onPlaybackCompleted)
    val watchTime = remember(torrentEngine) { PlaybackWatchTime() }
    val exitGate = remember(torrentEngine) { PlaybackExitGate() }
    fun exitPlayback() {
        if (exitGate.consume()) {
            if (onPlaybackExited != null && errorMessage == null) onPlaybackExited(watchTime.watchedMs)
            else onBackClicked()
        }
    }
    var playbackCompleted by remember(torrentEngine) { mutableStateOf(false) }
    androidx.activity.compose.BackHandler { exitPlayback() }

    var showControls by remember { mutableStateOf(true) }
    var showSeekBarOnly by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var zoom by remember { mutableStateOf(VideoZoomState()) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    // Download Stats State
    var downloadSpeedBytes by remember { mutableLongStateOf(0L) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var numPeers by remember { mutableIntStateOf(0) }
    val displayedTimeMs = swipePreviewMs ?: if (isSeeking && totalLengthMs > 0)
        (sliderPosition * totalLengthMs).toLong() else currentTimeMs

    // Poll live torrent statistics
    LaunchedEffect(torrentEngine) {
        while (true) {
            downloadSpeedBytes = torrentEngine.downloadSpeedBytes
            downloadProgress = torrentEngine.downloadProgress
            numPeers = torrentEngine.numPeers
            delay(1000)
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(showSeekBarOnly, isSeeking) {
        if (showSeekBarOnly && !isSeeking) { delay(1800); showSeekBarOnly = false }
    }
    LaunchedEffect(showControls, isPlaying, zoom, isSeeking, showMore) {
        if (showControls && isPlaying && !isSeeking && !showMore) {
            delay(3500)
            showControls = false
        }
    }

    val playerHolder = remember(torrentEngine) {
        try {
            val options = VideoPlaybackPolicy.options(com.example.torrentstreamplayer.BuildConfig.DEBUG)
            val vlc = LibVLC(context, options)
            val mp = MediaPlayer(vlc)

            mp.setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.TimeChanged -> {
                        seekUi = seekUi.timeChanged(event.timeChanged)
                        if (seekUi.targetMs == null) currentTimeMs = event.timeChanged
                        if (!isSeeking && seekUi.targetMs == null && totalLengthMs > 0) {
                            sliderPosition = (currentTimeMs.toFloat() / totalLengthMs.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                    MediaPlayer.Event.LengthChanged -> {
                        totalLengthMs = event.lengthChanged
                        val length = event.lengthChanged
                        // The engine sizes its urgent download window from the real bitrate (file size / length).
                        EngineCommandQueue.shared.post { torrentEngine.updatePlaybackDuration(length) }
                    }
                    MediaPlayer.Event.Buffering -> { seekUi = seekUi.buffered(event.buffering) }
                    MediaPlayer.Event.EndReached -> {
                        seekUi = SeekUiState(); isPlaying = false; currentTimeMs = totalLengthMs
                        positionCallback(0, 0); playbackCompleted = true
                    }
                    MediaPlayer.Event.Playing -> {
                        isPlaying = true
                    }
                    MediaPlayer.Event.Paused -> {
                        isPlaying = false
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        isPlaying = false
                        errorMessage = resources.getString(R.string.player_error_playback)
                    }
                }
            }

            PlayerHolder(vlc, mp)
        } catch (t: Throwable) {
            Log.e("VlcPlayerScreen", "Error initializing VLC", t)
            errorMessage = resources.getString(R.string.player_error_init, t.localizedMessage ?: resources.getString(R.string.failure_unknown))
            null
        }
    }

    DisposableEffect(playerHolder, streamServer, lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE && resumeApplied && totalLengthMs > 0)
                positionCallback(currentTimeMs, totalLengthMs)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (resumeApplied && totalLengthMs > 0) positionCallback(currentTimeMs, totalLengthMs)
            if (playerHolder != null) playerHolder.releaseAsync(streamServer)
            else EngineCommandQueue.shared.post { streamServer.stop() } // Player failed to start; only the server is open.
        }
    }

    val holder = playerHolder
    LaunchedEffect(playbackCompleted) {
        if (playbackCompleted && exitGate.consume()) completionCallback(watchTime.watchedMs)
    }
    LaunchedEffect(holder, lifecycle) {
        while (holder != null) {
            watchTime.sample(android.os.SystemClock.elapsedRealtime(), holder.mediaPlayer.time,
                holder.mediaPlayer.isPlaying && !isSeeking && !seekUi.waiting && resumeApplied &&
                    lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))
            delay(1000)
        }
    }
    // Save playback position separately from foreground watch time.
    LaunchedEffect(holder) {
        while (holder != null) {
            delay(5000)
            if (resumeApplied && !isSeeking && !seekUi.waiting && totalLengthMs > 0)
                positionCallback(currentTimeMs, totalLengthMs)
        }
    }
    val error = errorMessage
    fun seekTo(target: Long) {
        if (holder == null || totalLengthMs <= 0) return
        val position = target.coerceIn(0, totalLengthMs)
        seekUi = seekUi.requested(position, totalLengthMs, currentTimeMs)
        val length = totalLengthMs
        // beginSeek must precede VLC's new range request; both run in order on the engine queue.
        EngineCommandQueue.shared.post {
            torrentEngine.beginSeek(position, length)
            holder.mediaPlayer.time = position
        }
        currentTimeMs = position
        sliderPosition = position.toFloat() / totalLengthMs
    }
    LaunchedEffect(holder, initialPositionMs) {
        while (!resumeApplied && holder != null) {
            if (totalLengthMs > 0 && holder.mediaPlayer.isSeekable) {
                resumeApplied = true
                seekTo(com.example.torrentstreamplayer.engine.PlaybackResume.position(initialPositionMs, totalLengthMs))
            } else delay(250)
        }
    }
    val tapSeek by rememberUpdatedState<(androidx.compose.ui.geometry.Offset, Float) -> Unit>({ offset, width ->
        DoubleTapSeek.target(offset.x, width, seekUi.targetMs ?: currentTimeMs, totalLengthMs)?.let { seekTo(it) }
    })
    val toggleControls by rememberUpdatedState { showControls = !showControls; showSeekBarOnly = false }
    val transform by rememberUpdatedState<(androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset, Float) -> Unit>({ focus, pan, factor ->
        zoom = zoom.transformed(factor, pan.x, pan.y, viewport.width.toFloat(), viewport.height.toFloat(), focus.x, focus.y)
    })
    val swipeEnabled by rememberUpdatedState { PlayerOverlayPolicy.canSwipe(holder != null, totalLengthMs, showControls) }
    val panEnabled by rememberUpdatedState { !showControls && zoom.scale > 1f }
    val swipeMove by rememberUpdatedState<(Float) -> Unit>({ distance ->
        swipePreviewMs = SwipeSeek.target(distance, viewport.width.toFloat(), swipeStartMs, totalLengthMs)
        swipePreviewMs?.let { sliderPosition = it.toFloat() / totalLengthMs }
    })
    val swipeStart by rememberUpdatedState {
        if (!showControls) showSeekBarOnly = true
        swipeStartMs = seekUi.targetMs ?: currentTimeMs; swipePreviewMs = swipeStartMs; isSeeking = true
    }
    val swipeEnd by rememberUpdatedState<(Boolean) -> Unit>({ commit ->
        if (!commit) showSeekBarOnly = false
        val target = swipePreviewMs; swipePreviewMs = null; isSeeking = false
        if (commit && target != null) seekTo(target)
        else if (totalLengthMs > 0) sliderPosition = currentTimeMs.toFloat() / totalLengthMs
    })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaybackColors.Video)
            .clipToBounds()
            .onSizeChanged { viewport = it }
            .pointerInput(Unit) {
                detectVideoSwipeGestures({ swipeEnabled() }, { panEnabled() },
                    { swipeStart() }, { swipeMove(it) }, { swipeEnd(it) }, { focus, pan, factor -> transform(focus, pan, factor) })
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { toggleControls() },
                    onDoubleTap = { tapSeek(it, size.width.toFloat()) })
            },
        contentAlignment = Alignment.Center
    ) {
        if (error != null) {
            PlayerErrorState(error, onBack = { exitPlayback() })
        } else if (holder != null) {
            // Video Surface
            AndroidView(
                factory = { ctx ->
                    VLCVideoLayout(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        try {
                            val videoFile = torrentEngine.selectedVideoFile
                            if (videoFile != null && videoFile.exists()) {
                                val local = torrentEngine.localVideo
                                if (local == null && !streamServer.isAlive) streamServer.start()
                                holder.mediaPlayer.attachViews(this, null, false, true)
                                holder.mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
                                val media = Media(holder.libVlc,
                                    local?.let(Uri::fromFile) ?:
                                    Uri.parse("http://127.0.0.1:${streamServer.listeningPort}/stream"))
                                media.addOption(if (local == null) ":network-caching=500" else ":file-caching=300")
                                media.setHWDecoderEnabled(VideoPlaybackPolicy.hardwareDecoding(android.os.Build.HARDWARE), false)
                                holder.mediaPlayer.media = media
                                media.release()
                                holder.mediaPlayer.play()
                            } else {
                                errorMessage = ctx.getString(R.string.player_error_file_missing)
                            }
                        } catch (t: Throwable) {
                            Log.e("VlcPlayerScreen", "Error attaching video layout or starting media", t)
                            errorMessage = ctx.getString(R.string.player_error_start, t.localizedMessage ?: ctx.getString(R.string.failure_unknown))
                        }
                    }
                },
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = zoom.scale; scaleY = zoom.scale
                    translationX = zoom.x; translationY = zoom.y
                }
            )

            val timeLabel = stringResource(R.string.player_time_label, formatTime(displayedTimeMs), formatTime(totalLengthMs))
            if (!showControls && !showSeekBarOnly) {
                PlayerTimePill(timeLabel, Modifier.align(Alignment.BottomEnd))
            }
            if (PlayerOverlayPolicy.seekBarOnly(showControls, showSeekBarOnly)) {
                SwipeSeekBar(sliderPosition, totalLengthMs > 0, Modifier.align(Alignment.BottomCenter),
                    onChange = { sliderPosition = it; isSeeking = true },
                    onFinished = { isSeeking = false; if (totalLengthMs > 0) seekTo((sliderPosition * totalLengthMs).toLong()) },
                    pieces = pieces)
            }
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PlaybackColors.ControlsScrim)
                ) {
                    PlayerTopBar(
                        fileName = fileName,
                        stats = stringResource(R.string.player_stats, formatSpeed(downloadSpeedBytes),
                            downloadProgress.toInt(), numPeers),
                        zoomLabel = String.format(Locale.US, "%.1f×", zoom.scale),
                        landscape = landscape,
                        onBack = { exitPlayback() },
                        onZoomOut = {
                            zoom = zoom.transformed(0.8f, 0f, 0f,
                                viewport.width.toFloat(), viewport.height.toFloat())
                        },
                        onZoomReset = { zoom = VideoZoomState() },
                        onZoomIn = {
                            zoom = zoom.transformed(1.25f, 0f, 0f,
                                viewport.width.toFloat(), viewport.height.toFloat())
                        },
                        onMore = { showMore = true },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    PlayerCenterControls(
                        isPlaying = isPlaying,
                        waitingMessage = if (seekUi.waiting) stringResource(seekUi.waitingMessage) else null,
                        onRewind = {
                            if (totalLengthMs > 0) {
                                val target = (holder.mediaPlayer.time - 10000).coerceAtLeast(0)
                                seekTo(target)
                            }
                        },
                        onTogglePlay = {
                            try {
                                if (holder.mediaPlayer.isPlaying) {
                                    holder.mediaPlayer.pause()
                                    isPlaying = false
                                } else {
                                    holder.mediaPlayer.play()
                                    isPlaying = true
                                }
                            } catch (e: Exception) {
                                Log.e("VlcPlayerScreen", "Error toggling play/pause", e)
                            }
                        },
                        onForward = {
                            if (totalLengthMs > 0) {
                                val target = (holder.mediaPlayer.time + 10000).coerceAtMost(totalLengthMs)
                                seekTo(target)
                            }
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    PlayerBottomPanel(
                        timeLabel = timeLabel,
                        downloadLabel = stringResource(R.string.player_downloaded, downloadProgress.toInt()),
                        position = sliderPosition,
                        pieces = pieces,
                        seekEnabled = true,
                        landscape = landscape,
                        onSeekChange = { pos ->
                            sliderPosition = pos
                            isSeeking = true
                        },
                        onSeekFinished = {
                            isSeeking = false
                            if (totalLengthMs > 0) {
                                val targetTime = (sliderPosition * totalLengthMs).toLong()
                                seekTo(targetTime)
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
            if (showMore) {
                PlayerMoreSheet(
                    onDismiss = { showMore = false },
                    onAddLink = onAddDownload?.let { add -> { showMore = false; add() } },
                    onRestart = {
                        showMore = false; resumeApplied = true; seekTo(0); positionCallback(0, totalLengthMs)
                    }
                )
            }
        }
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    val kb = bytesPerSec / 1024
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.2f MB/s", mb)
    } else {
        "$kb KB/s"
    }
}

internal fun formatTime(timeMs: Long): String {
    if (timeMs <= 0) return "00:00"
    val totalSeconds = timeMs / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
