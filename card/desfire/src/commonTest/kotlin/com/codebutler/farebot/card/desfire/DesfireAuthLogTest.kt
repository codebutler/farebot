/*
 * DesfireAuthLogTest.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2026 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.desfire

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DesfireAuthLogTest {
    @Test
    fun testEquality() {
        val log1 =
            DesfireAuthLog(
                keyId = 1,
                challenge = byteArrayOf(0x01, 0x02, 0x03),
                response = byteArrayOf(0x04, 0x05, 0x06),
                confirm = byteArrayOf(0x07, 0x08, 0x09),
            )
        val log2 =
            DesfireAuthLog(
                keyId = 1,
                challenge = byteArrayOf(0x01, 0x02, 0x03),
                response = byteArrayOf(0x04, 0x05, 0x06),
                confirm = byteArrayOf(0x07, 0x08, 0x09),
            )

        assertEquals(log1, log2)
        assertEquals(log1.hashCode(), log2.hashCode())
    }

    @Test
    fun testInequalityDifferentKeyId() {
        val log1 =
            DesfireAuthLog(
                keyId = 1,
                challenge = byteArrayOf(0x01),
                response = byteArrayOf(0x02),
                confirm = byteArrayOf(0x03),
            )
        val log2 =
            DesfireAuthLog(
                keyId = 2,
                challenge = byteArrayOf(0x01),
                response = byteArrayOf(0x02),
                confirm = byteArrayOf(0x03),
            )

        assertNotEquals(log1, log2)
    }

    @Test
    fun testInequalityDifferentChallenge() {
        val log1 =
            DesfireAuthLog(
                keyId = 1,
                challenge = byteArrayOf(0x01),
                response = byteArrayOf(0x02),
                confirm = byteArrayOf(0x03),
            )
        val log2 =
            DesfireAuthLog(
                keyId = 1,
                challenge = byteArrayOf(0xFF.toByte()),
                response = byteArrayOf(0x02),
                confirm = byteArrayOf(0x03),
            )

        assertNotEquals(log1, log2)
    }

    @Test
    fun testInequalityDifferentResponse() {
        val log1 =
            DesfireAuthLog(
                keyId = 1,
                challenge = byteArrayOf(0x01),
                response = byteArrayOf(0x02),
                confirm = byteArrayOf(0x03),
            )
        val log2 =
            DesfireAuthLog(
                keyId = 1,
                challenge = byteArrayOf(0x01),
                response = byteArrayOf(0xFF.toByte()),
                confirm = byteArrayOf(0x03),
            )

        assertNotEquals(log1, log2)
    }

    @Test
    fun testInequalityDifferentConfirm() {
        val log1 =
            DesfireAuthLog(
                keyId = 1,
                challenge = byteArrayOf(0x01),
                response = byteArrayOf(0x02),
                confirm = byteArrayOf(0x03),
            )
        val log2 =
            DesfireAuthLog(
                keyId = 1,
                challenge = byteArrayOf(0x01),
                response = byteArrayOf(0x02),
                confirm = byteArrayOf(0xFF.toByte()),
            )

        assertNotEquals(log1, log2)
    }
}
