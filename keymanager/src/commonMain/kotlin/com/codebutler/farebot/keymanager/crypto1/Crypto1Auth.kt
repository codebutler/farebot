/*
 * Crypto1Auth.kt
 *
 * Based on crapto1 by blaez and Proxmark3's authentication implementation.
 * https://github.com/RfidResearchGroup/proxmark3
 *
 * MIFARE Classic authentication protocol helpers using the Crypto1 cipher.
 * Ported to Kotlin Multiplatform for FareBot.
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
     * Encrypt data bytes with parity for MIFARE Classic communication.
     *
     * For each byte, produces 8 keystream bits (via lfsrByte) for the data,
     * then reads one more keystream bit (filter output, WITHOUT clocking LFSR)
     * to encrypt the parity bit.
     *
     * This matches Proxmark3's mf_crypto1_encryptEx():
     *   data[i] = crypto1_byte(state, input, 0) ^ plaintext[i]
     *   par[i] = filter(state->odd) ^ oddparity8(plaintext[i])
     *
     * @param state Cipher state (mutated by this operation)
     * @param plaintext The plaintext bytes to encrypt
     * @param input Per-byte input fed to the LFSR (same length as plaintext), or null for zeros
     * @return Pair of (encrypted data bytes, encrypted parity bits as IntArray)
     */
    fun encryptBytesWithParity(
        state: Crypto1State,
        plaintext: ByteArray,
        input: ByteArray? = null,
    ): Pair<ByteArray, IntArray> {
        val encrypted = ByteArray(plaintext.size)
        val parity = IntArray(plaintext.size)
        for (i in plaintext.indices) {
            val pt = plaintext[i].toInt() and 0xFF
            val inp = if (input != null) (input[i].toInt() and 0xFF) else 0
            val ks = state.lfsrByte(inp, false)
            encrypted[i] = (pt xor ks).toByte()
            // Parity: read the filter output (next keystream bit preview) WITHOUT clocking
            val ksPar = Crypto1.filter(state.odd)
            parity[i] = oddParity(pt) xor ksPar
        }
        return Pair(encrypted, parity)
    }

    /**
     * Compute ISO 14443-3A odd parity bit for a byte.
     *
     * Returns 1 when the byte has an even number of set bits,
     * 0 when odd — so that total 1-bits (data + parity) is always odd.
     */
    fun oddParity(b: Int): Int {
        var x = b
        x = x xor (x shr 4)
        x = x xor (x shr 2)
        x = x xor (x shr 1)
        return (x and 1) xor 1
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
