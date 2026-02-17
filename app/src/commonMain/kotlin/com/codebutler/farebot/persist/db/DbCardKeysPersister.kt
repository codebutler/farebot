package com.codebutler.farebot.persist.db

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.db.model.SavedKey
import kotlin.time.Clock
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

    override fun getGlobalKeys(): List<ByteArray> =
        db.savedKeyQueries
            .selectAllGlobalKeys()
            .executeAsList()
            .map { hexToBytes(it.key_data) }

    override fun insertGlobalKeys(
        keys: List<ByteArray>,
        source: String,
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        keys.forEach { key ->
            db.savedKeyQueries.insertGlobalKey(
                key_data = bytesToHex(key),
                source = source,
                created_at = now,
            )
        }
    }

    override fun deleteAllGlobalKeys() {
        db.savedKeyQueries.deleteAllGlobalKeys()
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

@OptIn(ExperimentalStdlibApi::class)
private fun bytesToHex(bytes: ByteArray): String = bytes.toHexString()

@OptIn(ExperimentalStdlibApi::class)
private fun hexToBytes(hex: String): ByteArray = hex.hexToByteArray()
