/*
 * PCSCCardInfoTest.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.nfc

import com.codebutler.farebot.card.CardType
import kotlin.test.Test
import kotlin.test.assertEquals

class PCSCCardInfoTest {
    @Test
    fun testFromATR_vicinityICODESLI() {
        // ATR with PC/SC RID (A0 00 00 03 06) followed by SS=0x07 (ICODE SLI) and NN=0x00 0x01
        val atr =
            byteArrayOf(
                0x3B.toByte(),
                0x8F.toByte(),
                0x80.toByte(),
                0x01,
                0x80.toByte(),
                0x4F,
                0x0C,
                0xA0.toByte(),
                0x00,
                0x00,
                0x03,
                0x06,
                0x07, // SS = ICODE SLI (ISO 15693)
                0x00,
                0x01, // NN = card name
                0x00,
                0x00,
                0x00,
                0x00,
                0x00, // TCK
            )
        val info = PCSCCardInfo.fromATR(atr)
        assertEquals(CardType.Vicinity, info.cardType)
    }

    @Test
    fun testFromATR_vicinityTagIt() {
        // ATR with SS=0x0C (Tag-it HFI)
        val atr =
            byteArrayOf(
                0x3B.toByte(),
                0x8F.toByte(),
                0x80.toByte(),
                0x01,
                0x80.toByte(),
                0x4F,
                0x0C,
                0xA0.toByte(),
                0x00,
                0x00,
                0x03,
                0x06,
                0x0C, // SS = Tag-it HFI Plus (ISO 15693)
                0x00,
                0x01,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
            )
        val info = PCSCCardInfo.fromATR(atr)
        assertEquals(CardType.Vicinity, info.cardType)
    }

    @Test
    fun testFromATR_existingClassic1k() {
        // Verify existing behavior isn't broken: SS=0x01 → MifareClassic
        val atr =
            byteArrayOf(
                0x3B.toByte(),
                0x8F.toByte(),
                0x80.toByte(),
                0x01,
                0x80.toByte(),
                0x4F,
                0x0C,
                0xA0.toByte(),
                0x00,
                0x00,
                0x03,
                0x06,
                0x01, // SS = MIFARE Classic 1K
                0x00,
                0x01,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
            )
        val info = PCSCCardInfo.fromATR(atr)
        assertEquals(CardType.MifareClassic, info.cardType)
    }
}
