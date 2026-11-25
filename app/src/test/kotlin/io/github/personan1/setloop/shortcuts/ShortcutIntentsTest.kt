package io.github.personan1.setloop.shortcuts

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShortcutIntentsTest {
    private val id = "6f1c2d3e-aaaa-4bbb-8ccc-1234567890ab"

    @Test
    fun startTimerIntentRoundTrips() {
        val intent = ShortcutIntents.startTimer(ApplicationProvider.getApplicationContext(), id)
        assertEquals(ShortcutAction.StartTimer(id), ShortcutIntents.parse(intent))
    }

    @Test
    fun pauseAndResumeNeedNoExtras() {
        assertEquals(ShortcutAction.Pause, ShortcutIntents.parse(Intent(ShortcutIntents.ACTION_PAUSE)))
        assertEquals(ShortcutAction.Resume, ShortcutIntents.parse(Intent(ShortcutIntents.ACTION_RESUME)))
    }

    @Test
    fun foreignAndEmptyIntentsAreIgnored() {
        assertNull(ShortcutIntents.parse(null as Intent?))
        assertNull(ShortcutIntents.parse(Intent(Intent.ACTION_MAIN)))
        assertNull(ShortcutIntents.parse(Intent()))
    }

    @Test
    fun startWithoutValidIdIsIgnored() {
        assertNull(ShortcutIntents.parse(ShortcutIntents.ACTION_START_TIMER, null))
        assertNull(ShortcutIntents.parse(ShortcutIntents.ACTION_START_TIMER, ""))
        assertNull(ShortcutIntents.parse(ShortcutIntents.ACTION_START_TIMER, "   "))
        assertNull(ShortcutIntents.parse(ShortcutIntents.ACTION_START_TIMER, "../../etc/passwd"))
        assertNull(ShortcutIntents.parse(ShortcutIntents.ACTION_START_TIMER, "a".repeat(65)))
    }

    @Test
    fun dynamicSlotsLeaveRoomForStaticShortcuts() {
        assertEquals(2, TimerShortcuts.dynamicSlots(4))
        assertEquals(0, TimerShortcuts.dynamicSlots(2))
        assertEquals(0, TimerShortcuts.dynamicSlots(1))
        assertEquals(8, TimerShortcuts.dynamicSlots(10))
    }

    @Test
    fun shortcutIdIsStablePerTimer() {
        assertEquals("start-$id", TimerShortcuts.shortcutId(id))
    }
}
