/*
 * RawCEPASCard.kt
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

package com.codebutler.farebot.card.cepas.raw

import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.cepas.CEPASCard
import com.codebutler.farebot.card.cepas.CEPASHistory
import com.codebutler.farebot.card.cepas.CEPASPurse

@Serializable
data class RawCEPASCard(
    @Contextual private val tagId: ByteArray,
    private val scannedAt: Instant,
    val purses: List<RawCEPASPurse>,
    val histories: List<RawCEPASHistory>
) : RawCard<CEPASCard> {

    override fun cardType(): CardType = CardType.CEPAS

    override fun tagId(): ByteArray = tagId

    override fun scannedAt(): Instant = scannedAt

    override fun isUnauthorized(): Boolean = false

    override fun parse(): CEPASCard {
        val parsedPurses: List<CEPASPurse> = purses.map { it.parse() }
        val parsedHistories: List<CEPASHistory> = histories.map { it.parse() }
        return CEPASCard.create(tagId(), scannedAt(), parsedPurses, parsedHistories)
    }

    companion object {
        fun create(
            tagId: ByteArray,
            scannedAt: Instant,
            purses: List<RawCEPASPurse>,
            histories: List<RawCEPASHistory>
        ): RawCEPASCard {
            return RawCEPASCard(tagId, scannedAt, purses, histories)
        }
    }
}
