/*
 * CardKeysGsonTypeAdapterFactory.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.app.core.serialize.gson

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.key.ClassicCardKeys
import com.codebutler.farebot.key.CardKeys
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.internal.Streams
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.util.HashMap

class CardKeysGsonTypeAdapterFactory : TypeAdapterFactory {

    companion object {
        private val KEY_CARD_TYPE = "cardType"

        private val CLASSES = mapOf(
                CardType.MifareClassic to ClassicCardKeys::class.java
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!CardKeys::class.java.isAssignableFrom(type.rawType)) {
            return null
        }
        val delegates = HashMap<CardType, TypeAdapter<CardKeys>>()
        for ((key, _) in CLASSES) {
            val cardKeysType = object : TypeToken<CardKeys>() {}
            delegates.put(key, gson.getDelegateAdapter(this, cardKeysType))
        }
        return CardKeysTypeAdapter(delegates) as TypeAdapter<T>
    }

    private inner class CardKeysTypeAdapter internal constructor(
            private val delegates: Map<CardType, TypeAdapter<CardKeys>>) : TypeAdapter<CardKeys>() {

        override fun write(out: JsonWriter, value: CardKeys) {
            val delegateAdapter = delegates[value.cardType()]
                    ?: throw IllegalArgumentException("Unknown type: ${value.cardType()}")
            val jsonObject = delegateAdapter.toJsonTree(value).asJsonObject
            Streams.write(jsonObject, out)
        }

        override fun read(inJsonReader: JsonReader): CardKeys {
            val rootElement = Streams.parse(inJsonReader)
            val typeElement = rootElement.asJsonObject.remove(KEY_CARD_TYPE)
            val cardType = CardType.valueOf(typeElement.asString)
            val delegateAdapter = delegates[cardType]
                    ?: throw IllegalArgumentException("Unknown type: $cardType")
            return delegateAdapter.fromJsonTree(rootElement)
        }
    }
}
