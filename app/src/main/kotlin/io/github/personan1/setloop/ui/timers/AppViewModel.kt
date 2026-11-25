package io.github.personan1.setloop.ui.timers

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.personan1.setloop.data.AppContainer
import io.github.personan1.setloop.ui.LocalAppContainer

/** A `ViewModel` of the current navigation entry, built from the app container (manual DI, no Hilt). */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = LocalAppContainer.current
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}
