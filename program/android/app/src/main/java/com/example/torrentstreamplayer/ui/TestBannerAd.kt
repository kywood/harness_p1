package com.example.torrentstreamplayer.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object TestAdsRuntime {
    private val lock = kotlinx.coroutines.sync.Mutex()
    private var initialized = false
    suspend fun initialize(context: android.content.Context) {
        lock.lock()
        try {
            if (!initialized) withContext(Dispatchers.IO) {
                MobileAds.initialize(context.applicationContext) {}
                initialized = true
            }
        } finally { lock.unlock() }
    }
}

@Composable
internal fun TestBannerAd() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var ready by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<AdView?>(null) }
    LaunchedEffect(Unit) {
        ready = runCatching { TestAdsRuntime.initialize(context) }.isSuccess
    }
    DisposableEffect(banner, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> banner?.resume()
                Lifecycle.Event.ON_PAUSE -> banner?.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val width = maxWidth.value.toInt().coerceAtLeast(1)
        if (ready) key(width) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                factory = {
                    AdView(it).apply {
                        adUnitId = TestAdPolicy.bannerUnitId(com.example.torrentstreamplayer.BuildConfig.DEBUG)
                        setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(it, width))
                        banner = this
                        loadAd(AdRequest.Builder().build())
                    }
                },
                onRelease = { it.destroy(); if (banner === it) banner = null }
            )
        }
    }
}
