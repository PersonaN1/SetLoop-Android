package io.github.personan1.setloop.data

import androidx.room.withTransaction
import io.github.personan1.setloop.core.Limits
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.core.WorkoutSummary
import io.github.personan1.setloop.core.cappedName
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Records of one calendar day, newest first. */
data class HistoryDay(val day: LocalDate, val records: List<WorkoutRecordEntity>)

/** Workouts count and active time in seconds. */
typealias WeekMetrics = RecordTotals

/**
 * [clock] and [locale] are read on every call, so a changed time zone or language applies at once.
 * [maxRecords] is a parameter so tests can use a small limit.
 */
class HistoryRepository(
    private val database: SetLoopDatabase,
    private val clock: () -> Clock = Clock::systemDefaultZone,
    private val locale: () -> Locale = Locale::getDefault,
    private val maxRecords: Int = Limits.maxHistoryRecords,
) {
    private val dao = database.recordDao()

    /** Newest first. */
    fun observeRecords(): Flow<List<WorkoutRecordEntity>> = dao.observeAll()

    /** Records grouped by local day, days and records newest first. */
    fun observeDays(): Flow<List<HistoryDay>> = dao.observeAll().map { records ->
        val zone = clock().zone
        records
            .groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .map { (day, dayRecords) -> HistoryDay(day, dayRecords) }
            .sortedByDescending { it.day }
    }

    suspend fun get(id: String): WorkoutRecordEntity? = dao.get(id)

    /**
     * Saves a finished workout. At the limit the oldest records are removed in the same transaction
     * (SPEC D10). A blank [name] falls back to [defaultName], the timer name.
     */
    suspend fun save(
        name: String,
        note: String,
        startedAt: Long,
        summary: WorkoutSummary,
        definition: TimerDefinition,
        defaultName: String,
    ): WorkoutRecordEntity = database.withTransaction {
        val overflow = dao.count() - maxRecords + 1
        if (overflow > 0) dao.deleteOldest(overflow)
        val record = WorkoutRecordEntity.create(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { defaultName }.cappedName,
            note = note.trim(),
            startedAt = startedAt,
            summary = summary,
            definition = definition,
        )
        dao.insert(record)
        record
    }

    suspend fun deleteMany(ids: Collection<String>) = database.withTransaction {
        ids.chunked(DELETE_CHUNK).forEach { dao.deleteByIds(it) }
    }

    /**
     * Totals of the current calendar week (first day of the week per [locale]). The week is fixed when
     * collection starts; collect again to pick up a new week.
     */
    fun observeWeekMetrics(): Flow<WeekMetrics> = flow {
        val now = clock()
        val firstDay = LocalDate.now(now).with(WeekFields.of(locale()).dayOfWeek(), 1)
        val from = firstDay.atStartOfDay(now.zone).toInstant().toEpochMilli()
        val to = firstDay.plusDays(DAYS_IN_WEEK).atStartOfDay(now.zone).toInstant().toEpochMilli()
        emitAll(dao.observeTotals(from, to))
    }

    private companion object {
        const val DELETE_CHUNK = 500
        const val DAYS_IN_WEEK = 7L
    }
}
