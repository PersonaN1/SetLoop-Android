package io.github.personan1.setloop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class WorkoutPlanTest {
    @Test
    fun presetTotalsMatchDesign() {
        val presets = Presets.all(presetNames).map { it.definition.plan }
        assertEquals(250L, presets[0].totalDuration)
        assertEquals(600L, presets[1].totalDuration)

        val example = presets[2]
        assertEquals(292L, example.totalDuration)
        assertEquals(33, example.steps.size)
        assertEquals(105L, example.duration(StepType.Category.Work))
        assertEquals(177L, example.duration(StepType.Category.Rest))
        assertEquals(10L, example.duration(StepType.Category.Other))
    }

    @Test
    fun positionsCountSetsBlocksAndCycles() {
        val plan = Presets.all(presetNames)[2].definition.plan
        assertNull(plan.steps[0].position.set)
        assertEquals(PlannedStep.Position(1, 3, 1, 2, 1, 2), plan.steps[1].position)
        assertEquals(PlannedStep.Position(1, 3, 2, 2, 1, 3), plan.steps[5].position)
        assertNull(plan.steps[11].position.set) // rest between sets
        assertEquals((0 until 33).toList(), plan.steps.map { it.index })
    }

    @Test
    fun skipLastRestRemovesOnlyFinalRest() {
        var definition = TimerDefinition(
            blocks = listOf(Block(steps = listOf(step(20), step(10, StepType.Rest)), repeats = 2)),
            sets = 2,
            restBetweenSets = step(60, StepType.Rest),
        )
        assertEquals(9, definition.plan.steps.size)

        definition = definition.copy(skipLastRest = true)
        val plan = definition.plan
        assertEquals(8, plan.steps.size)
        assertEquals(StepType.WorkA, plan.steps.last().type)
        assertEquals(1, plan.steps.count { it.duration == 60 }) // rest between sets stays
    }

    @Test
    fun skipLastRestKeepsCooldownAndIgnoresWorkEnding() {
        val withCooldown = TimerDefinition(
            blocks = listOf(Block(steps = listOf(step(20), step(10, StepType.Rest)))),
            cooldown = step(30, StepType.Cooldown),
            skipLastRest = true,
        ).plan
        assertEquals(listOf(StepType.WorkA, StepType.Cooldown), withCooldown.steps.map { it.type })

        val workLast = TimerDefinition(blocks = listOf(Block(steps = listOf(step(10, StepType.Rest), step(20)))), skipLastRest = true).plan
        assertEquals(2, workLast.steps.size)
    }

    @Test
    fun validationLimits() {
        assertFailsWith<TimerDefinition.ValidationError.NoSteps> {
            TimerDefinition(blocks = listOf(Block(steps = emptyList()))).validate()
        }
        assertFailsWith<TimerDefinition.ValidationError.RepeatsOutOfRange> {
            TimerDefinition(blocks = listOf(Block(steps = listOf(step(5)), repeats = 100))).validate()
        }
        val tooLong = step(6000)
        val outOfRange = assertFailsWith<TimerDefinition.ValidationError.StepDurationOutOfRange> {
            TimerDefinition(blocks = listOf(Block(steps = listOf(tooLong)))).validate()
        }
        assertEquals(TimerDefinition.ValidationError.StepDurationOutOfRange(tooLong.id), outOfRange)
        assertFailsWith<TimerDefinition.ValidationError.TooManyBlocks> {
            TimerDefinition(blocks = List(11) { Block(steps = listOf(step(5))) }).validate()
        }
        assertFailsWith<TimerDefinition.ValidationError.EmptyPlan> {
            TimerDefinition(blocks = listOf(Block(steps = listOf(step(5, StepType.Rest)))), skipLastRest = true).validate()
        }
        // Prepare still gives the plan something to run.
        TimerDefinition(
            prepare = step(5, StepType.Prepare),
            blocks = listOf(Block(steps = listOf(step(5, StepType.Rest)))),
            skipLastRest = true,
        ).validate()
    }

    @Test
    fun definitionRoundTripsThroughJson() {
        val definition = Presets.all(presetNames)[2].definition
        assertEquals(definition, TimerDefinition.fromJson(definition.toJson()))
    }
}

class TimerDefinitionJsonTest {
    /** Shaped like Swift `JSONEncoder` output: uppercase UUIDs, nil optionals omitted, raw enum values. */
    private val swiftJson = """
        {"version":1,
         "prepare":{"id":"11111111-1111-1111-1111-111111111111","name":"Prepare","duration":10,"type":"prepare"},
         "blocks":[{"id":"22222222-2222-2222-2222-222222222222","repeats":2,"steps":[
            {"id":"33333333-3333-3333-3333-333333333333","name":"Work","duration":20,"type":"workA"},
            {"id":"44444444-4444-4444-4444-444444444444","name":"Rest","duration":10,"type":"rest"}]}],
         "sets":3,
         "restBetweenSets":{"id":"55555555-5555-5555-5555-555555555555","name":"Set rest","duration":60,"type":"rest"},
         "skipLastRest":true}
    """.trimIndent()

    @Test
    fun decodesSwiftEncodedSample() {
        val definition = TimerDefinition.fromJson(swiftJson)
        assertEquals(1, definition.version)
        assertEquals("Prepare", definition.prepare?.name)
        assertEquals(StepType.Prepare, definition.prepare?.type)
        assertEquals(2, definition.blocks.single().repeats)
        assertEquals(listOf(StepType.WorkA, StepType.Rest), definition.blocks.single().steps.map { it.type })
        assertEquals("33333333-3333-3333-3333-333333333333", definition.blocks.single().steps[0].id.toString())
        assertEquals(3, definition.sets)
        assertEquals(60, definition.restBetweenSets?.duration)
        assertNull(definition.cooldown)
        assertTrue(definition.skipLastRest)
    }

    @Test
    fun encodesSameKeysAndRawValues() {
        val json = Json.parseToJsonElement(TimerDefinition.fromJson(swiftJson).toJson()).jsonObject
        assertEquals(setOf("version", "prepare", "blocks", "sets", "restBetweenSets", "skipLastRest"), json.keys)
        assertEquals(Json.parseToJsonElement(swiftJson), json)
        val step = json.getValue("prepare").jsonObject
        assertEquals(setOf("id", "name", "duration", "type"), step.keys)
        assertEquals("\"11111111-1111-1111-1111-111111111111\"", step.getValue("id").toString())
        assertEquals("\"prepare\"", step.getValue("type").toString())
    }

    @Test
    fun encodesUuidUppercaseLikeSwift() {
        val definition = TimerDefinition(blocks = listOf(Block(steps = listOf(step(5)))))
        val encoded = definition.toJson()
        val id = definition.blocks[0].id.toString()
        assertTrue(encoded.contains("\"${id.uppercase()}\""))
        assertEquals(definition, TimerDefinition.fromJson(encoded))
    }
}

class PaletteTest {
    @Test
    fun lightenedRedMatchesDesignIntent() {
        val light = RGB(0xE8452C).lightened
        assertEquals(0xFF, light.hex shr 16)
        assertTrue(light.green > RGB(0xE8452C).green)
    }

    @Test
    fun restColorIsBlockedForWork() {
        val colors = StepColors.DEFAULT
        assertFalse(colors.isAllowed(RGB(0x1E8E52), StepType.WorkB))
        assertFalse(colors.isAllowed(RGB(0xE8452C), StepType.Rest))
        assertTrue(colors.isAllowed(RGB(0x2F6BDB), StepType.WorkB))
        assertTrue(colors.isAllowed(RGB(0x1E8E52), StepType.Prepare))
    }

    @Test
    fun hexValuesAndArgbAreKept() {
        assertEquals(12, Palette.colors.size)
        assertEquals(0xE8452C, Palette.colors.first().hex)
        assertEquals(0xFF5B6470L, Palette.colors.last().argb)
        assertEquals(0xABCDEF, RGB(0x12ABCDEF).hex)
    }
}

class PlanArithmeticTest {
    private fun expectMatchesPlan(definition: TimerDefinition) {
        val plan = definition.plan
        assertEquals(plan.steps.size, definition.stepCount)
        assertEquals(plan.totalDuration, definition.totalDuration)
        for (category in StepType.Category.entries) {
            assertEquals(plan.duration(category), definition.duration(category))
        }
    }

    @Test
    fun presetsMatchPlan() {
        for (preset in Presets.all(presetNames)) {
            expectMatchesPlan(preset.definition)
            expectMatchesPlan(preset.definition.copy(skipLastRest = !preset.definition.skipLastRest))
        }
    }

    @Test
    fun edgeCasesMatchPlan() {
        val rest = step(7, StepType.Rest)
        val cases = listOf(
            TimerDefinition(blocks = emptyList()),
            TimerDefinition(blocks = listOf(Block(steps = emptyList()))),
            TimerDefinition(blocks = listOf(Block(steps = listOf(rest))), skipLastRest = true),
            TimerDefinition(prepare = step(5, StepType.Prepare), blocks = listOf(Block(steps = listOf(rest))), skipLastRest = true),
            TimerDefinition(
                blocks = listOf(Block(steps = listOf(step(20), rest), repeats = 4)), sets = 3,
                restBetweenSets = step(60, StepType.Rest), cooldown = step(30, StepType.Cooldown), skipLastRest = true,
            ),
            // The last non-empty block decides; an empty block after it doesn't.
            TimerDefinition(
                blocks = listOf(Block(steps = listOf(step(20), rest), repeats = 2), Block(steps = emptyList())), sets = 2,
                skipLastRest = true,
            ),
            TimerDefinition(
                blocks = listOf(Block(steps = listOf(rest, step(20)))), sets = 2,
                restBetweenSets = step(60, StepType.Rest), skipLastRest = true,
            ),
            TimerDefinition(blocks = emptyList(), sets = 3, restBetweenSets = step(60, StepType.Rest), skipLastRest = true),
            TimerDefinition(
                blocks = listOf(Block(steps = listOf(step(20, StepType.WorkB), step(15, StepType.WorkC)), repeats = 99)),
                sets = 99,
            ),
        )
        cases.forEach(::expectMatchesPlan)
    }

    @Test
    fun emptyPlanIsCounted() {
        assertEquals(
            0,
            TimerDefinition(blocks = listOf(Block(steps = listOf(step(5, StepType.Rest)))), skipLastRest = true).stepCount,
        )
    }

    @Test
    fun totalsBeyondIntRangeDoNotOverflow() {
        val definition = TimerDefinition(
            blocks = List(10) { Block(steps = List(30) { step(5999) }, repeats = 99) },
            sets = 99,
        )
        assertEquals(10L * 30 * 99 * 99 * 5999, definition.totalDuration)
    }
}

class StepColorsCodingTest {
    @Test
    fun roundTripsAsKeyedObject() {
        val colors = StepColors(StepColors.DEFAULT.colors + (StepType.WorkB to RGB(0x2F6BDB)))
        val encoded = Json.encodeToString(StepColors.serializer(), colors)
        val json = Json.parseToJsonElement(encoded).jsonObject
        assertTrue(json.getValue("colors") is JsonObject)
        assertEquals(colors, Json.decodeFromString(StepColors.serializer(), encoded))
    }

    @Test
    fun readsKeyedObjectSkippingUnknownTypes() {
        val json = """{"colors": {"workA": {"hex": 3107803}, "workD": {"hex": 1}}}"""
        val colors = Json.decodeFromString(StepColors.serializer(), json)
        assertEquals(mapOf(StepType.WorkA to RGB(0x2F6BDB)), colors.colors)
    }

    @Test
    fun readsLegacyFlatArraySkippingUnknownTypes() {
        val json = """{"colors": ["rest", {"hex": 3107803}, "workD", {"hex": 1}, "workA", {"hex": 15222060}]}"""
        val colors = Json.decodeFromString(StepColors.serializer(), json)
        assertEquals(mapOf(StepType.Rest to RGB(0x2F6BDB), StepType.WorkA to RGB(0xE8452C)), colors.colors)
    }
}
