package com.codebutler.farebot.shared

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.shared.di.IosAppGraph
import com.codebutler.farebot.shared.di.LocalAppGraph
import com.codebutler.farebot.shared.platform.IosPlatformActions
import dev.zacsweers.metro.createGraph

private lateinit var iosGraph: IosAppGraph

@Suppress("ktlint:standard:function-naming")
fun MainViewController() =
    ComposeUIViewController {
        val platformActions = remember { IosPlatformActions() }

        CompositionLocalProvider(LocalAppGraph provides iosGraph) {
            FareBotApp(
                platformActions = platformActions,
                supportedCardTypes = CardType.entries.toSet() - setOf(CardType.MifareClassic, CardType.Vicinity),
            )
        }
    }

fun handleImportedFileContent(content: String) {
    iosGraph.cardImporter.submitImport(content)
}

fun initGraph() {
    iosGraph = createGraph<IosAppGraph>()
}
