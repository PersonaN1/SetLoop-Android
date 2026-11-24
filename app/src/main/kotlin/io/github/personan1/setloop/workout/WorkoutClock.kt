package io.github.personan1.setloop.workout

import android.os.SystemClock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Monotonic time for the engine (TECH T9). */
fun interface WorkoutClock {
    fun now(): Duration
}

/** Elapsed realtime keeps counting in deep sleep, unlike uptime. */
object ElapsedRealtimeClock : WorkoutClock {
    override fun now(): Duration = SystemClock.elapsedRealtime().milliseconds
}
