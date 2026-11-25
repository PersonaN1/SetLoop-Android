package io.github.personan1.setloop.ui.timers

import io.github.personan1.setloop.core.Limits
import io.github.personan1.setloop.core.StepType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StepDraftTest {
    private val work = StepDraft(step("Work", seconds = 75))

    @Test
    fun `duration splits into minutes and seconds`() {
        assertEquals(1, work.minutes)
        assertEquals(15, work.seconds)
    }

    @Test
    fun `wheels change one part and keep the other`() {
        assertEquals(135, work.withMinutes(2).step.duration)
        assertEquals(90, work.withSeconds(30).step.duration)
    }

    @Test
    fun `duration is capped at 99 59`() {
        assertEquals(Limits.stepDuration.last, work.withMinutes(99).withSeconds(59).step.duration)
        assertEquals(Limits.stepDuration.last, StepDraft(step("A", 5999)).withMinutes(99).step.duration)
    }

    @Test
    fun `zero is kept on the wheels but cannot be saved`() {
        val zero = StepDraft(step("A", seconds = 5)).withSeconds(0)
        assertEquals(0, zero.step.duration)
        assertFalse(zero.canSave)
        assertEquals(5, zero.withSeconds(5).step.duration)
        assertTrue(zero.withSeconds(1).canSave)
    }

    @Test
    fun `changing type renames a default name`() {
        val rest = work.withType(StepType.Rest, testNames)
        assertEquals(StepType.Rest, rest.step.type)
        assertEquals("Rest", rest.step.name)
        assertEquals("Work", rest.withType(StepType.WorkB, testNames).step.name)
    }

    @Test
    fun `changing type keeps a name the user typed`() {
        val named = work.withName("Burpees").withType(StepType.Rest, testNames)
        assertEquals("Burpees", named.step.name)
    }

    @Test
    fun `an empty name follows the type`() {
        assertEquals("Rest", work.withName("  ").withType(StepType.Rest, testNames).step.name)
    }

    @Test
    fun `finished trims the name and defaults an empty one`() {
        assertEquals("Burpees", work.withName("  Burpees ").finished(testNames).name)
        assertEquals("Work", work.withName("   ").finished(testNames).name)
    }

    @Test
    fun `name is capped`() {
        assertEquals(Limits.maxNameLength, work.withName("y".repeat(200)).step.name.length)
    }
}
