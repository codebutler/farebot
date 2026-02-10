package com.codebutler.farebot.persist.db.model

import com.codebutler.farebot.card.CardType
import kotlin.time.Clock
import kotlin.time.Instant

data class SavedKey(
    val id: Long = 0,
    val cardId: String,
    val cardType: CardType,
    val keyData: String,
    val createdAt: Instant = Clock.System.now()
)
