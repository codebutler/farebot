/*
 * KotlinxCardKeysSerializer.kt
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

package com.codebutler.farebot.app.core.serialize

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.key.ClassicCardKeys
import com.codebutler.farebot.key.CardKeys
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KotlinxCardKeysSerializer(private val json: Json) : CardKeysSerializer {

    override fun serialize(cardKeys: CardKeys): String {
        return when (cardKeys.cardType()) {
            CardType.MifareClassic -> json.encodeToString(
                ClassicCardKeys.serializer(),
                cardKeys as ClassicCardKeys
            )
            else -> throw IllegalArgumentException("Unknown card keys type: ${cardKeys.cardType()}")
        }
    }

    override fun deserialize(data: String): CardKeys {
        val jsonObject = json.decodeFromString(JsonObject.serializer(), data)
        val cardTypeName = jsonObject["cardType"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing cardType field")
        val cardType = CardType.valueOf(cardTypeName)
        return when (cardType) {
            CardType.MifareClassic -> json.decodeFromString(ClassicCardKeys.serializer(), data)
            else -> throw IllegalArgumentException("Unknown card keys type: $cardType")
        }
    }
}
