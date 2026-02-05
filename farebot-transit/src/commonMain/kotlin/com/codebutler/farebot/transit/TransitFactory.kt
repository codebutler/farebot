package com.codebutler.farebot.transit

import com.codebutler.farebot.card.Card

interface TransitFactory<C : Card, T : TransitInfo> {

    /**
     * List of cards that this factory can handle.
     *
     * This is used to populate the "Supported Cards" screen and provide
     * metadata about each supported transit system.
     *
     * Most factories return a single CardInfo, but some (like catch-all
     * handlers or multi-card factories) may return an empty list or
     * multiple cards.
     */
    val allCards: List<CardInfo>
        get() = emptyList()

    fun check(card: C): Boolean

    fun parseIdentity(card: C): TransitIdentity

    fun parseInfo(card: C): T
}
