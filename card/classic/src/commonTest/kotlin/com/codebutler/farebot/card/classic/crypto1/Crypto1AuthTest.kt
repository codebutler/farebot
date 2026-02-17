/*
 * Crypto1AuthTest.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic.crypto1

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for the MIFARE Classic authentication protocol helpers.
 */
class Crypto1AuthTest {
    // Common test constants
    private val testKey = 0xFFFFFFFFFFFF // Default MIFARE key (all 0xFF bytes)
    private val testKeyA0 = 0xA0A1A2A3A4A5L
    private val testUid = 0xDEADBEEFu
    private val testNT = 0x12345678u
    private val testNR = 0xAABBCCDDu

    @Test
    fun testInitCipher() {
        // Verify initCipher produces a non-zero state for a non-zero key
        val state = Crypto1Auth.initCipher(testKey, testUid, testNT)
        // After loading key and feeding uid^nT, state should be non-trivial
        assertTrue(
            state.odd != 0u || state.even != 0u,
            "Initialized cipher state should be non-zero",
        )

        // Verify determinism: same inputs produce same state
        val state2 = Crypto1Auth.initCipher(testKey, testUid, testNT)
        assertEquals(state.odd, state2.odd, "initCipher should be deterministic (odd)")
        assertEquals(state.even, state2.even, "initCipher should be deterministic (even)")

        // Different keys should produce different states
        val state3 = Crypto1Auth.initCipher(testKeyA0, testUid, testNT)
        assertTrue(
            state.odd != state3.odd || state.even != state3.even,
            "Different keys should produce different states",
        )

        // Different UIDs should produce different states
        val state4 = Crypto1Auth.initCipher(testKey, 0x01020304u, testNT)
        assertTrue(
            state.odd != state4.odd || state.even != state4.even,
            "Different UIDs should produce different states",
        )

        // Different nonces should produce different states
        val state5 = Crypto1Auth.initCipher(testKey, testUid, 0x87654321u)
        assertTrue(
            state.odd != state5.odd || state.even != state5.even,
            "Different nonces should produce different states",
        )
    }

    @Test
    fun testComputeReaderResponse() {
        val state = Crypto1Auth.initCipher(testKey, testUid, testNT)
        val (nREnc, aREnc) = Crypto1Auth.computeReaderResponse(state, testNR, testNT)

        // Encrypted values should differ from plaintext
        assertNotEquals(testNR, nREnc, "Encrypted nR should differ from plaintext nR")

        val aR = Crypto1.prngSuccessor(testNT, 64u)
        assertNotEquals(aR, aREnc, "Encrypted aR should differ from plaintext aR")

        // Verify determinism: same inputs produce same encrypted outputs
        val state2 = Crypto1Auth.initCipher(testKey, testUid, testNT)
        val (nREnc2, aREnc2) = Crypto1Auth.computeReaderResponse(state2, testNR, testNT)
        assertEquals(nREnc, nREnc2, "computeReaderResponse should be deterministic (nR)")
        assertEquals(aREnc, aREnc2, "computeReaderResponse should be deterministic (aR)")
    }

    @Test
    fun testFullAuthRoundtrip() {
        // Simulate a full three-pass mutual authentication between reader and card.
        //
        // Protocol:
        //   1. Card sends nT
        //   2. Reader computes {nR}{aR} where aR = suc^64(nT)
        //   3. Card verifies aR and responds with {aT} where aT = suc^96(nT)
        //   4. Reader verifies aT
        //
        // Both sides initialize with the same key and uid^nT.

        val key = testKeyA0
        val uid = 0x01020304u
        val nT = 0xCAFEBABEu
        val nR = 0xDEAD1234u

        // --- Reader side ---
        val readerState = Crypto1Auth.initCipher(key, uid, nT)
        val (nREnc, aREnc) = Crypto1Auth.computeReaderResponse(readerState, nR, nT)

        // --- Card side ---
        // Card initializes its own cipher the same way
        val cardState = Crypto1Auth.initCipher(key, uid, nT)

        // Card decrypts nR using encrypted mode: this feeds the plaintext nR bits
        // into the LFSR (since isEncrypted=true, feedback = ciphertext XOR keystream = plaintext).
        // This matches the reader side which fed nR via lfsrWord(nR, false).
        val nRDecrypted = nREnc xor cardState.lfsrWord(nREnc, true)

        // Card decrypts aR: both sides feed 0 into the LFSR for the aR portion.
        // The reader used lfsrWord(0, false), so the card must also feed 0
        // and XOR the keystream with the ciphertext externally.
        val expectedAR = Crypto1.prngSuccessor(nT, 64u)
        val aRKeystream = cardState.lfsrWord(0u, false)
        val aRDecrypted = aREnc xor aRKeystream
        assertEquals(expectedAR, aRDecrypted, "Card should decrypt aR to suc^64(nT)")

        // Card computes and encrypts aT = suc^96(nT)
        // Both sides feed 0 for the aT portion as well.
        val aT = Crypto1.prngSuccessor(nT, 96u)
        val aTEnc = aT xor cardState.lfsrWord(0u, false)

        // --- Reader side verifies card response ---
        val verified = Crypto1Auth.verifyCardResponse(readerState, aTEnc, nT)
        assertTrue(verified, "Reader should verify card's response successfully")
    }

    @Test
    fun testVerifyCardResponseRejectsWrongValue() {
        val key = testKey
        val uid = testUid
        val nT = testNT
        val nR = testNR

        val state = Crypto1Auth.initCipher(key, uid, nT)
        Crypto1Auth.computeReaderResponse(state, nR, nT)

        // Send a wrong encrypted aT
        val wrongATEnc = 0xBADF00Du
        val verified = Crypto1Auth.verifyCardResponse(state, wrongATEnc, nT)
        assertFalse(verified, "verifyCardResponse should reject incorrect card response")
    }

    @Test
    fun testCrcA() {
        // ISO 14443-3A CRC test vectors.
        //
        // CRC_A of AUTH command (0x60) for block 0 (0x00):
        // AUTH_READ = 0x60, block = 0x00 -> CRC = [0xF5, 0x7B]
        // This is a well-known test vector from the MIFARE specification.
        val authCmd = byteArrayOf(0x60, 0x00)
        val crc = Crypto1Auth.crcA(authCmd)
        assertEquals(2, crc.size, "CRC-A should be 2 bytes")
        assertContentEquals(byteArrayOf(0xF5.toByte(), 0x7B), crc, "CRC-A of [0x60, 0x00]")

        // CRC of empty data should be initial value split into bytes: [0x63, 0x63]
        val emptyCrc = Crypto1Auth.crcA(byteArrayOf())
        assertContentEquals(
            byteArrayOf(0x63, 0x63),
            emptyCrc,
            "CRC-A of empty data should be [0x63, 0x63]",
        )

        // CRC of a single zero byte
        val zeroCrc = Crypto1Auth.crcA(byteArrayOf(0x00))
        assertEquals(2, zeroCrc.size, "CRC-A should always be 2 bytes")

        // CRC of READ command (0x30) for block 0 (0x00)
        val readCmd = byteArrayOf(0x30, 0x00)
        val readCrc = Crypto1Auth.crcA(readCmd)
        assertContentEquals(byteArrayOf(0x02, 0xA8.toByte()), readCrc, "CRC-A of [0x30, 0x00]")
    }

    @Test
    fun testEncryptDecryptRoundtrip() {
        val key = testKeyA0
        val uid = 0x01020304u
        val nT = 0xABCD1234u

        val plaintext = byteArrayOf(
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10,
        )

        // Encrypt with one cipher state
        val encState = Crypto1Auth.initCipher(key, uid, nT)
        val ciphertext = Crypto1Auth.encryptBytes(encState, plaintext)

        // Ciphertext should differ from plaintext
        assertFalse(
            plaintext.contentEquals(ciphertext),
            "Ciphertext should differ from plaintext",
        )

        // Decrypt with a fresh cipher state (same initialization)
        val decState = Crypto1Auth.initCipher(key, uid, nT)
        val decrypted = Crypto1Auth.decryptBytes(decState, ciphertext)

        assertContentEquals(plaintext, decrypted, "Decrypt(Encrypt(data)) should return original data")
    }

    @Test
    fun testEncryptDecryptEmptyData() {
        val state = Crypto1Auth.initCipher(testKey, testUid, testNT)
        val result = Crypto1Auth.encryptBytes(state, byteArrayOf())
        assertContentEquals(byteArrayOf(), result, "Encrypting empty data should return empty")
    }

    @Test
    fun testEncryptDecryptSingleByte() {
        val key = testKey
        val uid = testUid
        val nT = testNT

        val plaintext = byteArrayOf(0x42)

        val encState = Crypto1Auth.initCipher(key, uid, nT)
        val ciphertext = Crypto1Auth.encryptBytes(encState, plaintext)
        assertEquals(1, ciphertext.size, "Single-byte encrypt should produce one byte")

        val decState = Crypto1Auth.initCipher(key, uid, nT)
        val decrypted = Crypto1Auth.decryptBytes(decState, ciphertext)
        assertContentEquals(plaintext, decrypted, "Single-byte roundtrip should work")
    }

    @Test
    fun testCrcAMultipleBytes() {
        // Additional CRC-A test: WRITE command (0xA0) for block 4 (0x04)
        val writeCmd = byteArrayOf(0xA0.toByte(), 0x04)
        val crc = Crypto1Auth.crcA(writeCmd)
        assertEquals(2, crc.size)
        // Verify the CRC is not the initial value (confirms computation happened)
        assertFalse(
            crc[0] == 0x63.toByte() && crc[1] == 0x63.toByte(),
            "CRC of non-empty data should differ from initial value",
        )
    }
}
