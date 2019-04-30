package com.codebutler.farebot.persist.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.codebutler.farebot.persist.db.model.SavedKey

@Dao
interface SavedKeyDao {
    @Query("SELECT * FROM keys ORDER BY created_at DESC")
    fun selectAll(): List<SavedKey>

    @Query("SELECT * FROM keys WHERE card_id = :cardId")
    fun selectByCardId(cardId: String): SavedKey?

    @Insert
    fun insert(savedKey: SavedKey): Long

    @Delete
    fun delete(savedKey: SavedKey)
}
