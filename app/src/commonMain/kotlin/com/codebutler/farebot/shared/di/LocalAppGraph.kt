package com.codebutler.farebot.shared.di

import androidx.compose.runtime.compositionLocalOf

val LocalAppGraph = compositionLocalOf<AppGraph> { error("No AppGraph provided") }
