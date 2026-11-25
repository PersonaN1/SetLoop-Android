package io.github.personan1.setloop.ui.history

import android.content.res.Resources
import android.text.format.DateFormat
import io.github.personan1.setloop.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Date, time and total-time texts of the history screens, all following the device language and settings. */
object HistoryFormat {
    private const val DAY_HEADER = "EEEEdMMMM"
    private const val FULL_DATE_TIME = "EEEEdMMMMyjm"
    private const val SECONDS_IN_MINUTE = 60
    private const val SECONDS_IN_HOUR = 3600

    /** "Wednesday, November 25" */
    fun dayHeader(day: LocalDate, locale: Locale): String =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, DAY_HEADER), locale).format(day)

    /** "12:30 PM" or "12:30", by the locale. */
    fun time(epochMillis: Long, zone: ZoneId, locale: Locale): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            .format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    /** "Wednesday, November 25, 2026 at 12:30 PM" */
    fun fullDateTime(epochMillis: Long, zone: ZoneId, locale: Locale): String =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, FULL_DATE_TIME), locale)
            .format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    /**
     * Total time of the week: "1 h 20 min", "45 min". Under a minute it shows seconds, so a short
     * workout never reads as "0 min" (iOS `longDuration`).
     */
    fun totalTime(resources: Resources, seconds: Int): String {
        val total = seconds.coerceAtLeast(0)
        val hours = total / SECONDS_IN_HOUR
        val minutes = total / SECONDS_IN_MINUTE % SECONDS_IN_MINUTE
        return when {
            total < SECONDS_IN_MINUTE -> resources.getString(R.string.history_total_seconds, total)
            hours > 0 && minutes > 0 -> resources.getString(R.string.history_total_hours_minutes, hours, minutes)
            hours > 0 -> resources.getString(R.string.history_total_hours, hours)
            else -> resources.getString(R.string.history_total_minutes, minutes)
        }
    }
}
