/*
 * GsonCardKeysSerializer.kt
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

import com.codebutler.farebot.app.core.serialize.CardKeysSerializer
import com.codebutler.farebot.key.CardKeys
import com.google.gson.Gson

class GsonCardKeysSerializer(private val gson: Gson) : CardKeysSerializer {

    override fun serialize(cardKeys: CardKeys): String {
        return gson.toJson(cardKeys)
    }

    override fun deserialize(data: String): CardKeys {
        return gson.fromJson(data, CardKeys::class.java)
    }
}
