package com.codebutler.farebot.shared.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
inline fun <reified VM : ViewModel> graphViewModel(
    key: String? = null,
    crossinline factory: AppGraph.() -> VM,
): VM {
    val graph = LocalAppGraph.current
    return viewModel(key = key) { graph.factory() }
}
