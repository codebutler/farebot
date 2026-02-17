@file:OptIn(ExperimentalWasmJsInterop::class)

package com.codebutler.farebot.web

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.model.SavedCard
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
private data class SerializableSavedCard(
    val id: Long,
    val type: String,
    val serial: String,
    val data: String,
    val scannedAtEpochMillis: Long,
)

private fun SavedCard.toSerializable() =
    SerializableSavedCard(
        id = id,
        type = type.name,
        serial = serial,
        data = data,
        scannedAtEpochMillis = scannedAt.toEpochMilliseconds(),
    )

private fun SerializableSavedCard.toSavedCard() =
    SavedCard(
        id = id,
        type = CardType.valueOf(type),
        serial = serial,
        data = data,
        scannedAt = Instant.fromEpochMilliseconds(scannedAtEpochMillis),
    )

private fun lsGetItem(key: JsString): JsString? = js("localStorage.getItem(key)")

private fun lsSetItem(
    key: JsString,
    value: JsString,
) {
    js("localStorage.setItem(key, value)")
}

class LocalStorageCardPersister(
    private val json: Json,
) : CardPersister {
    private companion object {
        const val STORAGE_KEY = "farebot_cards"
    }

    override fun getCards(): List<SavedCard> = loadCards()

    override fun getCard(id: Long): SavedCard? = loadCards().find { it.id == id }

    override fun insertCard(card: SavedCard): Long {
        val cards = loadCards().toMutableList()
        val nextId =
            if (card.id == 0L) {
                (cards.maxOfOrNull { it.id } ?: 0L) + 1L
            } else {
                card.id
            }
        val newCard =
            card.copy(
                id = nextId,
                scannedAt = if (card.scannedAt == Instant.DISTANT_PAST) Clock.System.now() else card.scannedAt,
            )
        cards.add(newCard)
        saveCards(cards)
        return nextId
    }

    override fun deleteCard(card: SavedCard) {
        val cards = loadCards().toMutableList()
        cards.removeAll { it.id == card.id }
        saveCards(cards)
    }

    private fun loadCards(): List<SavedCard> {
        val raw = lsGetItem(STORAGE_KEY.toJsString())?.toString() ?: return emptyList()
        return try {
            json.decodeFromString<List<SerializableSavedCard>>(raw).map { it.toSavedCard() }
        } catch (e: Exception) {
            println("[LocalStorage] Failed to load saved cards: $e")
            emptyList()
        }
    }

    private fun saveCards(cards: List<SavedCard>) {
        val serialized = json.encodeToString<List<SerializableSavedCard>>(cards.map { it.toSerializable() })
        lsSetItem(STORAGE_KEY.toJsString(), serialized.toJsString())
    }
}
