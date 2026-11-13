package com.example.torrentstreamplayer.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSaveNoticeTest {
    @Test fun failureShowsOnceUntilConsumed() {
        val notice = SettingsSaveNotice()
        assertFalse(notice.failed.value)
        notice.report()
        assertTrue(notice.failed.value)
        notice.consume()
        assertFalse(notice.failed.value)
    }

    @Test fun failureNotShownBeforeLeavingIsDroppedWhenSettingsOpensAgain() {
        val notice = SettingsSaveNotice()
        notice.report()
        notice.clear()
        assertFalse(notice.failed.value)
    }
}
