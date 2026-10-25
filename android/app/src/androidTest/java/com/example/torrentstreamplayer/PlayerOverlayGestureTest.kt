package com.example.torrentstreamplayer
import android.os.SystemClock
import android.view.MotionEvent
import android.view.InputDevice
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.dp
import com.example.torrentstreamplayer.engine.*
import com.example.torrentstreamplayer.ui.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.*
class PlayerOverlayGestureTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun await(check: () -> Boolean) {
        val limit = SystemClock.uptimeMillis() + 5000
        while (!check()) { check(SystemClock.uptimeMillis() < limit) { "UI did not update" }; Thread.sleep(30) }
    }
    private fun send(down: Long, action: Int, first: Offset, second: Offset? = null) {
        val points = listOfNotNull(first, second)
        val properties = Array(points.size) { index ->
            MotionEvent.PointerProperties().apply { id = index; toolType = MotionEvent.TOOL_TYPE_FINGER }
        }
        val coords = Array(points.size) { index ->
            MotionEvent.PointerCoords().apply { x = points[index].x; y = points[index].y; pressure = 1f; size = 1f }
        }
        val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, points.size,
            properties, coords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
        try { instrumentation.sendPointerSync(event) } finally { event.recycle() }
        Thread.sleep(30)
    }
    @Test fun hiddenSwipeShowsOnlyTimelineAndPinchCancelsSeek() {
        val commits = AtomicInteger(); val cancellations = AtomicInteger(); val transforms = AtomicInteger()
        val bounds = AtomicReference<Rect>(); val previewVisible = AtomicBoolean()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                var preview by remember { mutableStateOf(false) }
                var position by remember { mutableFloatStateOf(0.25f) }
                Box(Modifier.fillMaxSize().onGloballyPositioned { bounds.set(it.boundsInWindow()) }.pointerInput(Unit) {
                    detectVideoSwipeGestures({ PlayerOverlayPolicy.canSwipe(true, 3600000) }, { false },
                        { preview = true }, { position = (0.25f + it / size.width).coerceIn(0f, 1f) },
                        { if (it) commits.incrementAndGet() else { cancellations.incrementAndGet(); preview = false } },
                        { _, _, _ -> transforms.incrementAndGet() })
                }.pointerInput(Unit) { detectTapGestures(onTap = {}, onDoubleTap = {}) }) {
                    if (PlayerOverlayPolicy.seekBarOnly(false, preview)) SwipeSeekBar(position, true,
                        Modifier.align(androidx.compose.ui.Alignment.BottomCenter), {}, {})
                }
                SideEffect { previewVisible.set(PlayerOverlayPolicy.seekBarOnly(false, preview)) }
            } }
            await { bounds.get() != null }
            val center = bounds.get().center
            assertFalse(previewVisible.get())
            val first = SystemClock.uptimeMillis()
            send(first, MotionEvent.ACTION_DOWN, center)
            send(first, MotionEvent.ACTION_MOVE, center + Offset(40f, 0f))
            send(first, MotionEvent.ACTION_MOVE, center + Offset(100f, 0f))
            await { previewVisible.get() }
            send(first, MotionEvent.ACTION_UP, center + Offset(100f, 0f))
            await { commits.get() == 1 }
            assertEquals(0, transforms.get())
            val second = SystemClock.uptimeMillis()
            val moved = center + Offset(100f, 0f)
            send(second, MotionEvent.ACTION_DOWN, center)
            send(second, MotionEvent.ACTION_MOVE, moved)
            send(second, MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                moved, center - Offset(60f, 0f))
            send(second, MotionEvent.ACTION_MOVE, moved, center - Offset(100f, 0f))
            send(second, MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                moved, center - Offset(100f, 0f))
            send(second, MotionEvent.ACTION_UP, moved)
            await { cancellations.get() == 1 && transforms.get() > 0 }
            assertEquals(1, commits.get())
            await { !previewVisible.get() }
        }
    }
    @Test fun inlinePieceInformationAndLegendOccupyOneCompactRow() {
        val heightDp = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                Box(Modifier.fillMaxWidth()) {
                    PieceMap(PieceMapSnapshot.capture(0, 99, 4) { it < 50 },
                        Modifier.onGloballyPositioned { heightDp.set((it.size.height / activity.resources.displayMetrics.density).toInt()) },
                        inlineInfo = true)
                }
            } }
            await { heightDp.get() > 0 }
            assertTrue("Inline map must contain only a bar and one information row: " + heightDp.get(), heightDp.get() in 24..48)
        }
    }
}
