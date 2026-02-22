package com.codebutler.farebot.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.shared.FareBotApp
import com.codebutler.farebot.shared.di.LocalAppGraph
import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.flow.MutableSharedFlow
import java.awt.Desktop
import java.awt.Taskbar
import java.net.URI
import javax.imageio.ImageIO

private const val ICON_PATH = "composeResources/farebot.app.generated.resources/drawable/ic_launcher.png"

/**
 * Preload the bundled libusb before usb4java initializes.
 *
 * usb4java's libusb4java.dylib links against /opt/local/lib/libusb-1.0.0.dylib.
 * We bundle a copy with its install name patched to match that path. By loading it
 * into the process first, dyld finds the already-loaded image (matched by install
 * name) when processing libusb4java's dependency — no external libusb required.
 */
private fun preloadBundledLibusb() {
    try {
        val stream =
            Thread
                .currentThread()
                .contextClassLoader
                .getResourceAsStream("native/libusb-1.0.0.dylib") ?: return
        val tmpFile = java.io.File.createTempFile("libusb-1.0", ".dylib")
        tmpFile.deleteOnExit()
        stream.use { input ->
            java.io.FileOutputStream(tmpFile).use { output ->
                input.copyTo(output)
            }
        }
        System.load(tmpFile.absolutePath)
    } catch (e: Throwable) {
        System.err.println("[FareBot] Could not preload bundled libusb: ${e.message}")
    }
}

fun main() {
    preloadBundledLibusb()
    System.setProperty("apple.awt.application.appearance", "system")

    val desktop = Desktop.getDesktop()
    if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
        desktop.setAboutHandler { Desktop.getDesktop().browse(URI("https://codebutler.github.io/farebot")) }
    }

    if (Taskbar.isTaskbarSupported()) {
        val taskbar = Taskbar.getTaskbar()
        if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
            val iconStream = Thread.currentThread().contextClassLoader.getResourceAsStream(ICON_PATH)
            if (iconStream != null) {
                taskbar.iconImage = ImageIO.read(iconStream)
            }
        }
    }

    application {
        val graph = createGraph<DesktopAppGraph>()
        val menuEvents = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }
        var selectedTab by remember { mutableIntStateOf(0) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "FareBot",
            icon = painterResource(ICON_PATH),
            state = rememberWindowState(width = 1024.dp, height = 768.dp),
        ) {
            if (System.getProperty("os.name").lowercase().contains("mac")) {
                MenuBar {
                    Menu("File") {
                        Item(
                            "Import\u2026",
                            shortcut = KeyShortcut(Key.O, meta = true),
                            onClick = { menuEvents.tryEmit("import") },
                        )
                        Item(
                            "Keys",
                            shortcut = KeyShortcut(Key.K, meta = true),
                            onClick = { menuEvents.tryEmit("keys") },
                        )
                    }
                    Menu("View") {
                        RadioButtonItem(
                            "Cards",
                            selected = selectedTab == 0,
                            shortcut = KeyShortcut(Key.One, meta = true),
                            onClick = {
                                selectedTab = 0
                                menuEvents.tryEmit("tab:cards")
                            },
                        )
                        RadioButtonItem(
                            "Explore",
                            selected = selectedTab == 1,
                            shortcut = KeyShortcut(Key.Two, meta = true),
                            onClick = {
                                selectedTab = 1
                                menuEvents.tryEmit("tab:explore")
                            },
                        )
                    }
                }
            }
            CompositionLocalProvider(LocalAppGraph provides graph) {
                FareBotApp(
                    platformActions = DesktopPlatformActions(),
                    supportedCardTypes = CardType.entries.toSet() - setOf(CardType.Vicinity),
                    isDebug = true,
                    menuEvents = menuEvents,
                    onSelectedTabChanged = { selectedTab = it },
                )
            }
        }
    }
}
