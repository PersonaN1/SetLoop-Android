package io.github.personan1.setloop.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe Navigation Compose destinations. Workout and Summary are not routes: see `SetLoopApp`. */
sealed interface Route {
    @Serializable
    data object TimerList : Route

    @Serializable
    data class TimerPreview(val timerId: String) : Route

    /** A null [timerId] creates a new timer. */
    @Serializable
    data class TimerEditor(val timerId: String? = null) : Route

    @Serializable
    data object History : Route

    @Serializable
    data class RecordDetail(val recordId: String) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object StepColors : Route
}
