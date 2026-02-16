package com.codebutler.farebot.web

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.shared.FareBotApp
import com.codebutler.farebot.shared.di.LocalAppGraph
import dev.zacsweers.metro.createGraph
import kotlinx.browser.document

@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Ensure js-joda timezone database is loaded before any kotlinx-datetime TimeZone usage
    JsJodaTimeZoneModule
    val graph = createGraph<WebAppGraph>()
    ComposeViewport(document.body!!) {
        CompositionLocalProvider(LocalAppGraph provides graph) {
            FareBotApp(
                platformActions = WebPlatformActions(),
                supportedCardTypes = CardType.entries.toSet(),
                isDebug = true,
            )
        }
    }
}
