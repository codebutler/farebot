/*
 * PCSCCardInfo.kt
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

import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.CardType

/**
 * Parses PC/SC ATR (Answer-To-Reset) bytes per PC/SC Part 3 Supplement
 * to determine the NFC card type.
 *
 * The ATR historical bytes contain a Standard byte (SS) and optional
 * Card Name (NN NN) that identify the contactless card technology.
 *
 * Reference: PC/SC Part 3 Supplemental Document
 */
data class PCSCCardInfo(
    val cardType: CardType,
    val classicSectorCount: Int = 0,
    val ultralightType: Int = UltralightTechnology.TYPE_ULTRALIGHT,
) {
    companion object {
        // PC/SC Standard bytes (SS) for NFC card types
        private const val SS_MIFARE_CLASSIC_1K: Byte = 0x01
        private const val SS_MIFARE_CLASSIC_4K: Byte = 0x02
        private const val SS_MIFARE_ULTRALIGHT: Byte = 0x03
        private const val SS_MIFARE_MINI: Byte = 0x26
        private const val SS_FELICA_212K: Byte = 0x11.toByte()
        private const val SS_FELICA_424K: Byte = 0x12.toByte()
        private const val SS_JCOP_DESFIRE: Byte = 0x04 // DESFire via JCOP emulation

        // ISO 15693 / NFC-V card types
        private const val SS_ICODE_SLI: Byte = 0x07
        private const val SS_TAG_IT_HFI: Byte = 0x0C

        // Card Name bytes (NN NN) from ATR for finer identification
        private const val NN_DESFIRE_EV1: Short = 0x0306
        private const val NN_DESFIRE_EV2: Short = 0x0308
        private const val NN_DESFIRE_EV3: Short = 0x030A
        private const val NN_ULTRALIGHT_C: Short = 0x003A
        private const val NN_NTAG_213: Short = 0x0044
        private const val NN_NTAG_215: Short = 0x0044
        private const val NN_NTAG_216: Short = 0x0044

        fun fromATR(atr: ByteArray): PCSCCardInfo {
            // ATR format: 3B 8x 80 01 ... {historical bytes}
            // Historical bytes contain the contactless card info
            // For PC/SC readers: look for the RID + standard byte pattern
            val historicalBytes = extractHistoricalBytes(atr)
            if (historicalBytes == null || historicalBytes.size < 2) {
                return PCSCCardInfo(CardType.MifareDesfire) // default fallback
            }

            // Look for PC/SC RID: A0 00 00 03 06 followed by SS and NN bytes
            val ridIndex = findPCSCRid(historicalBytes)
            if (ridIndex >= 0 && ridIndex + 6 < historicalBytes.size) {
                val ss = historicalBytes[ridIndex + 5]
                val nn =
                    if (ridIndex + 7 < historicalBytes.size) {
                        (
                            (historicalBytes[ridIndex + 6].toInt() and 0xFF) shl 8 or
                                (historicalBytes[ridIndex + 7].toInt() and 0xFF)
                        ).toShort()
                    } else {
                        0.toShort()
                    }
                return fromStandardByte(ss, nn)
            }

            // Some readers don't include the RID, just have a simple ATR
            // Try matching common patterns
            return inferFromATR(atr)
        }

        private fun fromStandardByte(
            ss: Byte,
            nn: Short,
        ): PCSCCardInfo =
            when (ss) {
                SS_MIFARE_CLASSIC_1K -> {
                    PCSCCardInfo(
                        cardType = CardType.MifareClassic,
                        classicSectorCount = 16,
                    )
                }

                SS_MIFARE_CLASSIC_4K -> {
                    PCSCCardInfo(
                        cardType = CardType.MifareClassic,
                        classicSectorCount = 40,
                    )
                }

                SS_MIFARE_MINI -> {
                    PCSCCardInfo(
                        cardType = CardType.MifareClassic,
                        classicSectorCount = 5,
                    )
                }

                SS_MIFARE_ULTRALIGHT -> {
                    when (nn) {
                        NN_ULTRALIGHT_C -> {
                            PCSCCardInfo(
                                cardType = CardType.MifareUltralight,
                                ultralightType = UltralightTechnology.TYPE_ULTRALIGHT_C,
                            )
                        }

                        else -> {
                            PCSCCardInfo(
                                cardType = CardType.MifareUltralight,
                                ultralightType = UltralightTechnology.TYPE_ULTRALIGHT,
                            )
                        }
                    }
                }

                SS_JCOP_DESFIRE -> {
                    PCSCCardInfo(CardType.MifareDesfire)
                }

                SS_FELICA_212K, SS_FELICA_424K -> {
                    PCSCCardInfo(CardType.FeliCa)
                }

                SS_ICODE_SLI, SS_TAG_IT_HFI -> {
                    PCSCCardInfo(CardType.Vicinity)
                }

                else -> {
                    PCSCCardInfo(CardType.MifareDesfire)
                } // default: try DESFire/ISO7816
            }

        private fun extractHistoricalBytes(atr: ByteArray): ByteArray? {
            if (atr.size < 2) return null
            // T0 byte: lower nibble = number of historical bytes
            val t0 = atr[1].toInt() and 0xFF
            val numHistorical = t0 and 0x0F
            // TD1, TD2 etc. may be present depending on upper nibble of T0
            var offset = 2
            // Count interface bytes (TA1, TB1, TC1, TD1, etc.)
            var td = t0
            while (td and 0x80 != 0) {
                // TA present?
                if (td and 0x10 != 0) offset++
                // TB present?
                if (td and 0x20 != 0) offset++
                // TC present?
                if (td and 0x40 != 0) offset++
                // TD present?
                if (td and 0x80 != 0) {
                    if (offset >= atr.size) return null
                    td = atr[offset].toInt() and 0xFF
                    offset++
                }
            }
            if (offset + numHistorical > atr.size) return null
            return atr.copyOfRange(offset, offset + numHistorical)
        }

        private fun findPCSCRid(bytes: ByteArray): Int {
            // PC/SC RID: A0 00 00 03 06
            val rid =
                byteArrayOf(
                    0xA0.toByte(),
                    0x00,
                    0x00,
                    0x03,
                    0x06,
                )
            outer@ for (i in 0..bytes.size - rid.size) {
                for (j in rid.indices) {
                    if (bytes[i + j] != rid[j]) continue@outer
                }
                return i
            }
            return -1
        }

        private fun inferFromATR(atr: ByteArray): PCSCCardInfo {
            // ACR122U and similar readers report specific ATR patterns
            // DESFire commonly: 3B 81 80 01 80 80 (or similar)
            // MIFARE Classic: 3B 8F 80 01 80 4F 0C A0 00 00 03 06 03 00 01 00 00 00 00 6A
            // Try to detect based on known ATR patterns
            val hex = atr.hex()
            return when {
                hex.contains("0001") -> PCSCCardInfo(CardType.MifareClassic, classicSectorCount = 16)
                hex.contains("0002") -> PCSCCardInfo(CardType.MifareClassic, classicSectorCount = 40)
                hex.contains("0003") -> PCSCCardInfo(CardType.MifareUltralight)
                hex.contains("F004") || hex.contains("0306") -> PCSCCardInfo(CardType.MifareDesfire)
                else -> PCSCCardInfo(CardType.MifareDesfire) // default: try ISO7816/DESFire
            }
        }
    }
}
