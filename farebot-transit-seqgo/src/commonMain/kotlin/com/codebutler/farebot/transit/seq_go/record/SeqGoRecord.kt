/*
 * SeqGoRecord.kt
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

package com.codebutler.farebot.transit.seq_go.record

/**
 * Represents a record on a SEQ Go Card (Translink).
 */
abstract class SeqGoRecord {

    companion object {
        fun recordFromBytes(input: ByteArray): SeqGoRecord? {
            var record: SeqGoRecord? = null
            when (input[0]) {
                0x01.toByte() -> {
                    // Check if the next byte is not null
                    when {
                        input[1] == 0x00.toByte() -> {
                            // Metadata record, which we don't understand yet
                        }
                        input[1] == 0x01.toByte() -> {
                            if (input[13] == 0x00.toByte()) {
                                // Some other metadata type
                                return null
                            }
                            record = SeqGoTopupRecord.recordFromBytes(input)
                        }
                        else -> {
                            record = SeqGoBalanceRecord.recordFromBytes(input)
                        }
                    }
                }

                0x31.toByte() -> {
                    if (input[1] == 0x01.toByte()) {
                        if (input[13] == 0x00.toByte()) {
                            // Some other metadata type
                            return null
                        }
                        record = SeqGoTopupRecord.recordFromBytes(input)
                    } else {
                        record = SeqGoTapRecord.recordFromBytes(input)
                    }
                }

                else -> {
                    // Unknown record type
                }
            }

            return record
        }
    }
}
