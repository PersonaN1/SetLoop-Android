package io.github.personan1.setloop

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.content.ContextCompat
import io.github.personan1.setloop.core.Presets
import io.github.personan1.setloop.workout.WorkoutController
import io.github.personan1.setloop.workout.WorkoutOptions
import io.github.personan1.setloop.workout.WorkoutService
import io.github.personan1.setloop.workout.WorkoutUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PrototypeApp()
                }
            }
        }
    }
}

@Composable
private fun PrototypeApp() {
    val controller = WorkoutController.instance
    val state by controller.state.collectAsState()
    when (val current = state) {
        WorkoutUiState.Idle -> PresetListScreen()
        is WorkoutUiState.Active -> WorkoutScreen(
            state = current,
            onTogglePause = controller::togglePause,
            onSkip = controller::skip,
            onBack = controller::back,
            onFinish = controller::finish,
        )
        is WorkoutUiState.Finished -> ResultScreen(current.result, onClose = controller::dismissResult)
    }
}

@Composable
private fun PresetListScreen() {
    val context = LocalContext.current
    val presets = Presets.all(
        Presets.Names(
            prepare = stringResource(R.string.preset_prepare),
            work = stringResource(R.string.preset_work),
            rest = stringResource(R.string.preset_rest),
            restBetweenSets = stringResource(R.string.preset_rest_between_sets),
            tabata = stringResource(R.string.preset_tabata),
            simpleInterval = stringResource(R.string.preset_simple_interval),
            twoBlocks = stringResource(R.string.preset_two_blocks),
        ),
    )
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }
    var pending by rememberSaveable { mutableStateOf<Int?>(null) }

    fun start(index: Int) {
        val preset = presets[index]
        WorkoutService.start(context, preset.definition, preset.name, WorkoutOptions())
    }

    // The workout runs whether or not the permission is granted; only its notification is hidden.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pending?.let(::start)
        pending = null
    }

    PresetList(presets) { index ->
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !askedForNotifications &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            askedForNotifications = true
            pending = index
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            start(index)
        }
    }
}
