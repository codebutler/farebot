/*
 * NextfareRecordTest.kt
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.transit.nextfare

import com.codebutler.farebot.transit.nextfare.record.NextfareBalanceRecord
import com.codebutler.farebot.transit.nextfare.record.NextfareConfigRecord
import com.codebutler.farebot.transit.nextfare.record.NextfareTransactionRecord
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests relating to Cubic Nextfare reader.
 *
 * Ported from Metrodroid's NextfareTest.kt (record-level tests).
 */
@OptIn(ExperimentalStdlibApi::class)
class NextfareRecordTest {

    @Test
    fun testExpiryDate() {
        val r20250602 = "01030000c2320000010200000000bf0c".hexToByteArray()
        val r20240925 = "0103000039310000010200000000924a".hexToByteArray()
        val r20180815 = "010300010f25000004000000fb75c2f7".hexToByteArray()

        val r1 = NextfareConfigRecord.recordFromBytes(r20250602, TimeZone.UTC)!!
        assertEquals(
            LocalDateTime(2025, 6, 2, 0, 0, 0).toInstant(TimeZone.UTC),
            r1.expiry
        )

        val r2 = NextfareConfigRecord.recordFromBytes(r20240925, TimeZone.UTC)!!
        assertEquals(
            LocalDateTime(2024, 9, 25, 0, 0, 0).toInstant(TimeZone.UTC),
            r2.expiry
        )

        val r3 = NextfareConfigRecord.recordFromBytes(r20180815, TimeZone.UTC)!!
        assertEquals(
            LocalDateTime(2018, 8, 15, 0, 0, 0).toInstant(TimeZone.UTC),
            r3.expiry
        )
    }

    @Test
    fun testTransactionRecord() {
        val rnull = "01000000000000000000000000007f28".hexToByteArray()

        val r = NextfareTransactionRecord.recordFromBytes(rnull, TimeZone.UTC)
        assertNull(r)
    }

    @Test
    fun testBalanceRecord() {
        // This tests the offset negative flag in seq_go.
        // NOTE: These records are synthetic and incomplete, but representative for the tests.
        // Checksums are wrong.

        // SEQ: $12.34, sequence 0x12
        val r1 = NextfareBalanceRecord.recordFromBytes(
            "0128d20400000000000000000012ffff".hexToByteArray()
        )!!
        assertEquals(0x12, r1.version)
        assertEquals(1234, r1.balance)

        // SEQ: -$10.00, sequence 0x23
        val r2 = NextfareBalanceRecord.recordFromBytes(
            "01a8e80300000000000000000023ffff".hexToByteArray()
        )!!
        assertEquals(0x23, r2.version)
        assertEquals(-1000, r2.balance)

        // SEQ: -$10.00, sequence 0x34
        val r3 = NextfareBalanceRecord.recordFromBytes(
            "01a0e80300000000000000000034ffff".hexToByteArray()
        )!!
        assertEquals(0x34, r3.version)
        assertEquals(-1000, r3.balance)
    }
}
