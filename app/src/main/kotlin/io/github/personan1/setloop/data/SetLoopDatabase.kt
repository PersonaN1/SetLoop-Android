package io.github.personan1.setloop.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Schema version 1; the JSON schema is exported to `app/schemas`. */
@Database(
    entities = [SavedTimerEntity::class, WorkoutRecordEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SetLoopDatabase : RoomDatabase() {
    abstract fun timerDao(): SavedTimerDao

    abstract fun recordDao(): WorkoutRecordDao

    companion object {
        const val FILE_NAME = "setloop.db"

        fun create(context: Context): SetLoopDatabase =
            Room.databaseBuilder(context.applicationContext, SetLoopDatabase::class.java, FILE_NAME).build()
    }
}
