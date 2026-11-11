package com.example.torrentstreamplayer.ui

import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AdConsentPolicyTest {
    @Test fun privacyEntryIsShownOnlyWhereUmpRequiresIt() {
        val shown = PrivacyOptionsRequirementStatus.entries.associateWith(AdConsentPolicy::showsPrivacyEntry)
        assertEquals(mapOf(PrivacyOptionsRequirementStatus.UNKNOWN to false,
            PrivacyOptionsRequirementStatus.NOT_REQUIRED to false,
            PrivacyOptionsRequirementStatus.REQUIRED to true), shown)
    }
}
