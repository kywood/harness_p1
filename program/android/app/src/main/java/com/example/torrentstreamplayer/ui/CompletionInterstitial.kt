package com.example.torrentstreamplayer.ui

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import com.example.torrentstreamplayer.engine.EngineFailureLog
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.*
import com.example.torrentstreamplayer.BuildConfig

internal class CompletionInterstitial(private val activity: Activity) {
    private var ad: InterstitialAd? = null
    private var loadedAt = 0L
    private var loading = false
    private var disposed = false
    private val preferences = activity.getSharedPreferences("completion_ads", Activity.MODE_PRIVATE)
    fun preload() {
        if (SystemClock.elapsedRealtime() - loadedAt >= 3_600_000) ad = null
        if (disposed || loading || ad != null) return
        loading = true
        InterstitialAd.load(activity, CompletionAdPolicy.unitId(BuildConfig.DEBUG),
            AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    loading = false
                    if (!disposed) { ad = loaded; loadedAt = SystemClock.elapsedRealtime() }
                }
                override fun onAdFailedToLoad(error: LoadAdError) { loading = false }
            })
    }
    fun showIfReady(watchedMs: Long, canShow: Boolean = true) {
        if (disposed) return
        val completions = CompletionAdPolicy.nextCompletionCount(preferences.getInt("completions", 0))
        preferences.edit().putInt("completions", completions).apply()
        if (!canShow) return
        val ready = ad ?: return
        val last = if (preferences.contains("last_shown")) preferences.getLong("last_shown", 0) else null
        if (disposed || activity.isFinishing || activity.isDestroyed ||
            !CompletionAdPolicy.eligible(watchedMs, System.currentTimeMillis(), last, completions)) return
        ad = null
        if (SystemClock.elapsedRealtime() - loadedAt >= 3_600_000) return
        ready.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                preferences.edit().putLong("last_shown", System.currentTimeMillis())
                    .putInt("completions", 0).apply()
            }
        }
        runCatching { ready.show(activity) }
            .onFailure { Log.w("CompletionAd", "Display failed: ${EngineFailureLog.describe(it)}") }
    }
    fun dispose() { disposed = true; ad = null }
}
