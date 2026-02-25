/*
 * PN533RawClassic.kt
 *
 * Based on Proxmark3's mifare_sendcmd_short / mifare_classic_authex.
 * PN533 register manipulation based on NXP PN533 User Manual.
 * https://github.com/RfidResearchGroup/proxmark3
 *
 * Raw MIFARE Classic communication via PN533 InCommunicateThru,
 * bypassing the chip's built-in Crypto1 handling to expose raw
 * authentication nonces needed for key recovery attacks.
 * Ported to Kotlin Multiplatform for FareBot.
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
import com.codebutler.farebot.keymanager.crypto1.Crypto1
import com.codebutler.farebot.keymanager.crypto1.Crypto1Auth
import com.codebutler.farebot.keymanager.crypto1.Crypto1State
import kotlinx.coroutines.delay

/**
 * Result of a nested authentication attempt, containing the encrypted
 * nonce and the associated encrypted parity bits.
 *
 * @param encryptedNonce 4-byte encrypted card nonce as UInt (big-endian)
 * @param encryptedParity 4 encrypted parity bits, one per nonce byte.
 *   Packed as: bit 3 = parity of byte 0, bit 0 = parity of byte 3.
 */
data class NestedAuthResult(
    val encryptedNonce: UInt,
    val encryptedParity: Int,
)

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
     * Re-select the card without cycling the RF field.
     *
     * After an incomplete MIFARE Classic authentication (e.g., requestAuth()
     * collects the nonce but doesn't complete the handshake), the card
     * returns to IDLE state after its Frame Waiting Time expires (~5ms).
     * We wait for that timeout, release the PN533's internal target tracking,
     * then re-select with InListPassiveTarget (which sends REQA).
     *
     * Crucially, this keeps the RF field powered — the card's PRNG continues
     * running from its original seed, which is required for PRNG distance
     * calibration in the nested attack.
     *
     * @return true if the card was successfully re-selected
     */
    suspend fun reselectCard(): Boolean {
        restoreNormalMode()
        // Wait for card's auth timeout (FWT ~5ms) so it returns to IDLE state
        delay(CARD_AUTH_TIMEOUT_MS)
        return try {
            // Release PN533's internal target tracking
            try {
                pn533.inRelease(0)
            } catch (_: PN533Exception) {
                // May fail if no target was listed — that's fine
            }
            // Re-select card (REQA → anti-collision → SELECT)
            pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_106_ISO14443A) != null
        } catch (_: PN533Exception) {
            false
        }
    }

    /**
     * Re-select the card by cycling the RF field.
     *
     * Unlike [reselectCard], this turns the RF field off and on,
     * which resets the card completely (including reseeding its PRNG).
     * Use this when the card is in ACTIVE or HALT state and a soft
     * reselect (REQA-based) won't work.
     *
     * This MUST NOT be used during PRNG calibration as it destroys
     * the PRNG sequence continuity.
     *
     * @return true if the card was successfully re-selected
     */
    suspend fun hardReselectCard(): Boolean {
        restoreNormalMode()
        return try {
            try {
                pn533.inRelease(0)
            } catch (_: PN533Exception) {
            }
            pn533.rfFieldOff()
            delay(RF_CYCLE_DELAY_MS)
            pn533.rfFieldOn()
            delay(RF_CYCLE_DELAY_MS)
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
     * 1. Set up CIU for raw mode (CRC off, parity off, crypto1 off) upfront
     * 2. Send AUTH command with software parity (packed bitstream)
     * 3. Receive card nonce (packed bitstream, unpack)
     * 4. Initialize cipher and encrypt {nR}{aR} with parity
     * 5. Send {nR}{aR} immediately (NO register writes between nonce and response!)
     * 6. Receive and verify encrypted {aT}
     *
     * Critical timing: MIFARE Classic requires the reader to respond within
     * 5ms of the card's nonce. By disabling parity BEFORE the AUTH command
     * (and computing parity in software for all frames), we eliminate USB
     * register-write round-trips between receiving the nonce and sending
     * the reader response.
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
        // Step 1: CRC off, parity ON (CIU standard parity for plaintext AUTH), crypto1 off
        disableCrc()
        enableParity()
        clearCrypto1()

        // Step 2: Send plaintext AUTH command with CIU parity → get nonce
        val authCmd = buildAuthCommand(keyType, blockIndex)
        val nonceResponse =
            try {
                pn533.inCommunicateThru(authCmd)
            } catch (e: PN533Exception) {
                println("[RawAuth] Step 2 FAIL: AUTH command failed: ${e.message}")
                return null
            }
        if (nonceResponse.size < 4) {
            println("[RawAuth] Step 3 FAIL: nonce response too short (${nonceResponse.size} bytes)")
            return null
        }
        // Nonce is 4 bytes with CIU parity stripped (standard mode)
        val nT = bytesToUInt(nonceResponse)
        println("[RawAuth] Step 3 OK: nT=0x${nT.toString(16).padStart(8, '0')}")

        // Step 4: Disable CIU parity for encrypted communication
        // (single register write — card is computing its nonce successor, we have FWT budget)
        disableParity()

        // Step 5: Initialize cipher and encrypt {nR, aR} with parity
        val uidInt = bytesToUInt(uid)
        val state = Crypto1Auth.initCipher(key, uidInt, nT)

        val nR = 0x01020304u
        val aR = Crypto1.prngSuccessor(nT, 64u)
        val nRBytes = uintToBytes(nR)
        val aRBytes = uintToBytes(aR)
        val plaintext = nRBytes + aRBytes
        val input = nRBytes + ByteArray(4) // nR fed back for first 4 bytes, 0 for aR
        val (encData, parityBits) = Crypto1Auth.encryptBytesWithParity(state, plaintext, input)

        // Step 6: Pack and send {nR}{aR} as raw bitstream
        // 8 data bytes × 9 bits = 72 bits = 9 FIFO bytes (byte-aligned, no BitFraming needed)
        val (packed, _) = packWithParity(encData, parityBits)
        println("[RawAuth] Step 6: sending ${packed.size} packed bytes")

        val cardResponse =
            try {
                pn533.inCommunicateThru(packed)
            } catch (e: PN533Exception) {
                println("[RawAuth] Step 6 FAIL: ${e.message}")
                return null
            }

        // Step 7: Unpack card response {aT}
        // Card sends 4 encrypted bytes + 4 parity bits = 36 bits = 5 FIFO bytes
        println("[RawAuth] Step 7: response ${cardResponse.size} bytes")
        val aTEnc: UInt
        if (cardResponse.size >= 5) {
            val (aTBytes, _) = unpackWithParity(cardResponse, 4)
            aTEnc = bytesToUInt(aTBytes)
        } else if (cardResponse.size >= 4) {
            aTEnc = bytesToUInt(cardResponse)
        } else {
            println("[RawAuth] Step 7 FAIL: response too short (${cardResponse.size} bytes)")
            return null
        }

        // Step 8: Verify card response
        if (!Crypto1Auth.verifyCardResponse(state, aTEnc, nT)) {
            println(
                "[RawAuth] Step 8 FAIL: card response verification failed (aTEnc=0x${aTEnc.toString(
                    16,
                ).padStart(8, '0')})",
            )
            return null
        }
        println("[RawAuth] Step 8 OK: auth successful")

        return state
    }

    /**
     * Perform a nested authentication within an existing encrypted session.
     *
     * Sends an AUTH command encrypted with the current Crypto1 state,
     * including proper encrypted parity bits packed into the raw bitstream.
     * The card responds with an encrypted nonce (also with parity).
     * The encrypted nonce and parity bits are returned raw (not decrypted)
     * for key recovery.
     *
     * @param keyType 0x60 for Key A, 0x61 for Key B
     * @param blockIndex Block number to authenticate against
     * @param currentState Current Crypto1 cipher state from a previous authentication
     * @return [NestedAuthResult] with encrypted nonce and parity, or null on failure
     */
    suspend fun nestedAuth(
        keyType: Byte,
        blockIndex: Int,
        currentState: Crypto1State,
    ): NestedAuthResult? {
        // Build plaintext AUTH command (with CRC): [keyType, blockIndex, CRC_L, CRC_H]
        val plainCmd = buildAuthCommand(keyType, blockIndex)

        // Encrypt with parity (LFSR input = 0 for encrypted communication)
        val (encCmd, parityBits) = Crypto1Auth.encryptBytesWithParity(currentState, plainCmd, null)

        // Pack into raw bitstream: 4 bytes × 9 bits = 36 bits = 5 FIFO bytes
        val (packed, txLastBits) = packWithParity(encCmd, parityBits)
        if (txLastBits != 0) {
            pn533.writeRegister(REG_CIU_BIT_FRAMING, txLastBits)
        }

        val response =
            try {
                pn533.inCommunicateThru(packed)
            } catch (_: PN533Exception) {
                if (txLastBits != 0) pn533.writeRegister(REG_CIU_BIT_FRAMING, 0x00)
                return null
            }
        if (txLastBits != 0) {
            pn533.writeRegister(REG_CIU_BIT_FRAMING, 0x00)
        }

        // Card responds with encrypted nonce: 4 bytes + parity = 36 bits = 5 FIFO bytes
        val encNonce: UInt
        val encParity: Int
        if (response.size >= 5) {
            val (nonceBytes, parBits) = unpackWithParity(response, 4)
            encNonce = bytesToUInt(nonceBytes)
            // Pack 4 parity bits: bit 3 = par of byte 0, bit 0 = par of byte 3
            encParity = (parBits[0] shl 3) or (parBits[1] shl 2) or (parBits[2] shl 1) or parBits[3]
        } else if (response.size >= 4) {
            encNonce = bytesToUInt(response)
            encParity = 0 // No parity available
        } else {
            return null
        }

        return NestedAuthResult(encNonce, encParity)
    }

    /**
     * Read a block using software Crypto1 encryption.
     *
     * Encrypts a READ command with parity, packs into raw bitstream,
     * sends it, unpacks the encrypted response, and decrypts.
     *
     * @param blockIndex Block number to read
     * @param state Current Crypto1 cipher state (from a successful authentication)
     * @return Decrypted 16-byte block data, or null on failure
     */
    suspend fun readBlockEncrypted(
        blockIndex: Int,
        state: Crypto1State,
    ): ByteArray? {
        // Build plaintext READ command (with CRC): [0x30, blockIndex, CRC_L, CRC_H]
        val plainCmd = buildReadCommand(blockIndex)

        // Encrypt with parity
        val (encCmd, parityBits) = Crypto1Auth.encryptBytesWithParity(state, plainCmd, null)

        // Pack: 4 bytes × 9 bits = 36 bits = 5 FIFO bytes (txLastBits = 4)
        val (packed, txLastBits) = packWithParity(encCmd, parityBits)
        if (txLastBits != 0) {
            pn533.writeRegister(REG_CIU_BIT_FRAMING, txLastBits)
        }

        val response =
            try {
                pn533.inCommunicateThru(packed)
            } catch (_: PN533Exception) {
                if (txLastBits != 0) pn533.writeRegister(REG_CIU_BIT_FRAMING, 0x00)
                return null
            }
        if (txLastBits != 0) {
            pn533.writeRegister(REG_CIU_BIT_FRAMING, 0x00)
        }

        // Response: 18 bytes (16 data + 2 CRC) with parity = 18 × 9 = 162 bits = 21 FIFO bytes
        // Unpack data bytes from response, then decrypt
        val expectedBytes = 18 // 16 data + 2 CRC
        val expectedFifoBytes = (expectedBytes * 9 + 7) / 8 // 21 bytes
        if (response.size >= expectedFifoBytes) {
            val (encDataBytes, _) = unpackWithParity(response, expectedBytes)
            val decrypted = Crypto1Auth.decryptBytes(state, encDataBytes)
            return decrypted.copyOfRange(0, 16)
        } else if (response.size >= 16) {
            // Fallback: no parity in response, data is still encrypted
            val decrypted = Crypto1Auth.decryptBytes(state, response)
            return decrypted.copyOfRange(0, 16)
        }
        return null
    }

    companion object {
        /** CIU TxMode register — Bit 7 = TX CRC enable */
        const val REG_CIU_TX_MODE = 0x6302

        /** CIU RxMode register — Bit 7 = RX CRC enable */
        const val REG_CIU_RX_MODE = 0x6303

        /** CIU ManualRCV register — Bit 4 = parity disable */
        const val REG_CIU_MANUAL_RCV = 0x630D

        /** CIU BitFraming register — Bits 2:0 = TxLastBits */
        const val REG_CIU_BIT_FRAMING = 0x633D

        /** CIU Status2 register — Bit 3 = Crypto1 active */
        const val REG_CIU_STATUS2 = 0x6338

        /** Wait time in ms for card's auth timeout (FWT) before re-selecting */
        private const val CARD_AUTH_TIMEOUT_MS = 10L

        /** Wait time in ms for RF field off/on cycle during hard reselect */
        private const val RF_CYCLE_DELAY_MS = 25L

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

        /**
         * Pack data bytes and parity bits into a raw bitstream for PN533 FIFO.
         *
         * ISO 14443 transmits LSB first. Each data byte (8 bits, LSB first) is
         * followed by one parity bit, forming a 9-bit frame. With ParityDisable=1,
         * the PN533 CIU sends the FIFO contents as raw bits without inserting
         * parity, so we must interleave data and parity bits ourselves.
         *
         * Matches libnfc's pn53x_wrap_frame() behavior.
         *
         * @param data Encrypted data bytes
         * @param parity Parity bits (one per data byte, 0 or 1)
         * @return Pair of (packed FIFO bytes, TxLastBits for BitFraming register)
         */
        fun packWithParity(
            data: ByteArray,
            parity: IntArray,
        ): Pair<ByteArray, Int> {
            val totalBits = data.size * 9
            val fifoSize = (totalBits + 7) / 8
            val fifo = ByteArray(fifoSize)

            var bitPos = 0
            for (i in data.indices) {
                val byte = data[i].toInt() and 0xFF
                // Emit 8 data bits, LSB first
                for (b in 0 until 8) {
                    if ((byte shr b) and 1 == 1) {
                        fifo[bitPos / 8] = (fifo[bitPos / 8].toInt() or (1 shl (bitPos % 8))).toByte()
                    }
                    bitPos++
                }
                // Emit parity bit
                if (parity[i] == 1) {
                    fifo[bitPos / 8] = (fifo[bitPos / 8].toInt() or (1 shl (bitPos % 8))).toByte()
                }
                bitPos++
            }

            val txLastBits = totalBits % 8
            return Pair(fifo, txLastBits)
        }

        /**
         * Unpack a raw bitstream from PN533 FIFO into data bytes and parity bits.
         *
         * Inverse of [packWithParity]. Each 9-bit frame contains 8 data bits
         * (LSB first) followed by 1 parity bit.
         *
         * Matches libnfc's pn53x_unwrap_frame() behavior.
         *
         * @param fifo Raw FIFO bytes from PN533
         * @param byteCount Number of data bytes to extract
         * @return Pair of (data bytes, parity bits)
         */
        fun unpackWithParity(
            fifo: ByteArray,
            byteCount: Int,
        ): Pair<ByteArray, IntArray> {
            val data = ByteArray(byteCount)
            val parity = IntArray(byteCount)

            var bitPos = 0
            for (i in 0 until byteCount) {
                var byte = 0
                for (b in 0 until 8) {
                    if (bitPos / 8 < fifo.size &&
                        (fifo[bitPos / 8].toInt() shr (bitPos % 8)) and 1 == 1
                    ) {
                        byte = byte or (1 shl b)
                    }
                    bitPos++
                }
                data[i] = byte.toByte()
                // Read parity bit
                if (bitPos / 8 < fifo.size) {
                    parity[i] = (fifo[bitPos / 8].toInt() shr (bitPos % 8)) and 1
                }
                bitPos++
            }

            return Pair(data, parity)
        }
    }
}
