package io.github.personan1.setloop.core

val presetNames = Presets.Names(
    prepare = "Prepare", work = "Work", rest = "Rest", restBetweenSets = "Set rest",
    tabata = "Tabata", simpleInterval = "Simple", twoBlocks = "Two blocks",
)

fun step(duration: Int, type: StepType = StepType.WorkA): Step =
    Step(name = type.rawValue, duration = duration, type = type)

fun step(name: String, type: StepType = StepType.WorkA, duration: Int = 10): Step =
    Step(name = name, duration = duration, type = type)
