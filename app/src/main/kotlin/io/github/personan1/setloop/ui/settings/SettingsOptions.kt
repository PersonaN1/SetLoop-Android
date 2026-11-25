package io.github.personan1.setloop.ui.settings

import androidx.annotation.StringRes
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.SoundSet
import io.github.personan1.setloop.data.AppSettingsValues
import io.github.personan1.setloop.data.AppTheme

/** What the sound picker offers, in order. A null [set] is "Off". */
enum class SoundChoice(val set: SoundSet?, @StringRes val label: Int) {
    Off(null, R.string.settings_sound_off),
    Glass(SoundSet.Glass, R.string.settings_sound_glass),
    Marimba(SoundSet.Marimba, R.string.settings_sound_marimba),
    Note(SoundSet.Note, R.string.settings_sound_note),
    ;

    companion object {
        fun of(set: SoundSet?): SoundChoice = entries.first { it.set == set }
    }
}

@get:StringRes
val AppTheme.label: Int
    get() = when (this) {
        AppTheme.System -> R.string.settings_theme_system
        AppTheme.Light -> R.string.settings_theme_light
        AppTheme.Dark -> R.string.settings_theme_dark
    }

/** The explanation under the sound group. Only one shows, the more serious first (SPEC D6, TECH T2, T6). */
enum class SoundNote(@StringRes val text: Int) {
    AllSoundsOff(R.string.settings_note_all_sounds_off),
    FollowsSilentMode(R.string.settings_note_silent_mode),
    ;

    companion object {
        fun of(settings: AppSettingsValues): SoundNote? = when {
            settings.soundSet == null && !settings.voicePrompts -> AllSoundsOff
            !settings.ignoreSilentMode -> FollowsSilentMode
            else -> null
        }
    }
}
