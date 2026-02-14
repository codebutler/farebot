package com.codebutler.farebot.transit

import com.codebutler.farebot.card.Card

interface TransitFactory<C : Card, T : TransitInfo> {
    val allCards: List<CardInfo>

    fun check(card: C): Boolean

    fun parseIdentity(card: C): TransitIdentity

    fun parseInfo(card: C): T
}
