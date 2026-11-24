package io.github.personan1.setloop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MergeTest {
    private val a = step("A")
    private val b = step("B", StepType.Rest)
    private val c = step("C")
    private val d = step("D", StepType.Rest)

    @Test
    fun middleStepsBecomeLoopedBlock() {
        val original = Block(steps = listOf(a, b, c, d), repeats = 1)
        val definition = TimerDefinition(blocks = listOf(original)).mergeIntoBlock(setOf(b.id, c.id))
        assertEquals(listOf(listOf("A"), listOf("B", "C"), listOf("D")), definition.blocks.map { it.steps.map(Step::name) })
        assertEquals(listOf(1, 2, 1), definition.blocks.map { it.repeats })
        assertEquals(original.id, definition.blocks[0].id)
        assertEquals(3, definition.blocks.map { it.id }.toSet().size)
    }

    @Test
    fun leadingStepsSplitOnce() {
        val original = Block(steps = listOf(a, b, c), repeats = 3)
        val definition = TimerDefinition(blocks = listOf(original)).mergeIntoBlock(setOf(a.id, b.id))
        assertEquals(listOf(listOf("A", "B"), listOf("C")), definition.blocks.map { it.steps.map(Step::name) })
        assertEquals(listOf(2, 3), definition.blocks.map { it.repeats })
        assertEquals(original.id, definition.blocks[0].id)
        assertNotEquals(original.id, definition.blocks[1].id)
    }

    @Test
    fun rejectsGapsOtherBlocksAndWholeBlock() {
        val definition = TimerDefinition(blocks = listOf(Block(steps = listOf(a, b, c)), Block(steps = listOf(d))))
        assertFailsWith<TimerDefinition.MergeError.NotContiguous> { definition.mergeIntoBlock(setOf(a.id, c.id)) }
        assertFailsWith<TimerDefinition.MergeError.NotContiguous> { definition.mergeIntoBlock(setOf(c.id, d.id)) }
        assertFailsWith<TimerDefinition.MergeError.NotContiguous> { definition.mergeIntoBlock(emptySet()) }
        assertFailsWith<TimerDefinition.MergeError.WholeBlock> { definition.mergeIntoBlock(setOf(d.id)) }
    }

    @Test
    fun respectsBlockLimit() {
        val filler = List(9) { Block(steps = listOf(step("x"))) }
        val definition = TimerDefinition(blocks = filler + Block(steps = listOf(a, b, c)))
        assertFailsWith<TimerDefinition.MergeError.TooManyBlocks> { definition.mergeIntoBlock(setOf(b.id)) }
        assertEquals(10, definition.blocks.size)
    }

    @Test
    fun nextStepAlternates() {
        val names: (StepType) -> String = { if (it == StepType.Rest) "Rest" else "Work" }
        assertEquals(StepType.WorkA, Step.next(null, names).type)
        assertEquals(StepType.Rest, Step.next(step("W", StepType.WorkB), names).type)
        assertEquals(StepType.WorkA, Step.next(step("R", StepType.Rest), names).type)
    }
}

class PreviewStripTest {
    private val work = step("W", StepType.WorkA, 20)
    private val rest = step("R", StepType.Rest, 10)

    @Test
    fun smallWorkoutShowsEverything() {
        val strip = PreviewStrip.from(
            TimerDefinition(prepare = step("P", StepType.Prepare), blocks = listOf(Block(steps = listOf(work, rest), repeats = 8))),
        )
        assertEquals(PreviewStrip.Level.Workout, strip.level)
        assertEquals(1, strip.groups.size)
        assertEquals(17, strip.groups[0].segments.size)
    }

    @Test
    fun manySetsShowOneSet() {
        val strip = PreviewStrip.from(TimerDefinition(blocks = listOf(Block(steps = listOf(work, rest), repeats = 8)), sets = 4))
        assertEquals(PreviewStrip.Level.OneSet, strip.level)
        val oneSet = List(8) {
            listOf(PreviewStrip.Segment(StepType.WorkA, 20), PreviewStrip.Segment(StepType.Rest, 10))
        }.flatten()
        assertEquals(listOf(PreviewStrip.Group(oneSet, repeats = 4)), strip.groups)
    }

    @Test
    fun longBlocksShowOneCycleEach() {
        val strip = PreviewStrip.from(
            TimerDefinition(blocks = listOf(Block(steps = listOf(work, rest), repeats = 20), Block(steps = listOf(rest), repeats = 3))),
        )
        assertEquals(PreviewStrip.Level.OneCycle, strip.level)
        assertEquals(listOf(20, 3), strip.groups.map { it.repeats })
    }

    @Test
    fun hugeBlocksCollapse() {
        val long = step("L", StepType.WorkB, 60)
        val steps = List(20) { work } + long
        val strip = PreviewStrip.from(
            TimerDefinition(
                blocks = listOf(Block(steps = steps, repeats = 2), Block(steps = listOf(rest, work, rest, work, rest))),
            ),
        )
        assertEquals(PreviewStrip.Level.Blocks, strip.level)
        assertEquals(listOf(PreviewStrip.Segment(StepType.WorkB, 460)), strip.groups[0].segments)
        assertEquals(1, strip.groups[1].segments.size)
    }
}

class TimerNamingTest {
    @Test
    fun freeNameStaysAsIs() {
        assertEquals("New Timer", TimerNaming.unique("New Timer", listOf("Tabata")))
    }

    @Test
    fun takenNameGetsNextNumber() {
        assertEquals("New Timer (2)", TimerNaming.unique("New Timer", listOf("New Timer")))
        assertEquals("New Timer (3)", TimerNaming.unique("New Timer", listOf("New Timer", "New Timer (2)")))
    }

    @Test
    fun fillsTheFirstGap() {
        assertEquals("A (2)", TimerNaming.unique("A", listOf("A", "A (3)")))
    }

    @Test
    fun copyOfNumberedNameContinuesTheSeries() {
        assertEquals("A (3)", TimerNaming.unique("A (2)", listOf("A", "A (2)")))
    }

    @Test
    fun otherParenthesesAreKept() {
        assertEquals("Run (easy) (2)", TimerNaming.unique("Run (easy)", listOf("Run (easy)")))
        assertEquals("Set (1) (2)", TimerNaming.unique("Set (1)", listOf("Set (1)")))
    }
}

class RenameStepsTest {
    @Test
    fun renamesEveryStepThatMatches() {
        val definition = TimerDefinition(
            prepare = Step(name = "Prepare", duration = 10, type = StepType.Prepare),
            blocks = listOf(
                Block(
                    steps = listOf(
                        Step(name = "Work", duration = 20, type = StepType.WorkA),
                        Step(name = "Burpees", duration = 20, type = StepType.WorkB),
                    ),
                ),
            ),
            sets = 2,
            restBetweenSets = Step(name = "Rest", duration = 60, type = StepType.Rest),
            cooldown = Step(name = "Cooldown", duration = 60, type = StepType.Cooldown),
        )
        val names = mapOf("Prepare" to "Подготовка", "Work" to "Работа", "Rest" to "Отдых")
        val renamed = definition.renameSteps { names[it] }
        assertNotSame(definition, renamed)
        assertEquals("Подготовка", renamed.prepare?.name)
        assertEquals(listOf("Работа", "Burpees"), renamed.blocks[0].steps.map { it.name })
        assertEquals("Отдых", renamed.restBetweenSets?.name)
        assertEquals("Cooldown", renamed.cooldown?.name)
        assertSame(renamed, renamed.renameSteps { names[it] })
    }

    @Test
    fun cappedNameCutsByCharacters() {
        assertEquals(60, "x".repeat(80).cappedName.length)
        assertEquals("short", "short".cappedName)
        assertTrue(("e\u0301".repeat(61)).cappedName == "e\u0301".repeat(60))
    }
}
