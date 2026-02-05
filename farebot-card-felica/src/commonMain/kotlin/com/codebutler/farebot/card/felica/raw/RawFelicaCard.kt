/*
 * RawFelicaCard.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.felica.raw

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.felica.FeliCaIdm
import com.codebutler.farebot.card.felica.FeliCaPmm
import com.codebutler.farebot.card.felica.FelicaCard
import com.codebutler.farebot.card.felica.FelicaSystem
import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class RawFelicaCard(
    @Contextual private val _tagId: ByteArray,
    private val _scannedAt: Instant,
    @Contextual val idm: FeliCaIdm,
    @Contextual val pmm: FeliCaPmm,
    val systems: List<FelicaSystem>
) : RawCard<FelicaCard> {

    override fun cardType(): CardType = CardType.FeliCa

    override fun tagId(): ByteArray = _tagId

    override fun scannedAt(): Instant = _scannedAt

    override fun isUnauthorized(): Boolean = false

    override fun parse(): FelicaCard {
        return FelicaCard.create(_tagId, _scannedAt, idm, pmm, systems)
    }

    companion object {
        fun create(
            tagId: ByteArray,
            scannedAt: Instant,
            idm: FeliCaIdm,
            pmm: FeliCaPmm,
            systems: List<FelicaSystem>
        ): RawFelicaCard {
            return RawFelicaCard(tagId, scannedAt, idm, pmm, systems)
        }
    }
}
