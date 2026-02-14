package com.codebutler.farebot.persist.db

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.db.model.SavedKey
import kotlin.time.Instant

class DbCardKeysPersister(
    private val db: FareBotDb,
) : CardKeysPersister {
    override fun getSavedKeys(): List<SavedKey> =
        db.savedKeyQueries
            .selectAll()
            .executeAsList()
            .map { it.toSavedKey() }

    override fun getForTagId(tagId: String): SavedKey? =
        db.savedKeyQueries
            .selectByCardId(tagId)
            .executeAsOneOrNull()
            ?.toSavedKey()

    override fun insert(savedKey: SavedKey): Long {
        db.savedKeyQueries.insert(
            card_id = savedKey.cardId,
            card_type = savedKey.cardType.name,
            key_data = savedKey.keyData,
            created_at = savedKey.createdAt.toEpochMilliseconds(),
        )
        return db.savedKeyQueries
            .selectAll()
            .executeAsList()
            .firstOrNull()
            ?.id ?: -1
    }

    override fun delete(savedKey: SavedKey) {
        db.savedKeyQueries.deleteById(savedKey.id)
    }
}

private fun Keys.toSavedKey() =
    SavedKey(
        id = id,
        cardId = card_id,
        cardType = CardType.valueOf(card_type),
        keyData = key_data,
        createdAt = Instant.fromEpochMilliseconds(created_at),
    )
