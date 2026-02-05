/*
 * CalypsoConstants.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.en1545

/**
 * SFI (Short File Identifier) constants for Calypso card files.
 * These are the standard file identifiers used by Calypso/EN1545 transit cards.
 */
object CalypsoConstants {
    /** Ticket environment (holder info, card validity, etc.) */
    const val SFI_TICKETING_ENVIRONMENT = 0x07

    /** Transaction log */
    const val SFI_TICKETING_LOG = 0x08

    /** First contracts file */
    const val SFI_TICKETING_CONTRACTS_1 = 0x09

    /** Second contracts file */
    const val SFI_TICKETING_CONTRACTS_2 = 0x06

    /** Counter 1 */
    const val SFI_TICKETING_COUNTERS_1 = 0x19

    /** Counter 2 */
    const val SFI_TICKETING_COUNTERS_2 = 0x1A

    /** Counter 3 */
    const val SFI_TICKETING_COUNTERS_3 = 0x1B

    /** Counter 4 */
    const val SFI_TICKETING_COUNTERS_4 = 0x1C

    /** Contract list */
    const val SFI_TICKETING_CONTRACT_LIST = 0x1E

    /** Shared counter (multiple contracts) */
    const val SFI_TICKETING_COUNTERS_9 = 0x10

    /** Special events log */
    const val SFI_TICKETING_SPECIAL_EVENTS = 0x1D

    private val COUNTER_SFIS = intArrayOf(
        SFI_TICKETING_COUNTERS_1,
        SFI_TICKETING_COUNTERS_2,
        SFI_TICKETING_COUNTERS_3,
        SFI_TICKETING_COUNTERS_4
    )

    fun getCounterSfi(recordNum: Int): Int? {
        if (recordNum < 1 || recordNum > 4) return null
        return COUNTER_SFIS[recordNum - 1]
    }
}
