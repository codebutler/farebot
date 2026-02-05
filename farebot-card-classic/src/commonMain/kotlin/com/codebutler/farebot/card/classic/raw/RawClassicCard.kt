/*
 * RawClassicCard.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
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

package com.codebutler.farebot.card.classic.raw

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.classic.ClassicCard
import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class RawClassicCard(
    @Contextual private val tagId: ByteArray,
    private val scannedAt: Instant,
    private val sectors: List<RawClassicSector>
) : RawCard<ClassicCard> {

    override fun cardType(): CardType = CardType.MifareClassic

    override fun tagId(): ByteArray = tagId

    override fun scannedAt(): Instant = scannedAt

    override fun isUnauthorized(): Boolean {
        for (sector in sectors) {
            if (sector.type != RawClassicSector.TYPE_UNAUTHORIZED) {
                return false
            }
        }
        return true
    }

    override fun parse(): ClassicCard {
        val parsedSectors = sectors.map { it.parse() }
        return ClassicCard.create(tagId, scannedAt, parsedSectors)
    }

    fun sectors(): List<RawClassicSector> = sectors

    companion object {
        fun create(tagId: ByteArray, scannedAt: Instant, sectors: List<RawClassicSector>): RawClassicCard =
            RawClassicCard(tagId, scannedAt, sectors)
    }
}
