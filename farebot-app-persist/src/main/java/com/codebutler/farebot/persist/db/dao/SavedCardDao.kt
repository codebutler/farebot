package com.codebutler.farebot.persist.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.codebutler.farebot.persist.db.model.SavedCard

@Dao
interface SavedCardDao {
    @Query("SELECT * FROM cards ORDER BY scanned_at DESC")
    fun selectAll(): List<SavedCard>

    @Query("SELECT * FROM cards WHERE _id = :id")
    fun selectById(id: Long): SavedCard?

    @Insert
    fun insert(savedCard: SavedCard): Long

    @Delete
    fun delete(savedCard: SavedCard)
}
