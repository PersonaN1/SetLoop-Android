package io.github.personan1.setloop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.SoundSet
import io.github.personan1.setloop.data.AppSettings
import io.github.personan1.setloop.data.AppSettingsValues
import io.github.personan1.setloop.data.AppTheme
import io.github.personan1.setloop.ui.LocalAppContainer
import io.github.personan1.setloop.ui.components.TitledScreenScaffold
import io.github.personan1.setloop.ui.navigation.AppNavigator
import io.github.personan1.setloop.ui.navigation.TopLevelTab
import io.github.personan1.setloop.ui.tips.rememberTips
import io.github.personan1.setloop.ui.theme.LocalAppSettings
import io.github.personan1.setloop.workout.SoundPreviewPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Groups: Workout / Sound and vibration / Appearance / About (SPEC D22, DESIGN V8). Every change is saved at once. */
@Composable
fun SettingsScreen(navigator: AppNavigator) {
    val settings = LocalAppSettings.current
    val store = LocalAppContainer.current.settings
    val scope = rememberCoroutineScope()
    val save: Launcher = { block -> scope.launch { block() } }
    val preview = remember { SoundPreviewPlayer(scope) }
    val tips = rememberTips()
    DisposableEffect(preview) { onDispose(preview::release) }

    TitledScreenScaffold(title = stringResource(R.string.tab_settings), largeTitle = true) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                WorkoutGroup(settings, store, save)
                SoundGroup(settings, store, preview, save)
                AppearanceGroup(settings, store, navigator, save)
                AboutGroup(onShowTipsAgain = {
                    tips.showAgain()
                    navigator.openTab(TopLevelTab.Timers)
                })
            }
        }
    }
}

private typealias Launcher = (suspend () -> Unit) -> Unit

@Composable
private fun WorkoutGroup(settings: AppSettingsValues, store: AppSettings, save: Launcher) {
    SettingsGroup(title = stringResource(R.string.settings_group_workout)) {
        SwitchRow(stringResource(R.string.settings_pause_on_calls), settings.pauseOnInterruption) {
            save { store.setPauseOnInterruption(it) }
        }
        RowDivider()
        SwitchRow(stringResource(R.string.settings_show_on_lock_screen), settings.showOnLockScreen) {
            save { store.setShowOnLockScreen(it) }
        }
        RowDivider()
        SwitchRow(stringResource(R.string.settings_keep_screen_on), settings.keepScreenOn) {
            save { store.setKeepScreenOn(it) }
        }
    }
}

@Composable
private fun SoundGroup(settings: AppSettingsValues, store: AppSettings, preview: SoundPreviewPlayer, save: Launcher) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val note = SoundNote.of(settings)
    SettingsGroup(
        title = stringResource(R.string.settings_group_sound),
        footer = note?.let { stringResource(it.text) },
    ) {
        ValueRow(
            title = stringResource(R.string.settings_sounds),
            value = stringResource(SoundChoice.of(settings.soundSet).label),
            onClick = { picking = true },
        )
        RowDivider()
        SwitchRow(stringResource(R.string.settings_voice_prompts), settings.voicePrompts) { save { store.setVoicePrompts(it) } }
        RowDivider()
        SwitchRow(stringResource(R.string.settings_vibration), settings.vibration) { save { store.setVibration(it) } }
        RowDivider()
        SwitchRow(stringResource(R.string.settings_ignore_silent_mode), settings.ignoreSilentMode) {
            save { store.setIgnoreSilentMode(it) }
        }
    }
    if (picking) {
        ChoiceDialog(
            title = stringResource(R.string.settings_sounds),
            options = SoundChoice.entries,
            selected = SoundChoice.of(settings.soundSet),
            label = { stringResource(it.label) },
            onSelect = { choice ->
                save { store.setSoundSet(choice.set) }
                choice.set?.let { preview.play(it, settings.ignoreSilentMode) }
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun AppearanceGroup(settings: AppSettingsValues, store: AppSettings, navigator: AppNavigator, save: Launcher) {
    var picking by rememberSaveable { mutableStateOf(false) }
    SettingsGroup(title = stringResource(R.string.settings_group_appearance)) {
        ValueRow(
            title = stringResource(R.string.settings_theme),
            value = stringResource(settings.theme.label),
            onClick = { picking = true },
        )
        RowDivider()
        ValueRow(title = stringResource(R.string.title_step_colors), value = null, onClick = navigator::openStepColors)
    }
    if (picking) {
        ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = AppTheme.entries,
            selected = settings.theme,
            label = { stringResource(it.label) },
            onSelect = { theme ->
                save { store.setTheme(theme) }
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun AboutGroup(onShowTipsAgain: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember(context) { AppVersion.of(context) }
    var showingLicenses by rememberSaveable { mutableStateOf(false) }
    SettingsGroup(title = stringResource(R.string.settings_group_about)) {
        LinkRow(stringResource(R.string.settings_telegram)) { uriHandler.openUri(SupportLinks.TELEGRAM) }
        RowDivider()
        LinkRow(stringResource(R.string.settings_support)) { uriHandler.openUri(SupportLinks.SUPPORT) }
        RowDivider()
        LinkRow(stringResource(R.string.settings_privacy)) { uriHandler.openUri(SupportLinks.PRIVACY) }
        RowDivider()
        ValueRow(stringResource(R.string.settings_show_tips_again), value = null, onClick = onShowTipsAgain)
        RowDivider()
        ValueRow(stringResource(R.string.settings_licenses), value = null, onClick = { showingLicenses = true })
        RowDivider()
        InfoRow(
            title = stringResource(R.string.settings_version),
            value = stringResource(R.string.settings_version_value, version.name, version.code),
        )
    }
    if (showingLicenses) LicensesDialog(onDismiss = { showingLicenses = false })
}

@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val license by produceState("") {
        value = withContext(Dispatchers.IO) {
            context.assets.open(NUNITO_LICENSE_ASSET).bufferedReader().use { it.readText() }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_licenses_title)) },
        text = {
            Text(
                license,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                overflow = TextOverflow.Clip,
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
    )
}
