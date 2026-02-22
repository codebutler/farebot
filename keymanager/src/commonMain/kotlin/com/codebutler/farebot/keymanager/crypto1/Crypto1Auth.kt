/*
 * Crypto1Auth.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * MIFARE Classic authentication protocol helpers using the Crypto1 cipher.
 *
 * Implements the three-pass mutual authentication handshake:
 *   1. Reader sends AUTH command, card responds with nonce nT
 *   2. Reader sends encrypted {nR}{aR} where aR = suc^64(nT)
 *   3. Card responds with encrypted aT where aT = suc^96(nT)
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

package com.codebutler.farebot.keymanager.crypto1

/**
 * MIFARE Classic authentication protocol operations.
 *
 * Provides functions for the three-pass mutual authentication handshake,
 * data encryption/decryption, and ISO 14443-3A CRC computation.
 */
object Crypto1Auth {
    /**
     * Initialize cipher for an authentication session.
     *
     * Loads the 48-bit key into the LFSR, then feeds uid XOR nT
     * through the cipher to establish the initial authenticated state.
     *
     * @param key 48-bit MIFARE key (6 bytes packed into a Long)
     * @param uid Card UID (4 bytes)
     * @param nT Card nonce (tag nonce)
     * @return Initialized cipher state ready for authentication
     */
    fun initCipher(
        key: Long,
        uid: UInt,
        nT: UInt,
    ): Crypto1State {
        val state = Crypto1State()
        state.loadKey(key)
        state.lfsrWord(uid xor nT, false)
        return state
    }

    /**
     * Compute the encrypted reader response {nR}{aR}.
     *
     * The reader challenge nR is encrypted with the keystream.
     * The reader answer aR = suc^64(nT) is also encrypted with the keystream.
     *
     * @param state Initialized cipher state (from [initCipher])
     * @param nR Reader nonce (random challenge from the reader)
     * @param nT Card nonce (tag nonce, received from card)
     * @return Pair of (encrypted nR, encrypted aR)
     */
    fun computeReaderResponse(
        state: Crypto1State,
        nR: UInt,
        nT: UInt,
    ): Pair<UInt, UInt> {
        val aR = Crypto1.prngSuccessor(nT, 64u)
        val nREnc = nR xor state.lfsrWord(nR, false)
        val aREnc = aR xor state.lfsrWord(0u, false)
        return Pair(nREnc, aREnc)
    }

    /**
     * Verify the card's encrypted response.
     *
     * The card should respond with encrypted aT where aT = suc^96(nT).
     * This function decrypts the card's response and compares it to the expected value.
     *
     * @param state Cipher state (after [computeReaderResponse])
     * @param aTEnc Encrypted card answer received from the card
     * @param nT Card nonce (tag nonce)
     * @return true if the card's response is valid
     */
    fun verifyCardResponse(
        state: Crypto1State,
        aTEnc: UInt,
        nT: UInt,
    ): Boolean {
        val expectedAT = Crypto1.prngSuccessor(nT, 96u)
        val aT = aTEnc xor state.lfsrWord(0u, false)
        return aT == expectedAT
    }

    /**
     * Encrypt data using the cipher state.
     *
     * Each byte of the input is XORed with a keystream byte produced by the cipher.
     *
     * @param state Cipher state (mutated by this operation)
     * @param data Plaintext data to encrypt
     * @return Encrypted data
     */
    fun encryptBytes(
        state: Crypto1State,
        data: ByteArray,
    ): ByteArray =
        ByteArray(data.size) { i ->
            (data[i].toInt() xor state.lfsrByte(0, false)).toByte()
        }

    /**
     * Decrypt data using the cipher state.
     *
     * Symmetric with [encryptBytes] since XOR is its own inverse.
     *
     * @param state Cipher state (mutated by this operation)
     * @param data Encrypted data to decrypt
     * @return Decrypted data
     */
    fun decryptBytes(
        state: Crypto1State,
        data: ByteArray,
    ): ByteArray =
        ByteArray(data.size) { i ->
            (data[i].toInt() xor state.lfsrByte(0, false)).toByte()
        }

    /**
     * Compute ISO 14443-3A CRC (CRC-A).
     *
     * Polynomial: x^16 + x^12 + x^5 + 1
     * Initial value: 0x6363
     *
     * @param data Input data bytes
     * @return 2-byte CRC in little-endian order (LSB first)
     */
    fun crcA(data: ByteArray): ByteArray {
        var crc = 0x6363
        for (byte in data) {
            var b = (byte.toInt() and 0xFF) xor (crc and 0xFF)
            b = (b xor ((b shl 4) and 0xFF)) and 0xFF
            crc = (crc shr 8) xor (b shl 8) xor (b shl 3) xor (b shr 4)
            crc = crc and 0xFFFF
        }
        return byteArrayOf(
            (crc and 0xFF).toByte(),
            ((crc shr 8) and 0xFF).toByte(),
        )
    }
}
