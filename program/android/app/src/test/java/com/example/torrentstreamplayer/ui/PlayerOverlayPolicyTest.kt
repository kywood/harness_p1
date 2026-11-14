package com.example.torrentstreamplayer.ui
import org.junit.Assert.*
import org.junit.Test
class PlayerOverlayPolicyTest {
    @Test fun hiddenControlsDisableSeeking() { assertFalse(PlayerOverlayPolicy.canSwipe(true, 3600000, false)) }
    @Test fun visibleControlsAllowSeeking() { assertTrue(PlayerOverlayPolicy.canSwipe(true, 3600000, true)) }
    @Test fun unavailableVideoCannotSeek() { assertFalse(PlayerOverlayPolicy.canSwipe(false, 3600000, true)); assertFalse(PlayerOverlayPolicy.canSwipe(true, 0, true)) }
    @Test fun seekBarIsDisabledUntilTheLengthIsKnown() { assertFalse(PlayerOverlayPolicy.seekEnabled(0)); assertTrue(PlayerOverlayPolicy.seekEnabled(1)) }
    @Test fun previewShowsOnlySeekBarWithoutOpeningControls() { assertTrue(PlayerOverlayPolicy.seekBarOnly(false, true)) }
    @Test fun fullControlsAndIdleHiddenStateNeverDuplicateSeekBar() { assertFalse(PlayerOverlayPolicy.seekBarOnly(true, true)); assertFalse(PlayerOverlayPolicy.seekBarOnly(false, false)) }
}
