package com.example.torrentstreamplayer.meta

/** Ad timing and count rules. Register new ad values here instead of writing literals in the policies. */
internal object AdSpec {
    /** A completion interstitial may follow playback watched at least this long. */
    const val COMPLETION_MIN_WATCH_MS = 300_000L

    /** Minimum gap between two completion interstitials. */
    const val COMPLETION_COOLDOWN_MS = 600_000L

    /** Completed playbacks that also make a completion interstitial eligible. */
    const val COMPLETION_COUNT = 5
}

/**
 * AdMob ad units. Debug builds use Google's test units, release builds the real ones.
 * Register a new ad placement here; callers ask [id] with `BuildConfig.DEBUG` through their policy object.
 */
internal enum class AdUnit(val debugId: String, val releaseId: String) {
    /** Banner on the main and playlist screens. */
    BANNER("ca-app-pub-3940256099942544/9214589741", "ca-app-pub-2418522550325553/6125182266"),

    /** Interstitial shown after playback ends or completes. */
    INTERSTITIAL("ca-app-pub-3940256099942544/1033173712", "ca-app-pub-2418522550325553/6999280238");

    fun id(debug: Boolean): String = if (debug) debugId else releaseId
}
