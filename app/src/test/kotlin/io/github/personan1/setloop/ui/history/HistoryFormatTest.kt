package io.github.personan1.setloop.ui.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.personan1.setloop.data.TEST_SDK
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK], qualifiers = "en")
class HistoryFormatTest {
    private val resources get() = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `week time is hours and minutes, minutes, or seconds under a minute`() {
        assertEquals("30 sec", HistoryFormat.totalTime(resources, 30))
        assertEquals("0 sec", HistoryFormat.totalTime(resources, 0))
        assertEquals("1 min", HistoryFormat.totalTime(resources, 60))
        assertEquals("45 min", HistoryFormat.totalTime(resources, 45 * 60 + 59))
        assertEquals("1 h", HistoryFormat.totalTime(resources, 3600))
        assertEquals("1 h 20 min", HistoryFormat.totalTime(resources, 3600 + 20 * 60 + 5))
        assertEquals("25 h 1 min", HistoryFormat.totalTime(resources, 25 * 3600 + 60))
    }

    @Test
    fun `a negative total reads as zero`() {
        assertEquals("0 sec", HistoryFormat.totalTime(resources, -5))
    }

    @Test
    fun `day header names the weekday, day and month in the locale`() {
        val day = LocalDate.of(2026, 11, 25)

        assertEquals("Wednesday, November 25", HistoryFormat.dayHeader(day, Locale.US))
        assertEquals("среда, 25 ноября", HistoryFormat.dayHeader(day, Locale.forLanguageTag("ru")))
    }

    @Test
    fun `start time follows the locale`() {
        val noon = 1_795_609_800_000L // 2026-11-25T12:30:00Z

        assertEquals("12:30 PM", HistoryFormat.time(noon, ZoneOffset.UTC, Locale.US).replace('\u202F', ' '))
        assertEquals("12:30", HistoryFormat.time(noon, ZoneOffset.UTC, Locale.forLanguageTag("ru")))
    }
}
