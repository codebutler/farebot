/*
 * ManlyFastFerryPreambleRecord.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.manly_fast_ferry.record

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.transit.manly_fast_ferry.ManlyFastFerryTransitFactory

/**
 * Represents a "preamble" type record.
 */
class ManlyFastFerryPreambleRecord private constructor() : ManlyFastFerryRecord() {

    /**
     * Returns the card serial number. Returns null on old cards.
     */
    var cardSerial: String? = null
        private set

    companion object {
        private val OLD_CARD_ID = byteArrayOf(0x00, 0x00, 0x00)

        fun recordFromBytes(input: ByteArray): ManlyFastFerryPreambleRecord {
            val record = ManlyFastFerryPreambleRecord()
            // Check that the record is valid for a preamble
            if (!input.copyOfRange(0, ManlyFastFerryTransitFactory.SIGNATURE.size)
                    .contentEquals(ManlyFastFerryTransitFactory.SIGNATURE)
            ) {
                throw IllegalArgumentException("Preamble signature does not match")
            }
            // This is not set on 2012-era cards
            record.cardSerial = if (input.copyOfRange(10, 13).contentEquals(OLD_CARD_ID)) {
                null
            } else {
                ByteUtils.getHexString(input.copyOfRange(10, 14))
            }
            return record
        }
    }
}
