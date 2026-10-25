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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.torrentstreamplayer.engine.TorrentEngine
import com.example.torrentstreamplayer.engine.TorrentStreamServer
import com.example.torrentstreamplayer.ui.theme.PlaybackColors
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.Locale

private class PlayerHolder(val libVlc: LibVLC, val mediaPlayer: MediaPlayer)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VlcPlayerScreen(
    fileName: String,
    torrentEngine: TorrentEngine,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
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

    var showControls by remember { mutableStateOf(true) }
    var showSeekBarOnly by remember { mutableStateOf(false) }
    var zoom by remember { mutableStateOf(VideoZoomState()) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    // Download Stats State
    var downloadSpeedBytes by remember { mutableLongStateOf(0L) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var numPeers by remember { mutableIntStateOf(0) }

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
    LaunchedEffect(showControls, isPlaying, zoom, isSeeking) {
        if (showControls && isPlaying && !isSeeking) {
            delay(3500)
            showControls = false
        }
    }

    val playerHolder = remember(torrentEngine) {
        try {
            val options = VideoPlaybackPolicy.options
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
                    }
                    MediaPlayer.Event.Buffering -> { seekUi = seekUi.buffered(event.buffering) }
                    MediaPlayer.Event.EndReached -> { seekUi = SeekUiState(); isPlaying = false }
                    MediaPlayer.Event.Playing -> {
                        isPlaying = true
                    }
                    MediaPlayer.Event.Paused -> {
                        isPlaying = false
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        isPlaying = false
                        errorMessage = "Playback failed. Check peers and try streaming again."
                    }
                }
            }

            PlayerHolder(vlc, mp)
        } catch (t: Throwable) {
            Log.e("VlcPlayerScreen", "Error initializing VLC", t)
            errorMessage = "VLC Player Error: ${t.localizedMessage ?: "Failed to initialize native VLC player"}"
            null
        }
    }

    DisposableEffect(playerHolder, streamServer) {
        onDispose {
            streamServer.stop()
            try {
                playerHolder?.mediaPlayer?.stop()
                playerHolder?.mediaPlayer?.detachViews()
                playerHolder?.mediaPlayer?.release()
                playerHolder?.libVlc?.release()
            } catch (e: Exception) {
                Log.e("VlcPlayerScreen", "Error releasing VLC player", e)
            }
        }
    }

    val holder = playerHolder
    val error = errorMessage
    fun seekTo(target: Long) {
        if (holder == null || totalLengthMs <= 0) return
        val position = target.coerceIn(0, totalLengthMs)
        seekUi = seekUi.requested(position, totalLengthMs, currentTimeMs)
        torrentEngine.beginSeek(position, totalLengthMs)
        holder.mediaPlayer.time = position
        currentTimeMs = position
        sliderPosition = position.toFloat() / totalLengthMs
    }
    val tapSeek by rememberUpdatedState<(androidx.compose.ui.geometry.Offset, Float) -> Unit>({ offset, width ->
        DoubleTapSeek.target(offset.x, width, seekUi.targetMs ?: currentTimeMs, totalLengthMs)?.let { seekTo(it) }
    })
    val toggleControls by rememberUpdatedState { showControls = !showControls; showSeekBarOnly = false }
    val transform by rememberUpdatedState<(androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset, Float) -> Unit>({ focus, pan, factor ->
        zoom = zoom.transformed(factor, pan.x, pan.y, viewport.width.toFloat(), viewport.height.toFloat(), focus.x, focus.y)
    })
    val swipeEnabled by rememberUpdatedState { PlayerOverlayPolicy.canSwipe(holder != null, totalLengthMs) }
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
            .background(Color.Black)
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = error,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackClicked) {
                    Text("Go Back")
                }
            }
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
                                errorMessage = "Video file not found on disk."
                            }
                        } catch (t: Throwable) {
                            Log.e("VlcPlayerScreen", "Error attaching video layout or starting media", t)
                            errorMessage = "VLC Playback Error: ${t.localizedMessage}"
                        }
                    }
                },
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = zoom.scale; scaleY = zoom.scale
                    translationX = zoom.x; translationY = zoom.y
                }
            )

            if (seekUi.waiting && showControls) {
                Column(Modifier.align(Alignment.TopCenter).padding(top = 140.dp).background(PlaybackColors.Panel)
                    .padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PlaybackColors.Accent)
                    Spacer(Modifier.height(12.dp))
                    Text(seekUi.waitingMessage, color = Color.White)
                }
            }
            // Animated Controls Overlay
            if (PlayerOverlayPolicy.seekBarOnly(showControls, showSeekBarOnly)) {
                SwipeSeekBar(sliderPosition, totalLengthMs > 0, Modifier.align(Alignment.BottomCenter),
                    onChange = { sliderPosition = it; isSeeking = true },
                    onFinished = { isSeeking = false; if (totalLengthMs > 0) seekTo((sliderPosition * totalLengthMs).toLong()) })
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
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    // Top Bar (Title + Torrent Speed Pill)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = if (landscape) 2.dp else 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = onBackClicked) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = fileName,
                                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            VideoOrientationButton()
                            if (landscape) Text("↓ ${formatSpeed(downloadSpeedBytes)} · ${downloadProgress.toInt()}% · ${numPeers}p",
                                color = PlaybackColors.Accent, fontSize = 12.sp, maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp))
                            TextButton(onClick = {
                                zoom = zoom.transformed(0.8f, 0f, 0f,
                                    viewport.width.toFloat(), viewport.height.toFloat())
                            }, contentPadding = PaddingValues(4.dp), modifier = Modifier.width(40.dp)) {
                                Text("−", color = Color.White)
                            }
                            TextButton(onClick = { zoom = VideoZoomState() },
                                contentPadding = PaddingValues(4.dp), modifier = Modifier.width(56.dp)) {
                                Text(String.format(Locale.US, "%.1f×", zoom.scale), color = Color.White)
                            }
                            TextButton(onClick = {
                                zoom = zoom.transformed(1.25f, 0f, 0f,
                                    viewport.width.toFloat(), viewport.height.toFloat())
                            }, contentPadding = PaddingValues(4.dp), modifier = Modifier.width(40.dp)) {
                                Text("+", color = Color.White)
                            }
                        }

                        // Download Speed & Peers Pill
                        if (!landscape) Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .padding(start = 12.dp, top = 4.dp)
                        ) {
                            Text(
                                text = "⬇ ${formatSpeed(downloadSpeedBytes)} (${downloadProgress.toInt()}%) • ${numPeers}p",
                                color = PlaybackColors.Accent,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Center Rewind / Play-Pause / Fast-Forward
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind -10s
                        IconButton(
                            onClick = {
                                if (totalLengthMs > 0) {
                                    val target = (holder.mediaPlayer.time - 10000).coerceAtLeast(0)
                                    seekTo(target)

                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "-10s",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Play / Pause Toggle
                        IconButton(
                            onClick = {
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
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.35f))
                        ) {
                            if (isPlaying) {
                                Text(
                                    text = "❚❚",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        // Fast-Forward +10s
                        IconButton(
                            onClick = {
                                if (totalLengthMs > 0) {
                                    val target = (holder.mediaPlayer.time + 10000).coerceAtMost(totalLengthMs)
                                    seekTo(target)

                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "+10s",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Bottom Seekbar & Time Labels
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(PlaybackColors.Panel)
                            .padding(horizontal = 24.dp, vertical = if (landscape) 6.dp else 16.dp)
                    ) {
                        if (!landscape) Text("재생 위치 · 시간 기준", color = PlaybackColors.Download,
                            style = MaterialTheme.typography.labelSmall)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (landscape) 40.dp else 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Playback Position Slider
                            Slider(
                                value = sliderPosition,
                                onValueChange = { pos ->
                                    sliderPosition = pos
                                    isSeeking = true
                                },
                                onValueChangeFinished = {
                                    isSeeking = false
                                    if (totalLengthMs > 0) {
                                        val targetTime = (sliderPosition * totalLengthMs).toLong()
                                        seekTo(targetTime)

                                    }
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = PlaybackColors.Accent,
                                    activeTrackColor = PlaybackColors.Accent,
                                    inactiveTrackColor = PlaybackColors.Track
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        PieceMap(pieces, Modifier.fillMaxWidth().padding(bottom = if (landscape) 4.dp else 8.dp),
                            Color.White, inlineInfo = landscape)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "재생 ${formatTime(swipePreviewMs ?: currentTimeMs)}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${formatTime(totalLengthMs)} · 다운로드 ${downloadProgress.toInt()}%",
                                color = PlaybackColors.Download,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
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

private fun formatTime(timeMs: Long): String {
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
