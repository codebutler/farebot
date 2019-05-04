package com.codebutler.farebot.persist.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codebutler.farebot.card.CardType
import java.util.Date

@Entity(tableName = "keys")
data class SavedKey(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long? = 0,
    @ColumnInfo(name = "card_id") val cardId: String,
    @ColumnInfo(name = "card_type") val cardType: CardType,
    @ColumnInfo(name = "key_data") val keyData: String,
    @ColumnInfo(name = "created_at") val createdAt: Date = Date()
)
