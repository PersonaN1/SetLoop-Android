package io.github.personan1.setloop.core

/** Example timers created on first launch (SPEC D9). */
object Presets {
    data class Preset(val name: String, val definition: TimerDefinition)

    /** Step and timer names are passed in already localized by the app. */
    data class Names(
        val prepare: String,
        val work: String,
        val rest: String,
        val restBetweenSets: String,
        val tabata: String,
        val simpleInterval: String,
        val twoBlocks: String,
    )

    fun all(n: Names): List<Preset> {
        val prepare = Step(name = n.prepare, duration = 10, type = StepType.Prepare)
        return listOf(
            // 4:10
            Preset(
                n.tabata,
                TimerDefinition(
                    prepare = prepare,
                    blocks = listOf(
                        Block(
                            steps = listOf(
                                Step(name = n.work, duration = 20, type = StepType.WorkA),
                                Step(name = n.rest, duration = 10, type = StepType.Rest),
                            ),
                            repeats = 8,
                        ),
                    ),
                ),
            ),
            // 10:00
            Preset(
                n.simpleInterval,
                TimerDefinition(
                    blocks = listOf(
                        Block(
                            steps = listOf(
                                Step(name = n.work, duration = 30, type = StepType.WorkA),
                                Step(name = n.rest, duration = 30, type = StepType.Rest),
                            ),
                            repeats = 10,
                        ),
                    ),
                ),
            ),
            // 4:52, 33 steps: 10 s/5 s x2 + 5 s/3 s x3, 3 sets
            Preset(
                n.twoBlocks,
                TimerDefinition(
                    prepare = prepare,
                    blocks = listOf(
                        Block(
                            steps = listOf(
                                Step(name = n.work, duration = 10, type = StepType.WorkA),
                                Step(name = n.rest, duration = 5, type = StepType.Rest),
                            ),
                            repeats = 2,
                        ),
                        Block(
                            steps = listOf(
                                Step(name = n.work, duration = 5, type = StepType.WorkB),
                                Step(name = n.rest, duration = 3, type = StepType.Rest),
                            ),
                            repeats = 3,
                        ),
                    ),
                    sets = 3,
                    restBetweenSets = Step(name = n.restBetweenSets, duration = 60, type = StepType.Rest),
                ),
            ),
        )
    }
}
