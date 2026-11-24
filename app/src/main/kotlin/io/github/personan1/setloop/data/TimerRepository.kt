package io.github.personan1.setloop.data

import androidx.room.withTransaction
import io.github.personan1.setloop.core.Limits
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.core.TimerNaming
import io.github.personan1.setloop.core.cappedName
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow

sealed interface TimerAddResult {
    data class Added(val timer: SavedTimerEntity) : TimerAddResult

    /** Adding or duplicating is refused at the timer limit (SPEC D9). */
    data object LimitReached : TimerAddResult

    /** Only from [TimerRepository.duplicate]: the timer was deleted meanwhile. */
    data object NotFound : TimerAddResult
}

/** [maxTimers] is a parameter so tests can use a small limit. */
class TimerRepository(
    private val database: SetLoopDatabase,
    private val clock: Clock = Clock.systemUTC(),
    private val maxTimers: Int = Limits.maxTimers,
) {
    private val dao = database.timerDao()

    /** Ordered by `sortIndex`. */
    fun observeTimers(): Flow<List<SavedTimerEntity>> = dao.observeAll()

    suspend fun get(id: String): SavedTimerEntity? = dao.get(id)

    /** Appends to the end of the list. An empty [name] becomes the numbered [defaultName], as on iOS. */
    suspend fun add(name: String, definition: TimerDefinition, defaultName: String): TimerAddResult =
        database.withTransaction {
            if (dao.count() >= maxTimers) return@withTransaction TimerAddResult.LimitReached
            val finalName = resolveName(name, defaultName, dao.names())
            val sortIndex = (dao.maxSortIndex() ?: -1) + 1
            insertNew(finalName, definition, sortIndex)
        }

    /** Saves the edited timer. Returns null when it no longer exists. */
    suspend fun update(
        id: String,
        name: String,
        definition: TimerDefinition,
        defaultName: String,
    ): SavedTimerEntity? = database.withTransaction {
        val current = dao.get(id) ?: return@withTransaction null
        val finalName = resolveName(name, defaultName, dao.namesExcept(id))
        current.withDefinition(finalName, definition, clock.millis()).also { dao.update(it) }
    }

    /** Inserts a copy right after the original (SPEC D9). */
    suspend fun duplicate(id: String): TimerAddResult = database.withTransaction {
        val original = dao.get(id) ?: return@withTransaction TimerAddResult.NotFound
        if (dao.count() >= maxTimers) return@withTransaction TimerAddResult.LimitReached
        dao.shiftAfter(original.sortIndex)
        insertNew(
            name = TimerNaming.unique(original.name, dao.names()),
            definition = original.definition,
            sortIndex = original.sortIndex + 1,
        )
    }

    suspend fun deleteMany(ids: Collection<String>) = database.withTransaction {
        ids.chunked(DELETE_CHUNK).forEach { dao.deleteByIds(it) }
    }

    /**
     * Gives the timers the positions of [orderedIds]. Timers missing from the list (added meanwhile)
     * keep their relative order after the listed ones.
     */
    suspend fun reorder(orderedIds: List<String>) = database.withTransaction {
        val current = dao.getAll()
        val currentIndex = current.associate { it.id to it.sortIndex }
        val listed = orderedIds.filter { it in currentIndex }.distinct()
        val unlisted = current.map { it.id }.filter { it !in listed }
        (listed + unlisted).forEachIndexed { index, id ->
            if (currentIndex.getValue(id) != index) dao.setSortIndex(id, index)
        }
    }

    /**
     * Renames default names to the current language in names and steps of every timer (iOS
     * `DefaultNames.relocalize`). A changed definition counts as an edit and bumps `updatedAt`.
     */
    suspend fun relocalize(names: DefaultNames) = database.withTransaction {
        for (timer in dao.getAll()) {
            val name = names.localize(timer.name)
            val definition = timer.definition
            val localized = names.localize(definition)
            val updated = when {
                localized !== definition -> timer.withDefinition(name, localized, clock.millis())
                name != timer.name -> timer.copy(name = name)
                else -> continue
            }
            dao.update(updated)
        }
    }

    private suspend fun insertNew(name: String, definition: TimerDefinition, sortIndex: Int): TimerAddResult.Added {
        val timer = SavedTimerEntity.create(UUID.randomUUID().toString(), name, definition, sortIndex, clock.millis())
        dao.insert(timer)
        return TimerAddResult.Added(timer)
    }

    private fun resolveName(name: String, defaultName: String, others: List<String>): String {
        val trimmed = name.trim()
        return if (trimmed.isEmpty()) TimerNaming.unique(defaultName, others) else trimmed.cappedName
    }

    private companion object {
        /** Stays under SQLite's 999 bound-variable limit on older Android versions. */
        const val DELETE_CHUNK = 500
    }
}
