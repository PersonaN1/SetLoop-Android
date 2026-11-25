package io.github.personan1.setloop.workout

import io.github.personan1.setloop.core.Block
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.TimerDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class WorkoutStartFlowTest {
    private class FakeEnvironment(var needsPermission: Boolean = false, var restricted: Boolean = false) : StartEnvironment {
        var requested = 0

        override fun needsNotificationPermission() = needsPermission

        override fun markNotificationPermissionRequested() {
            requested++
            needsPermission = false
        }

        override fun isBackgroundRestricted() = restricted
    }

    private val prepared = PreparedWorkout(
        timerName = "Tabata",
        definition = TimerDefinition(blocks = listOf(Block(steps = listOf(Step(name = "Work", duration = 20, type = StepType.WorkA))))),
        options = WorkoutOptions(),
    )
    private val launched = mutableListOf<PreparedWorkout>()
    private var permissionRequests = 0

    private fun flow(environment: StartEnvironment, known: Set<String> = setOf("t1")) = WorkoutStartFlow(
        environment = environment,
        prepare = { id -> prepared.takeIf { id in known } },
        launch = { launched += it },
        requestNotificationPermission = { permissionRequests++ },
    )

    @Test
    fun `decision order is permission, then warning, then start`() {
        fun decide(needs: Boolean, restricted: Boolean, permissionDone: Boolean = false, warningDone: Boolean = false) =
            decideStart(needs, restricted, permissionDone, warningDone)

        assertEquals(StartDecision.RequestPermission, decide(needs = true, restricted = true))
        assertEquals(StartDecision.WarnBackgroundRestricted, decide(needs = true, restricted = true, permissionDone = true))
        assertEquals(StartDecision.Start, decide(needs = true, restricted = true, permissionDone = true, warningDone = true))
        assertEquals(StartDecision.WarnBackgroundRestricted, decide(needs = false, restricted = true))
        assertEquals(StartDecision.Start, decide(needs = false, restricted = false))
    }

    @Test
    fun `starts at once when nothing is needed`() = runTest {
        val flow = flow(FakeEnvironment())

        flow.begin("t1")

        assertEquals(listOf(prepared), launched)
        assertNull(flow.progress.value)
        assertEquals(0, permissionRequests)
    }

    @Test
    fun `asks for the permission once and starts after any answer`() = runTest {
        val environment = FakeEnvironment(needsPermission = true)
        val flow = flow(environment)

        flow.begin("t1")
        assertEquals(StartProgress("t1", StartProgress.Stage.AwaitingPermission), flow.progress.value)
        assertEquals(1, permissionRequests)
        assertEquals(emptyList(), launched)

        flow.onNotificationPermissionResult()
        assertEquals(listOf(prepared), launched)
        assertNull(flow.progress.value)

        flow.begin("t1")
        assertEquals(1, permissionRequests)
        assertEquals(2, launched.size)
    }

    @Test
    fun `background restriction waits for confirmation`() = runTest {
        val flow = flow(FakeEnvironment(restricted = true))

        flow.begin("t1")
        assertEquals(StartProgress("t1", StartProgress.Stage.BackgroundWarning), flow.progress.value)
        assertEquals(emptyList(), launched)

        flow.confirmBackgroundWarning()
        assertEquals(listOf(prepared), launched)
        assertNull(flow.progress.value)
    }

    @Test
    fun `permission then warning then start`() = runTest {
        val flow = flow(FakeEnvironment(needsPermission = true, restricted = true))

        flow.begin("t1")
        flow.onNotificationPermissionResult()
        assertEquals(StartProgress("t1", StartProgress.Stage.BackgroundWarning), flow.progress.value)
        flow.confirmBackgroundWarning()

        assertEquals(1, launched.size)
    }

    @Test
    fun `cancelling the warning starts nothing`() = runTest {
        val flow = flow(FakeEnvironment(restricted = true))

        flow.begin("t1")
        flow.cancel()
        flow.confirmBackgroundWarning()

        assertEquals(emptyList(), launched)
        assertNull(flow.progress.value)
    }

    @Test
    fun `a deleted timer starts nothing`() = runTest {
        val flow = flow(FakeEnvironment())

        flow.begin("gone")

        assertEquals(emptyList(), launched)
        assertNull(flow.progress.value)
    }

    @Test
    fun `a stale permission result is ignored`() = runTest {
        val flow = flow(FakeEnvironment(restricted = true))

        flow.onNotificationPermissionResult()
        flow.begin("t1")
        flow.onNotificationPermissionResult()

        assertEquals(StartProgress("t1", StartProgress.Stage.BackgroundWarning), flow.progress.value)
        assertEquals(emptyList(), launched)
    }

    @Test
    fun `an unfinished start can be restored`() = runTest {
        val environment = FakeEnvironment()
        val restored = WorkoutStartFlow(
            environment = environment,
            prepare = { prepared },
            launch = { launched += it },
            requestNotificationPermission = { permissionRequests++ },
            initial = StartProgress("t1", StartProgress.Stage.AwaitingPermission),
        )

        restored.onNotificationPermissionResult()

        assertEquals(1, launched.size)
    }
}
