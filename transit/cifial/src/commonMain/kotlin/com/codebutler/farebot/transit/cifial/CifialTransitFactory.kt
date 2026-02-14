/*
 * CifialTransitFactory.kt
 *
 * Copyright 2019 Google
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.transit.cifial

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import farebot.transit.cifial.generated.resources.Res
import farebot.transit.cifial.generated.resources.cifial_card_name
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

class CifialTransitFactory : TransitFactory<ClassicCard, CifialTransitInfo> {
    override val allCards: List<CardInfo> = emptyList()

    override fun check(card: ClassicCard): Boolean {
        val sector0 = card.getSector(0) as? DataClassicSector ?: return false
        return checkSector0(sector0)
    }

    override fun parseIdentity(card: ClassicCard): TransitIdentity =
        TransitIdentity.create(getStringBlocking(Res.string.cifial_card_name), null)

    override fun parseInfo(card: ClassicCard): CifialTransitInfo {
        val sector0 = card.getSector(0) as DataClassicSector
        val b1 = sector0.getBlock(1).data
        val b2 = sector0.getBlock(2).data
        return CifialTransitInfo(
            mRoomNumber = b1.getHexString(12, 2),
            mCheckIn = parseDateTime(b2, 5),
            mCheckOut = parseDateTime(b2, 10),
        )
    }

    private fun checkSector0(sector0: DataClassicSector): Boolean {
        if (sector0.blocks.size < 3) return false
        val block1 = sector0.getBlock(1).data
        val block2 = sector0.getBlock(2).data
        return block1[0] == 0x47.toByte() &&
            validateDate(block2, 5) &&
            validateDate(block2, 10)
    }

    companion object {
        private fun validateDate(
            b: ByteArray,
            off: Int,
        ): Boolean {
            if (off + 5 > b.size) return false
            val hexStr = b.getHexString(off, 5)
            return hexStr.all { it in '0'..'9' } &&
                b.byteArrayToInt(off, 1) in 0..0x59 &&
                b.byteArrayToInt(off + 1, 1) in 0..0x23 &&
                b.byteArrayToInt(off + 2, 1) in 1..0x31 &&
                b.byteArrayToInt(off + 3, 1) in 1..0x12
        }

        private fun parseDateTime(
            b: ByteArray,
            off: Int,
        ): Instant {
            val min = NumberUtils.convertBCDtoInteger(b[off])
            val hour = NumberUtils.convertBCDtoInteger(b[off + 1])
            val day = NumberUtils.convertBCDtoInteger(b[off + 2])
            val month = NumberUtils.convertBCDtoInteger(b[off + 3])
            val year = 2000 + NumberUtils.convertBCDtoInteger(b[off + 4])
            return LocalDateTime(year, month, day, hour, min)
                .toInstant(TimeZone.UTC)
        }
    }
}
