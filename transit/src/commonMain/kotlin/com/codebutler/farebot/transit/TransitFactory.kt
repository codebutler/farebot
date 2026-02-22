package com.codebutler.farebot.transit

import com.codebutler.farebot.card.Card

interface TransitFactory<C : Card, T : TransitInfo> {
    val allCards: List<CardInfo>

    fun check(card: C): Boolean

    fun parseIdentity(card: C): TransitIdentity

    fun parseInfo(card: C): T

    /**
     * Returns the [CardInfo] for the specific card variant detected.
     * Factories with multiple card variants (e.g. Suica/PASMO/ICOCA) should override this
     * to return the correct variant. Default returns the first entry in [allCards].
     */
    fun findCardInfo(card: C): CardInfo? = allCards.firstOrNull()
}
