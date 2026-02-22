package com.codebutler.farebot.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.shared.FareBotApp
import com.codebutler.farebot.shared.di.LocalAppGraph
import dev.zacsweers.metro.createGraph

fun main() =
    application {
        val graph = createGraph<DesktopAppGraph>()

        Window(
            onCloseRequest = ::exitApplication,
            title = "FareBot",
            state = rememberWindowState(width = 1024.dp, height = 768.dp),
        ) {
            CompositionLocalProvider(LocalAppGraph provides graph) {
                FareBotApp(
                    platformActions = DesktopPlatformActions(),
                    supportedCardTypes = CardType.entries.toSet() - setOf(CardType.Vicinity),
                    isDebug = true,
                )
            }
        }
    }
