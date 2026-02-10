package com.codebutler.farebot.persist.db

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.model.SavedCard
import kotlin.time.Instant

class DbCardPersister(private val db: FareBotDb) : CardPersister {

    override fun getCards(): List<SavedCard> =
        db.savedCardQueries.selectAll().executeAsList().map { it.toSavedCard() }

    override fun getCard(id: Long): SavedCard? =
        db.savedCardQueries.selectById(id).executeAsOneOrNull()?.toSavedCard()

    override fun insertCard(card: SavedCard): Long {
        db.savedCardQueries.insert(
            type = card.type.name,
            serial = card.serial,
            data_ = card.data,
            scanned_at = card.scannedAt.toEpochMilliseconds()
        )
        return db.savedCardQueries.selectAll().executeAsList().firstOrNull()?.id ?: -1
    }

    override fun deleteCard(card: SavedCard) {
        db.savedCardQueries.deleteById(card.id)
    }
}

private fun Cards.toSavedCard() = SavedCard(
    id = id,
    type = CardType.valueOf(type),
    serial = serial,
    data = data_,
    scannedAt = Instant.fromEpochMilliseconds(scanned_at)
)
