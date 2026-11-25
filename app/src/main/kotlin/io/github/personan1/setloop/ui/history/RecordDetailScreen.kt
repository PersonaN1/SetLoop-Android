package io.github.personan1.setloop.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.personan1.setloop.R
import io.github.personan1.setloop.ui.components.PlaceholderScreen
import io.github.personan1.setloop.ui.navigation.AppNavigator

@Composable
fun RecordDetailScreen(recordId: String, navigator: AppNavigator) {
    PlaceholderScreen(stringResource(R.string.title_workout), onBack = navigator::back)
}
