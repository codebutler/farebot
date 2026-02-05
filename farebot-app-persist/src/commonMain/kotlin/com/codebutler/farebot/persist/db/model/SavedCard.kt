package com.codebutler.farebot.persist.db.model

import com.codebutler.farebot.card.CardType
import kotlin.time.Clock
import kotlin.time.Instant

data class SavedCard(
    val id: Long = 0,
    val type: CardType,
    val serial: String,
    val data: String,
    val scannedAt: Instant = Clock.System.now()
)
