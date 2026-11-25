package io.github.personan1.setloop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.personan1.setloop.shortcuts.ShortcutAction
import io.github.personan1.setloop.shortcuts.ShortcutIntents
import io.github.personan1.setloop.ui.SetLoopApp

class MainActivity : ComponentActivity() {
    /** A shortcut request waiting for the UI; cleared once handled. */
    private var pendingShortcut by mutableStateOf<ShortcutAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // After recreation the launch intent is the same one that was already handled.
        if (savedInstanceState == null) pendingShortcut = ShortcutIntents.parse(intent)
        val container = (application as SetLoopApplication).container
        setContent {
            SetLoopApp(
                container = container,
                shortcut = pendingShortcut,
                onShortcutHandled = { pendingShortcut = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShortcut = ShortcutIntents.parse(intent)
    }
}
