/*
 * PN533CardInfo.kt
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

package com.codebutler.farebot.card.nfc.pn533

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.nfc.UltralightTechnology

/**
 * Maps PN533 target info (SAK byte for Type A, or FeliCa detection)
 * to FareBot [CardType] with metadata.
 *
 * SAK values follow ISO 14443-3A / NXP AN10833.
 */
data class PN533CardInfo(
    val cardType: CardType,
    val classicSectorCount: Int = 0,
    val ultralightType: Int = UltralightTechnology.TYPE_ULTRALIGHT,
) {
    companion object {
        fun fromTypeA(target: PN533.TargetInfo.TypeA): PN533CardInfo {
            val sak = target.sak.toInt() and 0xFF
            return when {
                // SAK bit 5 set = ISO-DEP capable (DESFire, ISO7816)
                sak and 0x20 != 0 ->
                    PN533CardInfo(CardType.MifareDesfire)
                // MIFARE Classic 1K
                sak == 0x08 ->
                    PN533CardInfo(
                        CardType.MifareClassic,
                        classicSectorCount = 16,
                    )
                // MIFARE Classic 4K
                sak == 0x18 ->
                    PN533CardInfo(
                        CardType.MifareClassic,
                        classicSectorCount = 40,
                    )
                // MIFARE Classic Mini
                sak == 0x09 ->
                    PN533CardInfo(
                        CardType.MifareClassic,
                        classicSectorCount = 5,
                    )
                // MIFARE Ultralight / NTAG
                sak == 0x00 ->
                    PN533CardInfo(CardType.MifareUltralight)
                // Default: try ISO7816/DESFire
                else ->
                    PN533CardInfo(CardType.MifareDesfire)
            }
        }

        fun fromFeliCa(): PN533CardInfo = PN533CardInfo(CardType.FeliCa)
    }
}
