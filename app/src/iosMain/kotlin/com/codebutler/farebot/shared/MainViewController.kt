package com.codebutler.farebot.shared

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.flipper.FlipperDebugLog
import com.codebutler.farebot.shared.di.IosAppGraph
import com.codebutler.farebot.shared.di.LocalAppGraph
import com.codebutler.farebot.shared.platform.IosPlatformActions
import dev.zacsweers.metro.createGraph
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

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

@OptIn(ExperimentalForeignApi::class)
fun initGraph() {
    iosGraph = createGraph<IosAppGraph>()

    // Write debug log to Documents/flipper_debug.log for retrieval via devicectl
    @Suppress("UNCHECKED_CAST")
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true) as List<String>
    val docsDir = paths.firstOrNull() ?: return
    val logPath = "$docsDir/flipper_debug.log"

    FlipperDebugLog.fileWriter = { line ->
        val fm = NSFileManager.defaultManager
        val existing =
            if (fm.fileExistsAtPath(logPath)) {
                NSString
                    .create(
                        contentsOfFile = logPath,
                        encoding = NSUTF8StringEncoding,
                        error = null,
                    )?.toString() ?: ""
            } else {
                ""
            }
        val updated = "$existing$line\n"
        (updated as NSString).writeToFile(logPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }
}
