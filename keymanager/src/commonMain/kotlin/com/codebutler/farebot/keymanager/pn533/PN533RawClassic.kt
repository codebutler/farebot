/*
 * PN533RawClassic.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * Raw MIFARE Classic communication via PN533 InCommunicateThru,
 * bypassing the chip's built-in Crypto1 handling to expose raw
 * authentication nonces needed for key recovery attacks.
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

package com.codebutler.farebot.keymanager.pn533

import com.codebutler.farebot.card.nfc.pn533.PN533
import com.codebutler.farebot.card.nfc.pn533.PN533Exception
import com.codebutler.farebot.keymanager.crypto1.Crypto1Auth
import com.codebutler.farebot.keymanager.crypto1.Crypto1State
import kotlinx.coroutines.delay

/**
 * Raw MIFARE Classic interface using PN533 InCommunicateThru.
 *
 * Bypasses the PN533's built-in Crypto1 handling by directly controlling
 * the CIU (Contactless Interface Unit) registers for CRC generation,
 * parity, and crypto state. This allows software-side Crypto1 operations,
 * which is required for key recovery (exposing raw nonces).
 *
 * Reference:
 * - NXP PN533 User Manual (CIU register map)
 * - ISO 14443-3A (CRC-A, MIFARE Classic auth protocol)
 * - mfoc/mfcuk (nested attack implementation)
 *
 * @param pn533 PN533 controller instance
 * @param uid 4-byte card UID (used in Crypto1 cipher initialization)
 */
class PN533RawClassic(
    private val pn533: PN533,
    private val uid: ByteArray,
) {
    /**
     * Disable CRC generation/checking in the CIU.
     *
     * Clears bit 7 of both TxMode and RxMode registers so the PN533
     * does not append/verify CRC bytes. Required for raw Crypto1
     * communication where CRC is computed in software.
     */
    suspend fun disableCrc() {
        pn533.writeRegister(REG_CIU_TX_MODE, 0x00)
        pn533.writeRegister(REG_CIU_RX_MODE, 0x00)
    }

    /**
     * Enable CRC generation/checking in the CIU.
     *
     * Sets bit 7 of both TxMode and RxMode registers for normal
     * CRC-appended communication.
     */
    suspend fun enableCrc() {
        pn533.writeRegister(REG_CIU_TX_MODE, 0x80)
        pn533.writeRegister(REG_CIU_RX_MODE, 0x80)
    }

    /**
     * Disable parity generation/checking in the CIU.
     *
     * Sets bit 4 of ManualRCV register. Required for raw Crypto1
     * communication where parity is handled in software.
     */
    suspend fun disableParity() {
        pn533.writeRegister(REG_CIU_MANUAL_RCV, 0x10)
    }

    /**
     * Enable parity generation/checking in the CIU.
     *
     * Clears bit 4 of ManualRCV register for normal parity handling.
     */
    suspend fun enableParity() {
        pn533.writeRegister(REG_CIU_MANUAL_RCV, 0x00)
    }

    /**
     * Clear the Crypto1 active flag in the CIU.
     *
     * Clears bit 3 of Status2 register, telling the PN533 that
     * no hardware Crypto1 session is active.
     */
    suspend fun clearCrypto1() {
        pn533.writeRegister(REG_CIU_STATUS2, 0x00)
    }

    /**
     * Restore normal CIU operating mode.
     *
     * Re-enables CRC, parity, and clears any Crypto1 state.
     * Call this after raw communication is complete.
     */
    suspend fun restoreNormalMode() {
        enableCrc()
        enableParity()
        clearCrypto1()
    }

    /**
     * Reset the card by cycling the RF field and re-selecting.
     *
     * After an incomplete MIFARE Classic authentication (e.g., requestAuth()
     * collects the nonce but doesn't complete the handshake), the card enters
     * HALT state and won't respond to subsequent commands. Cycling the RF field
     * resets the card, and InListPassiveTarget re-selects it.
     *
     * @return true if the card was successfully re-selected
     */
    suspend fun reselectCard(): Boolean {
        restoreNormalMode()
        return try {
            pn533.rfFieldOff()
            delay(RF_RESET_DELAY_MS)
            pn533.rfFieldOn()
            delay(RF_RESET_DELAY_MS)
            pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_106_ISO14443A) != null
        } catch (_: PN533Exception) {
            false
        }
    }

    /**
     * Send a raw AUTH command and receive the card nonce.
     *
     * Prepares the CIU for raw communication (disable CRC, parity,
     * clear crypto1), then sends the AUTH command via InCommunicateThru.
     * The card responds with a 4-byte plaintext nonce (nT).
     *
     * @param keyType 0x60 for Key A, 0x61 for Key B
     * @param blockIndex Block number to authenticate against
     * @return 4-byte card nonce as UInt (big-endian), or null on failure
     */
    suspend fun requestAuth(
        keyType: Byte,
        blockIndex: Int,
    ): UInt? {
        disableCrc()
        enableParity() // Plaintext auth requires standard ISO 14443-3A parity
        clearCrypto1()

        val cmd = buildAuthCommand(keyType, blockIndex)
        val response =
            try {
                pn533.inCommunicateThru(cmd)
            } catch (_: PN533Exception) {
                return null
            }

        if (response.size < 4) return null
        return parseNonce(response)
    }

    /**
     * Perform a full software Crypto1 authentication.
     *
     * Executes the complete three-pass mutual authentication handshake:
     * 1. Send AUTH command, receive card nonce nT
     * 2. Initialize cipher with key, UID, and nT
     * 3. Compute and send encrypted {nR}{aR}
     * 4. Receive and verify encrypted {aT}
     *
     * @param keyType 0x60 for Key A, 0x61 for Key B
     * @param blockIndex Block number to authenticate against
     * @param key 48-bit MIFARE key (6 bytes packed into a Long)
     * @return Cipher state on success (ready for encrypted communication), null on failure
     */
    suspend fun authenticate(
        keyType: Byte,
        blockIndex: Int,
        key: Long,
    ): Crypto1State? {
        // Step 1: Request auth and get card nonce (plaintext, parity enabled)
        val nT = requestAuth(keyType, blockIndex) ?: return null

        // Step 2: Initialize cipher with key, UID XOR nT
        val uidInt = bytesToUInt(uid)
        val state = Crypto1Auth.initCipher(key, uidInt, nT)

        // Step 3: Compute reader response {nR}{aR}
        // Use a fixed reader nonce (in real attacks this could be random)
        val nR = 0x01020304u
        val (nREnc, aREnc) = Crypto1Auth.computeReaderResponse(state, nR, nT)

        // Step 4: Send encrypted {nR}{aR} — disable parity (encrypted parity handled in software)
        disableParity()
        val readerMsg = uintToBytes(nREnc) + uintToBytes(aREnc)
        val cardResponse =
            try {
                pn533.inCommunicateThru(readerMsg)
            } catch (_: PN533Exception) {
                return null
            }

        // Step 5: Verify card's response {aT}
        if (cardResponse.size < 4) return null
        val aTEnc = bytesToUInt(cardResponse)
        if (!Crypto1Auth.verifyCardResponse(state, aTEnc, nT)) {
            return null
        }

        return state
    }

    /**
     * Perform a nested authentication within an existing encrypted session.
     *
     * Sends an AUTH command encrypted with the current Crypto1 state.
     * The card responds with an encrypted nonce. The encrypted nonce
     * is returned raw (not decrypted) for use in key recovery attacks.
     *
     * @param keyType 0x60 for Key A, 0x61 for Key B
     * @param blockIndex Block number to authenticate against
     * @param currentState Current Crypto1 cipher state from a previous authentication
     * @return Encrypted 4-byte card nonce as UInt (big-endian), or null on failure
     */
    suspend fun nestedAuth(
        keyType: Byte,
        blockIndex: Int,
        currentState: Crypto1State,
    ): UInt? {
        // Build plaintext AUTH command (with CRC)
        val plainCmd = buildAuthCommand(keyType, blockIndex)

        // Encrypt the command with the current cipher state
        val encCmd = Crypto1Auth.encryptBytes(currentState, plainCmd)

        // Send encrypted AUTH command
        val response =
            try {
                pn533.inCommunicateThru(encCmd)
            } catch (_: PN533Exception) {
                return null
            }

        if (response.size < 4) return null

        // Return the encrypted nonce (raw, for key recovery)
        return bytesToUInt(response)
    }

    /**
     * Read a block using software Crypto1 encryption.
     *
     * Encrypts a READ command with the current cipher state, sends it,
     * and decrypts the 16-byte response.
     *
     * @param blockIndex Block number to read
     * @param state Current Crypto1 cipher state (from a successful authentication)
     * @return Decrypted 16-byte block data, or null on failure
     */
    suspend fun readBlockEncrypted(
        blockIndex: Int,
        state: Crypto1State,
    ): ByteArray? {
        // Build plaintext READ command (with CRC)
        val plainCmd = buildReadCommand(blockIndex)

        // Encrypt the command
        val encCmd = Crypto1Auth.encryptBytes(state, plainCmd)

        // Send via InCommunicateThru
        val response =
            try {
                pn533.inCommunicateThru(encCmd)
            } catch (_: PN533Exception) {
                return null
            }

        // Response should be 16 bytes data + 2 bytes CRC = 18 bytes
        if (response.size < 16) return null

        // Decrypt the response
        val decrypted = Crypto1Auth.decryptBytes(state, response)

        // Return the 16-byte data (strip CRC if present)
        return decrypted.copyOfRange(0, 16)
    }

    companion object {
        /** CIU TxMode register — Bit 7 = TX CRC enable */
        const val REG_CIU_TX_MODE = 0x6302

        /** CIU RxMode register — Bit 7 = RX CRC enable */
        const val REG_CIU_RX_MODE = 0x6303

        /** CIU ManualRCV register — Bit 4 = parity disable */
        const val REG_CIU_MANUAL_RCV = 0x630D

        /** CIU Status2 register — Bit 3 = Crypto1 active */
        const val REG_CIU_STATUS2 = 0x6338

        /** Delay in ms for RF field cycling during card reset */
        private const val RF_RESET_DELAY_MS = 50L

        /**
         * Build a MIFARE Classic AUTH command with CRC.
         *
         * Format: [keyType, blockIndex, CRC_L, CRC_H]
         *
         * @param keyType 0x60 for Key A, 0x61 for Key B
         * @param blockIndex Block number to authenticate against
         * @return 4-byte command with ISO 14443-3A CRC appended
         */
        fun buildAuthCommand(
            keyType: Byte,
            blockIndex: Int,
        ): ByteArray {
            val data = byteArrayOf(keyType, blockIndex.toByte())
            val crc = Crypto1Auth.crcA(data)
            return data + crc
        }

        /**
         * Build a MIFARE Classic READ command with CRC.
         *
         * Format: [0x30, blockIndex, CRC_L, CRC_H]
         *
         * @param blockIndex Block number to read
         * @return 4-byte command with ISO 14443-3A CRC appended
         */
        fun buildReadCommand(blockIndex: Int): ByteArray {
            val data = byteArrayOf(0x30, blockIndex.toByte())
            val crc = Crypto1Auth.crcA(data)
            return data + crc
        }

        /**
         * Parse a 4-byte response into a card nonce (big-endian).
         *
         * @param response At least 4 bytes from the card
         * @return UInt nonce value (big-endian interpretation)
         */
        fun parseNonce(response: ByteArray): UInt = bytesToUInt(response)

        /**
         * Convert 4 bytes (big-endian) to a UInt.
         *
         * @param bytes At least 4 bytes, big-endian (MSB first)
         * @return UInt value
         */
        fun bytesToUInt(bytes: ByteArray): UInt =
            ((bytes[0].toInt() and 0xFF).toUInt() shl 24) or
                ((bytes[1].toInt() and 0xFF).toUInt() shl 16) or
                ((bytes[2].toInt() and 0xFF).toUInt() shl 8) or
                (bytes[3].toInt() and 0xFF).toUInt()

        /**
         * Convert a UInt to 4 bytes (big-endian).
         *
         * @param value UInt value to convert
         * @return 4-byte array, big-endian (MSB first)
         */
        fun uintToBytes(value: UInt): ByteArray =
            byteArrayOf(
                ((value shr 24) and 0xFFu).toByte(),
                ((value shr 16) and 0xFFu).toByte(),
                ((value shr 8) and 0xFFu).toByte(),
                (value and 0xFFu).toByte(),
            )
    }
}
