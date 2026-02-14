package com.codebutler.farebot.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.shared.FareBotApp
import com.codebutler.farebot.shared.di.sharedModule
import org.koin.core.context.startKoin

fun main() =
    application {
        startKoin {
            modules(sharedModule, desktopModule)
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "FareBot",
            state = rememberWindowState(width = 420.dp, height = 800.dp),
        ) {
            FareBotApp(
                platformActions = DesktopPlatformActions(),
                supportedCardTypes = CardType.entries.toSet() - setOf(CardType.Vicinity),
                isDebug = true,
            )
        }
    }
