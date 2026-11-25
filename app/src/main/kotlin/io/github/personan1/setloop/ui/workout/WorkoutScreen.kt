package io.github.personan1.setloop.ui.workout

import android.content.Context
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.personan1.setloop.R
import io.github.personan1.setloop.ui.components.WithMaxFontScale
import io.github.personan1.setloop.ui.components.spokenDuration
import io.github.personan1.setloop.ui.theme.LocalAppSettings
import io.github.personan1.setloop.ui.theme.TimerTextStyles
import io.github.personan1.setloop.ui.theme.toColor
import io.github.personan1.setloop.workout.ElapsedRealtimeClock
import io.github.personan1.setloop.workout.WorkoutFormat
import io.github.personan1.setloop.workout.WorkoutTexts
import io.github.personan1.setloop.workout.WorkoutUiState
import kotlin.time.Duration
import kotlinx.coroutines.delay

private val WorkoutOverlay = Color.White.copy(alpha = 0.18f)
private val RingTrack = Color.White.copy(alpha = 0.25f)
private val RingStroke = 10.dp

/** Widest the countdown font may grow with the system font scale. */
private const val DIGITS_MAX_FONT_SCALE = 1.15f

/** Digit string "10:00" is about this many em wide. */
private const val DIGITS_WIDTH_EM = 2.65f

private const val REDUCED_MOTION_TICK_MS = 250L

/**
 * Full-screen workout (DESIGN V1): step color fills the screen, white text and controls in light and dark.
 * Status bar icons are made white by [io.github.personan1.setloop.ui.SetLoopApp] while this is shown.
 */
@Composable
fun WorkoutScreen(
    state: WorkoutUiState.Active,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotion()
    val now = rememberWorkoutNow(state, reduceMotion)
    val keepScreenOn = LocalAppSettings.current.keepScreenOn
    KeepScreenOn(keepScreenOn)

    // Back leaves the workout running in the notification; Finish needs no confirmation because the
    // summary still lets the user discard the result (SPEC D3).
    val activity = LocalActivity.current
    BackHandler { activity?.moveTaskToBack(true) }

    val stepColor = state.color.toColor()
    val background by animateColorAsState(
        targetValue = stepColor,
        animationSpec = if (reduceMotion) tween(0) else tween(300),
        label = "workoutBackground",
    )
    val actions = WorkoutActions(
        onTogglePause = onTogglePause,
        onSkip = onSkip,
        onBack = onBack,
        onFinish = onFinish,
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(background)) {
        val screenWidth = maxWidth
        val landscape = maxWidth > maxHeight
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val viewport = maxHeight
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = viewport).padding(horizontal = 24.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (landscape) {
                            LandscapeContent(state, now, actions, stepColor, maxHeight = viewport, maxWidth = screenWidth)
                        } else {
                            PortraitContent(state, now, actions, stepColor, maxHeight = viewport, maxWidth = screenWidth)
                        }
                    }
                }
            }
        }
    }
}

private class WorkoutActions(
    val onTogglePause: () -> Unit,
    val onSkip: () -> Unit,
    val onBack: () -> Unit,
    val onFinish: () -> Unit,
)

@Composable
private fun PortraitContent(
    state: WorkoutUiState.Active,
    now: State<Duration>,
    actions: WorkoutActions,
    stepColor: Color,
    maxHeight: Dp,
    maxWidth: Dp,
) {
    val ringSize = minOf(maxWidth - 48.dp, 340.dp, maxHeight * 0.42f).coerceAtLeast(160.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepTitle(state.step.step.name)
        CountdownRing(state, now, ringSize)
        StepInfo(state)
        Controls(state.isPaused, stepColor, actions)
        FinishButton(actions.onFinish, modifier = Modifier.padding(top = 8.dp))
    }
}

/** The phone is too low for one column: the countdown on the left, the rest on the right. */
@Composable
private fun LandscapeContent(
    state: WorkoutUiState.Active,
    now: State<Duration>,
    actions: WorkoutActions,
    stepColor: Color,
    maxHeight: Dp,
    maxWidth: Dp,
) {
    val ringSize = minOf(maxHeight - 32.dp, maxWidth * 0.4f, 300.dp).coerceAtLeast(140.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            CountdownRing(state, now, ringSize)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepTitle(state.step.step.name)
            StepInfo(state)
            Controls(state.isPaused, stepColor, actions)
            FinishButton(actions.onFinish)
        }
    }
}

@Composable
private fun StepTitle(name: String) {
    Text(
        text = name,
        style = TimerTextStyles.workoutTitle,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Cycle/set and the next step. Both lines keep their height when empty, so the controls never jump. */
@Composable
private fun StepInfo(state: WorkoutUiState.Active) {
    val texts = WorkoutTexts(LocalContext.current)
    val position = texts.position(state.step).ifEmpty { " " }
    val next = state.next?.let(texts::next).orEmpty().ifEmpty { " " }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(position, style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center)
        Text(next, style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
    }
}

/** White ring that drains as the step runs, with the rounded tabular countdown inside. */
@Composable
private fun CountdownRing(state: WorkoutUiState.Active, now: State<Duration>, size: Dp) {
    val remainingSeconds by remember(state) {
        derivedStateOf { wholeSecondsLeft(state.remaining(now.value)) }
    }
    val stepSeconds = state.step.duration.coerceAtLeast(1)
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = RingStroke.toPx()
            val inset = stroke / 2
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(RingTrack, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arc, style = Stroke(stroke))
            val left = state.remaining(now.value).inWholeMilliseconds / 1000f / stepSeconds
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * left.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arc,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        val description = spokenDuration(remainingSeconds)
        WithMaxFontScale(DIGITS_MAX_FONT_SCALE) {
            val fontScale = LocalDensity.current.fontScale
            // Never wider than the ring's inner circle allows, even at the largest font scale.
            val fitSp = size.value * 0.8f / DIGITS_WIDTH_EM / fontScale
            Text(
                text = WorkoutFormat.time(remainingSeconds),
                modifier = Modifier.semantics { contentDescription = description },
                style = TimerTextStyles.workoutDigits.copy(
                    fontSize = minOf(96f, fitSp).sp,
                    lineHeight = (minOf(96f, fitSp) * 1.08f).sp,
                ),
                color = Color.White,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun Controls(isPaused: Boolean, stepColor: Color, actions: WorkoutActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundControl(Icons.Filled.SkipPrevious, stringResource(R.string.action_back), actions.onBack)
        Surface(
            onClick = actions.onTogglePause,
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color = Color.White,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = stringResource(if (isPaused) R.string.action_resume else R.string.action_pause),
                    tint = stepColor,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        RoundControl(Icons.Filled.SkipNext, stringResource(R.string.action_skip), actions.onSkip)
    }
}

@Composable
private fun RoundControl(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(64.dp), shape = CircleShape, color = WorkoutOverlay) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun FinishButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = CircleShape, color = WorkoutOverlay) {
        Box(modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 36.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.action_finish),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

/** Rounds up, so the digits show 0:01 until the step really ends. */
internal fun wholeSecondsLeft(remaining: Duration): Int =
    ((remaining.inWholeMilliseconds + 999) / 1000).toInt().coerceAtLeast(0)

/**
 * The current time on the workout clock, ticking while the screen is visible and the workout runs:
 * every frame, or four times a second when animations are turned off in the system settings.
 */
@Composable
private fun rememberWorkoutNow(state: WorkoutUiState.Active, reduceMotion: Boolean): State<Duration> {
    val now = remember { mutableStateOf(ElapsedRealtimeClock.now()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, reduceMotion, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            now.value = ElapsedRealtimeClock.now()
            while (!state.isPaused) {
                if (reduceMotion) delay(REDUCED_MOTION_TICK_MS) else withFrameNanos { }
                now.value = ElapsedRealtimeClock.now()
            }
        }
    }
    return now
}

/** True when the user turned animations off (animator duration scale 0). */
@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { isReduceMotionEnabled(context) }
}

internal fun isReduceMotionEnabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
