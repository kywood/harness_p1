package com.example.torrentstreamplayer.ui

import com.example.torrentstreamplayer.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalFooterPolicyTest {
    @Test fun consentRegionShowsAllLinksInFixedOrder() {
        assertEquals(
            listOf(LegalLink.USAGE_GUIDE, LegalLink.PRIVACY_POLICY, LegalLink.OPEN_SOURCE_LICENSES,
                LegalLink.AD_PRIVACY),
            LegalFooterPolicy.links(consentOptionsAvailable = true)
        )
    }

    @Test fun adPrivacyIsHiddenWhenNoConsentOptions() {
        val links = LegalFooterPolicy.links(consentOptionsAvailable = false)
        assertFalse(LegalLink.AD_PRIVACY in links)
        assertEquals(LegalLink.entries.size - 1, links.size)
    }

    @Test fun footerHasAtMostFourLinks() {
        assertTrue(LegalLink.entries.size <= 4)
    }

    @Test fun onlyPrivacyPolicyLeavesTheApp() {
        assertEquals(listOf(LegalLink.PRIVACY_POLICY), LegalLink.entries.filter { it.external })
        assertEquals(R.string.url_privacy_policy, LegalLink.PRIVACY_POLICY.url)
    }

    @Test fun usageGuideAndPrivacyPolicyAreAlwaysShown() {
        assertTrue(LegalLink.USAGE_GUIDE in LegalFooterPolicy.links(consentOptionsAvailable = false))
        assertTrue(LegalLink.PRIVACY_POLICY in LegalFooterPolicy.links(consentOptionsAvailable = false))
        assertTrue(LegalLink.OPEN_SOURCE_LICENSES in LegalFooterPolicy.links(consentOptionsAvailable = false))
    }

    @Test fun blankVersionHidesTheLine() {
        assertNull(LegalFooterPolicy.versionLabel(null))
        assertNull(LegalFooterPolicy.versionLabel(""))
        assertNull(LegalFooterPolicy.versionLabel("  "))
        assertEquals("v.2026.11.12.003", LegalFooterPolicy.versionLabel("v.2026.11.12.003"))
    }

    @Test fun actionsConnectEachLinkToItsOwnCallback() {
        val calls = mutableListOf<String>()
        val actions = LegalLinkActions(openUsageGuide = { calls += "guide" }, openExternal = { calls += "web:$it" },
            openLicenses = { calls += "licenses" }, privacyOptions = { calls += "ads" })
        LegalLink.entries.forEach { actions.actionFor(it, "https://example.test")!!.invoke() }
        assertEquals(listOf("guide", "web:https://example.test", "licenses", "ads"), calls)
        assertEquals(LegalFooterPolicy.links(consentOptionsAvailable = true), actions.links)
    }

    @Test fun missingConsentOptionsOrUrlLeaveNoAction() {
        val actions = LegalLinkActions({}, {}, {}, privacyOptions = null)
        assertNull(actions.actionFor(LegalLink.AD_PRIVACY, null))
        assertNull(actions.actionFor(LegalLink.PRIVACY_POLICY, null))
        assertFalse(LegalLink.AD_PRIVACY in actions.links)
    }
}
