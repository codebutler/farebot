/*
 * ExportHelper.kt
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

package com.codebutler.farebot.app.core.util

import com.codebutler.farebot.BuildConfig
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.model.SavedCard
import com.google.gson.Gson

class ExportHelper(
    private val cardPersister: CardPersister,
    private val cardSerializer: CardSerializer,
    private val gson: Gson
) {

    fun exportCards(): String = gson.toJson(Export(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            cards = cardPersister.cards.map { cardSerializer.deserialize(it.data) }
    ))

    fun importCards(exportJsonString: String): List<Long> = gson.fromJson(exportJsonString, Export::class.java)
            .cards.map { cardPersister.insertCard(SavedCard(
            type = it.cardType(),
            serial = it.tagId().hex(),
            data = cardSerializer.serialize(it)))
    }

    private data class Export(
        internal val versionName: String,
        internal val versionCode: Int,
        internal val cards: List<RawCard<*>>
    )
}
