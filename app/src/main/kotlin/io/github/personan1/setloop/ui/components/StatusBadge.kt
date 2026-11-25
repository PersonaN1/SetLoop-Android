package io.github.personan1.setloop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.personan1.setloop.R
import io.github.personan1.setloop.core.WorkoutStatus
import io.github.personan1.setloop.ui.theme.LocalAppSettings
import io.github.personan1.setloop.ui.theme.isDarkTheme

/** Green for a finished workout, orange for one finished early (iOS `StatusBadge`). */
@Composable
fun WorkoutStatus.accentColor(): Color {
    val dark = isDarkTheme(LocalAppSettings.current.theme)
    return when (this) {
        WorkoutStatus.Complete -> if (dark) Color(0xFF5FD08A) else Color(0xFF1E8E52)
        WorkoutStatus.Partial -> if (dark) Color(0xFFFFA94D) else Color(0xFFC76A00)
    }
}

@Composable
fun WorkoutStatus.label(): String = stringResource(
    if (this == WorkoutStatus.Complete) R.string.record_status_complete else R.string.record_status_partial,
)

@Composable
fun StatusBadge(status: WorkoutStatus, modifier: Modifier = Modifier) {
    val accent = status.accentColor()
    Text(
        text = status.label(),
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = accent,
        modifier = modifier
            .background(accent.copy(alpha = 0.15f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
