package io.github.personan1.setloop.data

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.Presets
import io.github.personan1.setloop.core.TimerDefinition
import java.util.Locale

/**
 * Example timers and default step names follow the interface language: after switching to Russian,
 * "Work" reads "Rabota". A name the user typed is left alone unless it equals a default name in any
 * bundled language (port of iOS `DefaultNames`).
 *
 * The core stores the English default names as keys; [localize] maps a stored name to the current language.
 */
class DefaultNames private constructor(private val translations: Map<String, String>) {
    fun localize(name: String): String = translations[name] ?: name

    /** Renames default-named steps. Returns the same instance when nothing changed. */
    fun localize(definition: TimerDefinition): TimerDefinition = definition.renameSteps(translations::get)

    companion object {
        private val bundledLocales = listOf(Locale.ENGLISH, Locale.forLanguageTag("ru"))

        private val keys: Map<String, Int> = mapOf(
            "Prepare" to R.string.default_name_prepare,
            "Work" to R.string.default_name_work,
            "Rest" to R.string.default_name_rest,
            "Rest between sets" to R.string.default_name_rest_between_sets,
            "Cooldown" to R.string.default_name_cooldown,
            "Tabata 20/10" to R.string.default_name_tabata,
            "Simple interval" to R.string.default_name_simple_interval,
            "Your example" to R.string.default_name_your_example,
        )

        /** The default name in every bundled language maps to the same name in the current language. */
        fun from(context: Context): DefaultNames {
            val translations = mutableMapOf<String, String>()
            for ((key, resId) in keys) {
                val current = context.getString(resId)
                translations[key] = current
                for (locale in bundledLocales) translations[context.getString(locale, resId)] = current
            }
            return DefaultNames(translations)
        }

        /** Names for the example timers, in the current language (applied once, at seeding). */
        fun presetNames(context: Context) = Presets.Names(
            prepare = context.getString(R.string.default_name_prepare),
            work = context.getString(R.string.default_name_work),
            rest = context.getString(R.string.default_name_rest),
            restBetweenSets = context.getString(R.string.default_name_rest_between_sets),
            tabata = context.getString(R.string.default_name_tabata),
            simpleInterval = context.getString(R.string.default_name_simple_interval),
            twoBlocks = context.getString(R.string.default_name_your_example),
        )

        /** The name for a timer saved with an empty name, before numbering. */
        fun newTimerName(context: Context): String = context.getString(R.string.default_name_new_timer)

        private fun Context.getString(locale: Locale, @StringRes resId: Int): String {
            val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
            return createConfigurationContext(configuration).getString(resId)
        }
    }
}
