package io.github.personan1.setloop.ui.components

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import io.github.personan1.setloop.core.Block
import io.github.personan1.setloop.core.Step
import io.github.personan1.setloop.core.StepType
import io.github.personan1.setloop.core.TimerDefinition
import io.github.personan1.setloop.data.TEST_SDK
import io.github.personan1.setloop.workout.WorkoutFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK], qualifiers = "en")
class DurationTextTest {
    private val resources get() = ApplicationProvider.getApplicationContext<Context>().resources

    private val definition = TimerDefinition(
        blocks = listOf(
            Block(steps = List(33) { Step(name = "Work", duration = 10, type = StepType.WorkA) }),
            Block(steps = listOf(Step(name = "Rest", duration = 10, type = StepType.Rest))),
        ),
        sets = 3,
    )

    @Test
    fun `digits show hours only when needed`() {
        assertEquals("0:05", WorkoutFormat.duration(5))
        assertEquals("4:52", WorkoutFormat.duration(292))
        assertEquals("59:59", WorkoutFormat.duration(3599))
        assertEquals("1:05:30", WorkoutFormat.duration(3930))
        assertEquals("0:00", WorkoutFormat.duration(-3))
    }

    @Test
    fun `spoken duration uses words and plurals`() {
        assertEquals("1 minute, 30 seconds", spokenDuration(resources, 90))
        assertEquals("1 second", spokenDuration(resources, 1))
        assertEquals("2 minutes", spokenDuration(resources, 120))
        assertEquals("1 hour, 5 minutes, 30 seconds", spokenDuration(resources, 3930))
        assertEquals("0 seconds", spokenDuration(resources, 0))
    }

    @Test
    @Config(qualifiers = "ru")
    fun `spoken duration follows Russian plural rules`() {
        assertEquals("1 минута, 30 секунд", spokenDuration(resources, 90))
        assertEquals("2 минуты, 1 секунда", spokenDuration(resources, 121))
        assertEquals("5 минут, 22 секунды", spokenDuration(resources, 322))
        assertEquals("1 час", spokenDuration(resources, 3600))
        assertEquals("0 секунд", spokenDuration(resources, 0))
    }

    @Test
    fun `structure text counts blocks, sets and steps`() {
        assertEquals("2 blocks \u00B7 3 sets", definition.structureText(resources))
        assertEquals("2 blocks \u00B7 3 sets \u00B7 102 steps", definition.structureText(resources, includingSteps = true))
        val single = TimerDefinition(blocks = definition.blocks.take(1).map { it.copy(steps = it.steps.take(1)) })
        assertEquals("1 block", single.structureText(resources))
        assertEquals("1 block \u00B7 1 step", single.structureText(resources, includingSteps = true))
    }

    @Test
    @Config(qualifiers = "ru")
    fun `structure text is plural in Russian`() {
        assertEquals("2 блока \u00B7 3 сета", definition.structureText(resources))
        assertEquals("2 блока \u00B7 3 сета \u00B7 102 шага", definition.structureText(resources, includingSteps = true))
    }
}
