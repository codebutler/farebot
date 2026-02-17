package com.codebutler.farebot.persist

import com.codebutler.farebot.persist.db.model.SavedKey

interface CardKeysPersister {
    fun getSavedKeys(): List<SavedKey>

    fun getForTagId(tagId: String): SavedKey?

    fun insert(savedKey: SavedKey): Long

    fun delete(savedKey: SavedKey)

    fun getGlobalKeys(): List<ByteArray>

    fun insertGlobalKeys(keys: List<ByteArray>, source: String)

    fun deleteAllGlobalKeys()
}
