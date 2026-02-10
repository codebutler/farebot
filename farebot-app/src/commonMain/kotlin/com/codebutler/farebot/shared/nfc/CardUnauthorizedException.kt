package com.codebutler.farebot.shared.nfc

import com.codebutler.farebot.card.CardType

class CardUnauthorizedException(
    val tagId: ByteArray,
    val cardType: CardType,
) : Throwable("Unauthorized")
