package io.github.personan1.setloop.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedTimerDao {
    @Query("SELECT * FROM saved_timer ORDER BY sortIndex, createdAt")
    fun observeAll(): Flow<List<SavedTimerEntity>>

    @Query("SELECT * FROM saved_timer ORDER BY sortIndex, createdAt")
    suspend fun getAll(): List<SavedTimerEntity>

    @Query("SELECT * FROM saved_timer WHERE id = :id")
    suspend fun get(id: String): SavedTimerEntity?

    @Query("SELECT COUNT(*) FROM saved_timer")
    suspend fun count(): Int

    @Query("SELECT MAX(sortIndex) FROM saved_timer")
    suspend fun maxSortIndex(): Int?

    @Query("SELECT name FROM saved_timer")
    suspend fun names(): List<String>

    @Query("SELECT name FROM saved_timer WHERE id != :id")
    suspend fun namesExcept(id: String): List<String>

    @Insert
    suspend fun insert(timer: SavedTimerEntity)

    @Update
    suspend fun update(timer: SavedTimerEntity)

    @Query("UPDATE saved_timer SET sortIndex = sortIndex + 1 WHERE sortIndex > :sortIndex")
    suspend fun shiftAfter(sortIndex: Int)

    @Query("UPDATE saved_timer SET sortIndex = :sortIndex WHERE id = :id")
    suspend fun setSortIndex(id: String, sortIndex: Int)

    @Query("DELETE FROM saved_timer WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}

/** Totals of the records in a time range. */
data class RecordTotals(val count: Int, val totalDuration: Int)

@Dao
interface WorkoutRecordDao {
    @Query("SELECT * FROM workout_record ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutRecordEntity>>

    @Query("SELECT * FROM workout_record WHERE id = :id")
    suspend fun get(id: String): WorkoutRecordEntity?

    @Query("SELECT COUNT(*) FROM workout_record")
    suspend fun count(): Int

    @Query(
        "SELECT COUNT(*) AS count, COALESCE(SUM(duration), 0) AS totalDuration FROM workout_record " +
            "WHERE startedAt >= :fromMillis AND startedAt < :toMillis",
    )
    fun observeTotals(fromMillis: Long, toMillis: Long): Flow<RecordTotals>

    @Insert
    suspend fun insert(record: WorkoutRecordEntity)

    @Query(
        "DELETE FROM workout_record WHERE id IN " +
            "(SELECT id FROM workout_record ORDER BY startedAt, id LIMIT :count)",
    )
    suspend fun deleteOldest(count: Int)

    @Query("DELETE FROM workout_record WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
