package io.github.personan1.setloop.shortcuts

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import io.github.personan1.setloop.R

/** A timer as the launcher shortcuts need it. */
data class ShortcutTimer(val id: String, val name: String)

/** Dynamic "Start <timer>" shortcuts for the first timers of the list (SPEC D15). */
class TimerShortcuts(context: Context) {
    private val context = context.applicationContext

    /** Replaces the dynamic shortcuts with the first timers in list order. */
    fun publish(timers: List<ShortcutTimer>) {
        val slots = dynamicSlots(ShortcutManagerCompat.getMaxShortcutCountPerActivity(context))
        val shortcuts = timers.take(slots).mapIndexed { index, timer ->
            ShortcutInfoCompat.Builder(context, shortcutId(timer.id))
                .setShortLabel(timer.name)
                .setLongLabel(context.getString(R.string.shortcut_start_named, timer.name))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_play))
                .setIntent(ShortcutIntents.startTimer(context, timer.id))
                .setRank(index)
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    companion object {
        /** Static Pause and Resume in `res/xml/shortcuts.xml` take slots of the system limit too. */
        const val STATIC_COUNT = 2

        fun dynamicSlots(maxPerActivity: Int): Int = (maxPerActivity - STATIC_COUNT).coerceAtLeast(0)

        fun shortcutId(timerId: String) = "start-$timerId"
    }
}
