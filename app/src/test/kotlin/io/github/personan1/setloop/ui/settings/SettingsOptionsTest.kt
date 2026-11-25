package io.github.personan1.setloop.ui.settings

import io.github.personan1.setloop.core.SoundSet
import io.github.personan1.setloop.data.AppSettingsValues
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsOptionsTest {
    @Test
    fun `sound choices map to sets and back`() {
        assertEquals(
            listOf(null, SoundSet.Glass, SoundSet.Marimba, SoundSet.Note),
            SoundChoice.entries.map { it.set },
        )
        (listOf(null) + SoundSet.entries).forEach { assertEquals(it, SoundChoice.of(it).set) }
    }

    @Test
    fun `defaults show no note`() {
        assertNull(SoundNote.of(AppSettingsValues()))
    }

    @Test
    fun `sounds and voice off explains silent steps`() {
        val settings = AppSettingsValues(soundSet = null, voicePrompts = false)

        assertEquals(SoundNote.AllSoundsOff, SoundNote.of(settings))
    }

    @Test
    fun `the all-off note wins over the silent mode note`() {
        val settings = AppSettingsValues(soundSet = null, voicePrompts = false, ignoreSilentMode = false)

        assertEquals(SoundNote.AllSoundsOff, SoundNote.of(settings))
    }

    @Test
    fun `voice alone is not silent`() {
        assertNull(SoundNote.of(AppSettingsValues(soundSet = null, voicePrompts = true)))
    }

    @Test
    fun `following silent mode is explained when ignoring is off`() {
        assertEquals(SoundNote.FollowsSilentMode, SoundNote.of(AppSettingsValues(ignoreSilentMode = false)))
    }
}
