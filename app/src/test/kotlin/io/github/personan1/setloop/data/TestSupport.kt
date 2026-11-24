package io.github.personan1.setloop.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.personan1.setloop.core.Block
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.TimerDefinition
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicLong

const val TEST_SDK = 37

fun inMemoryDatabase(): SetLoopDatabase = Room
    .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), SetLoopDatabase::class.java)
    .allowMainThreadQueries()
    .build()

fun sampleDefinition(workSeconds: Int = 20) = TimerDefinition(
    prepare = Step(name = "Prepare", duration = 5, type = StepType.Prepare),
    blocks = listOf(
        Block(
            steps = listOf(
                Step(name = "Work", duration = workSeconds, type = StepType.WorkA),
                Step(name = "Rest", duration = 10, type = StepType.Rest),
            ),
            repeats = 3,
        ),
    ),
)

fun fixedClock(iso: String): Clock = Clock.fixed(Instant.parse(iso), ZoneOffset.UTC)

/** Moves by one millisecond per call so entity timestamps are distinct and ordered. */
class TickingClock(start: Long = 1_000_000L) : Clock() {
    private val now = AtomicLong(start)

    override fun getZone() = ZoneOffset.UTC

    override fun withZone(zone: java.time.ZoneId?): Clock = this

    override fun instant(): Instant = Instant.ofEpochMilli(now.getAndIncrement())
}
