package com.codebutler.farebot.persist.db

import androidx.room.TypeConverter
import com.codebutler.farebot.card.CardType
import java.util.Date

@Suppress("unused")
class FareBotDbConverters {
    @TypeConverter
    fun cardTypeToString(cardType: CardType) = cardType.name

    @TypeConverter
    fun stringToCardType(cardTypeName: String) = CardType.valueOf(cardTypeName)

    @TypeConverter
    fun dateToLong(date: Date) = date.time

    @TypeConverter
    fun longToDate(dateValue: Long) = Date(dateValue)
}
