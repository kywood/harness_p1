package com.example.torrentstreamplayer.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Decisions about ad consent that do not depend on Android. */
internal object AdConsentPolicy {
    /** The privacy options entry point must be shown only where UMP says it is required. */
    fun showsPrivacyEntry(status: ConsentInformation.PrivacyOptionsRequirementStatus): Boolean =
        status == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
}

/**
 * Gathers the user's ad consent with Google UMP before any ad is requested.
 * Ads code must check [canRequestAds] before initializing the Mobile Ads SDK or loading an ad.
 */
internal class AdConsent private constructor(private val information: ConsentInformation) {
    private val mutableCanRequestAds = MutableStateFlow(information.canRequestAds())
    val canRequestAds: StateFlow<Boolean> = mutableCanRequestAds.asStateFlow()
    private val mutablePrivacyEntry = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = mutablePrivacyEntry.asStateFlow()

    /** Call on every app launch: refreshes consent and shows the consent form when one is required. */
    fun gather(activity: Activity) {
        information.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error -> report(error); refresh() }
        }, { error -> report(error); refresh() })
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error -> report(error); refresh() }
    }

    private fun refresh() {
        mutableCanRequestAds.value = information.canRequestAds()
        mutablePrivacyEntry.value = AdConsentPolicy.showsPrivacyEntry(information.privacyOptionsRequirementStatus)
    }

    private fun report(error: FormError?) {
        if (error != null) Log.w(TAG, "Consent: ${error.errorCode}")
    }

    companion object {
        private const val TAG = "AdConsent"
        @Volatile private var instance: AdConsent? = null

        fun get(context: Context): AdConsent = instance ?: synchronized(this) {
            instance ?: AdConsent(UserMessagingPlatform.getConsentInformation(context.applicationContext))
                .also { instance = it }
        }
    }
}
