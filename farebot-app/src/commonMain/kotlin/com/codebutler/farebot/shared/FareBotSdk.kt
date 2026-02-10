package com.codebutler.farebot.shared

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.transit.TransitInfo

/**
 * Main entry point for the FareBot SDK.
 *
 * This object provides access to supported card types and transit system
 * information from Kotlin Multiplatform consumers (iOS, Android).
 */
object FareBotSdk {
    /**
     * Returns the list of supported card types.
     */
    fun supportedCardTypes(): List<CardType> = CardType.entries

    /**
     * SDK version string.
     */
    const val VERSION = "1.0.0"
}
