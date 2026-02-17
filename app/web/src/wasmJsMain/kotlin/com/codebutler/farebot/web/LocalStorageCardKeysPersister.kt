@file:OptIn(ExperimentalWasmJsInterop::class)

package com.codebutler.farebot.web

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.db.model.SavedKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
private data class SerializableSavedKey(
    val id: Long,
    val cardId: String,
    val cardType: String,
    val keyData: String,
    val createdAtEpochMillis: Long,
)

private fun SavedKey.toSerializable() =
    SerializableSavedKey(
        id = id,
        cardId = cardId,
        cardType = cardType.name,
        keyData = keyData,
        createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    )

private fun SerializableSavedKey.toSavedKey() =
    SavedKey(
        id = id,
        cardId = cardId,
        cardType = CardType.valueOf(cardType),
        keyData = keyData,
        createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
    )

private fun lsGetItem(key: JsString): JsString? = js("localStorage.getItem(key)")

private fun lsSetItem(
    key: JsString,
    value: JsString,
) {
    js("localStorage.setItem(key, value)")
}

class LocalStorageCardKeysPersister(
    private val json: Json,
) : CardKeysPersister {
    private companion object {
        const val STORAGE_KEY = "farebot_keys"
        const val GLOBAL_KEYS_STORAGE_KEY = "farebot_global_keys"
    }

    override fun getSavedKeys(): List<SavedKey> = loadKeys()

    override fun getForTagId(tagId: String): SavedKey? = loadKeys().find { it.cardId == tagId }

    override fun insert(savedKey: SavedKey): Long {
        val keys = loadKeys().toMutableList()
        val nextId =
            if (savedKey.id == 0L) {
                (keys.maxOfOrNull { it.id } ?: 0L) + 1L
            } else {
                savedKey.id
            }
        val newKey =
            savedKey.copy(
                id = nextId,
                createdAt = if (savedKey.createdAt == Instant.DISTANT_PAST) Clock.System.now() else savedKey.createdAt,
            )
        keys.add(newKey)
        saveKeys(keys)
        return nextId
    }

    override fun delete(savedKey: SavedKey) {
        val keys = loadKeys().toMutableList()
        keys.removeAll { it.id == savedKey.id }
        saveKeys(keys)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getGlobalKeys(): List<ByteArray> {
        val raw = lsGetItem(GLOBAL_KEYS_STORAGE_KEY.toJsString())?.toString() ?: return emptyList()
        return try {
            json.decodeFromString<List<String>>(raw).map { it.hexToByteArray() }
        } catch (e: Exception) {
            println("[LocalStorage] Failed to load global keys: $e")
            emptyList()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun insertGlobalKeys(keys: List<ByteArray>, source: String) {
        val existing = getGlobalKeys().map { it.toHexString() }.toMutableSet()
        keys.forEach { existing.add(it.toHexString()) }
        val serialized = json.encodeToString<List<String>>(existing.toList())
        lsSetItem(GLOBAL_KEYS_STORAGE_KEY.toJsString(), serialized.toJsString())
    }

    override fun deleteAllGlobalKeys() {
        lsSetItem(GLOBAL_KEYS_STORAGE_KEY.toJsString(), "[]".toJsString())
    }

    private fun loadKeys(): List<SavedKey> {
        val raw = lsGetItem(STORAGE_KEY.toJsString())?.toString() ?: return emptyList()
        return try {
            json.decodeFromString<List<SerializableSavedKey>>(raw).map { it.toSavedKey() }
        } catch (e: Exception) {
            println("[LocalStorage] Failed to load saved keys: $e")
            emptyList()
        }
    }

    private fun saveKeys(keys: List<SavedKey>) {
        val serialized = json.encodeToString<List<SerializableSavedKey>>(keys.map { it.toSerializable() })
        lsSetItem(STORAGE_KEY.toJsString(), serialized.toJsString())
    }
}
