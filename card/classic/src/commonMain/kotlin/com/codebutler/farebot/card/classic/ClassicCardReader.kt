/*
 * ClassicCardReader.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic

import co.touchlab.kermit.Logger
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.CardLostException
import com.codebutler.farebot.card.classic.key.ClassicCardKeys
import com.codebutler.farebot.card.classic.key.ClassicSectorKey
import com.codebutler.farebot.card.classic.raw.RawClassicBlock
import com.codebutler.farebot.card.classic.raw.RawClassicCard
import com.codebutler.farebot.card.classic.raw.RawClassicSector
import com.codebutler.farebot.card.nfc.ClassicTechnology
import com.codebutler.farebot.card.nfc.pn533.PN533ClassicTechnology
import com.codebutler.farebot.card.nfc.pn533.PN533TransportException
import com.codebutler.farebot.shared.nfc.RecoveryPhase
import com.codebutler.farebot.shared.nfc.RecoveryProgressInfo
import kotlin.time.Clock

private val log = Logger.withTag("ClassicCardReader")

object ClassicCardReader {
    private val PREAMBLE_KEY =
        byteArrayOf(
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
        )

    @Throws(Exception::class)
    suspend fun readCard(
        tagId: ByteArray,
        tech: ClassicTechnology,
        cardKeys: ClassicCardKeys?,
        globalKeys: List<ByteArray>? = null,
        keyRecovery: ClassicKeyRecovery? = null,
        onProgress: (suspend (current: Int, total: Int) -> Unit)? = null,
        onPartialCard: (suspend (RawClassicCard) -> Unit)? = null,
        onRecoveryProgress: ((RecoveryProgressInfo) -> Unit)? = null,
    ): RawClassicCard {
        val sectors = ArrayList<RawClassicSector>()
        val sectorCount = tech.sectorCount
        val recoveredKeys = mutableMapOf<Int, Pair<ByteArray, Boolean>>()

        for (sectorIndex in 0 until sectorCount) {
            try {
                onProgress?.invoke(sectorIndex, sectorCount)
                var authSuccess = false
                var successfulKey: ByteArray? = null
                var isKeyA = true

                // Try saved sector-specific keys first (fastest path for known cards)
                if (cardKeys != null && !authSuccess) {
                    val sectorKey: ClassicSectorKey? = cardKeys.keyForSector(sectorIndex)
                    if (sectorKey != null) {
                        if (sectorKey.hasKeyA) {
                            log.d {
                                "Sector $sectorIndex: trying saved keyA=${sectorKey.keyA.hex()}"
                            }
                            authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, sectorKey.keyA)
                            if (authSuccess) {
                                successfulKey = sectorKey.keyA
                                isKeyA = true
                            }
                        }
                        if (!authSuccess && sectorKey.hasKeyB) {
                            log.d {
                                "Sector $sectorIndex: trying saved keyB=${sectorKey.keyB.hex()}"
                            }
                            authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, sectorKey.keyB)
                            if (authSuccess) {
                                successfulKey = sectorKey.keyB
                                isKeyA = false
                            }
                        }
                    }
                }

                // Try well-known default keys
                if (!authSuccess) {
                    log.d {
                        "Sector $sectorIndex: trying default keyA=${ClassicTechnology.KEY_DEFAULT.hex()}"
                    }
                    authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, ClassicTechnology.KEY_DEFAULT)
                    if (authSuccess) {
                        successfulKey = ClassicTechnology.KEY_DEFAULT
                        isKeyA = true
                    }
                }

                if (!authSuccess) {
                    log.d { "Sector $sectorIndex: trying preamble keyA=${PREAMBLE_KEY.hex()}" }
                    authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, PREAMBLE_KEY)
                    if (authSuccess) {
                        successfulKey = PREAMBLE_KEY
                        isKeyA = true
                    }
                }

                if (cardKeys != null) {
                    if (!authSuccess) {
                        // Be a little more forgiving on the key list.  Lets try all the keys!
                        //
                        // This takes longer, of course, but means that users aren't scratching
                        // their heads when we don't get the right key straight away.
                        //
                        // Deduplicate keys to avoid retrying the same key (e.g., many sectors
                        // sharing the default key). Each failed auth is expensive (~200ms).
                        val triedKeys = mutableSetOf(ClassicTechnology.KEY_DEFAULT.toList())
                        if (sectorIndex == 0) triedKeys.add(PREAMBLE_KEY.toList())
                        cardKeys.keyForSector(sectorIndex)?.let {
                            triedKeys.add(it.keyA.toList())
                            triedKeys.add(it.keyB.toList())
                        }

                        val keys: List<ClassicSectorKey> = cardKeys.keys

                        log.d {
                            "Sector $sectorIndex: brute-forcing ${keys.size} saved keys (${triedKeys.size} already tried)"
                        }
                        for (keyIndex in keys.indices) {
                            if (keyIndex == sectorIndex) continue
                            val sectorKey = keys[keyIndex]

                            if (sectorKey.hasKeyA && triedKeys.add(sectorKey.keyA.toList())) {
                                log.d {
                                    "Sector $sectorIndex: brute keyA[$keyIndex]=${sectorKey.keyA.hex()}"
                                }
                                authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, sectorKey.keyA)
                                if (authSuccess) {
                                    successfulKey = sectorKey.keyA
                                    isKeyA = true
                                    break
                                }
                            }

                            if (sectorKey.hasKeyB && triedKeys.add(sectorKey.keyB.toList())) {
                                log.d {
                                    "Sector $sectorIndex: brute keyB[$keyIndex]=${sectorKey.keyB.hex()}"
                                }
                                authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, sectorKey.keyB)
                                if (authSuccess) {
                                    successfulKey = sectorKey.keyB
                                    isKeyA = false
                                    break
                                }
                            }
                        }
                        log.d {
                            "Sector $sectorIndex: brute-force done, tried ${triedKeys.size} unique keys"
                        }
                    }
                }

                // Try global dictionary keys
                if (!authSuccess && !globalKeys.isNullOrEmpty()) {
                    log.d { "Sector $sectorIndex: trying ${globalKeys.size} global keys" }
                    for ((i, globalKey) in globalKeys.withIndex()) {
                        log.d { "Sector $sectorIndex: global[$i] keyA=${globalKey.hex()}" }
                        authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, globalKey)
                        if (authSuccess) {
                            successfulKey = globalKey
                            isKeyA = true
                            break
                        }
                        log.d { "Sector $sectorIndex: global[$i] keyB=${globalKey.hex()}" }
                        authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, globalKey)
                        if (authSuccess) {
                            successfulKey = globalKey
                            isKeyA = false
                            break
                        }
                    }
                }

                // Try previously recovered keys (avoids expensive hardnested if sectors share keys)
                if (!authSuccess && recoveredKeys.isNotEmpty()) {
                    log.d { "Sector $sectorIndex: trying ${recoveredKeys.size} recovered keys" }
                    for ((fromSector, keyInfo) in recoveredKeys) {
                        val (keyBytes, _) = keyInfo
                        authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, keyBytes)
                        if (authSuccess) {
                            successfulKey = keyBytes
                            isKeyA = true
                            log.d {
                                "Sector $sectorIndex: recovered key from sector $fromSector works as keyA!"
                            }
                            break
                        }
                        authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, keyBytes)
                        if (authSuccess) {
                            successfulKey = keyBytes
                            isKeyA = false
                            log.d {
                                "Sector $sectorIndex: recovered key from sector $fromSector works as keyB!"
                            }
                            break
                        }
                    }
                }

                // Try key recovery via pluggable implementation (PN533 only)
                if (!authSuccess &&
                    keyRecovery != null &&
                    tech is PN533ClassicTechnology &&
                    recoveredKeys.isNotEmpty()
                ) {
                    onProgress?.invoke(sectorIndex, sectorCount)
                    val recoveryEngineName = keyRecovery.engineName
                    val recoveryOnProgress: ((String) -> Unit)? =
                        if (onRecoveryProgress != null) {
                            { msg ->
                                val phase = parseRecoveryPhase(msg)
                                onRecoveryProgress.invoke(
                                    RecoveryProgressInfo(
                                        phase = phase,
                                        currentSector = sectorIndex,
                                        totalSectors = sectorCount,
                                        engineName = recoveryEngineName,
                                        recoveredKeys = recoveredKeys.size,
                                        progressDetail = msg,
                                    ),
                                )
                            }
                        } else {
                            null
                        }
                    val recovered = keyRecovery.attemptRecovery(tech, sectorIndex, recoveredKeys, recoveryOnProgress)
                    if (recovered != null) {
                        val (keyBytes, recoveredIsKeyA) = recovered
                        authSuccess =
                            if (recoveredIsKeyA) {
                                tech.authenticateSectorWithKeyA(sectorIndex, keyBytes)
                            } else {
                                tech.authenticateSectorWithKeyB(sectorIndex, keyBytes)
                            }
                        if (authSuccess) {
                            successfulKey = keyBytes
                            isKeyA = recoveredIsKeyA
                            log.i { "Sector $sectorIndex: key recovered!" }
                            onRecoveryProgress?.invoke(
                                RecoveryProgressInfo(
                                    phase = RecoveryPhase.KeyFound,
                                    currentSector = sectorIndex,
                                    totalSectors = sectorCount,
                                    engineName = recoveryEngineName,
                                    recoveredKeys = recoveredKeys.size + 1,
                                    progressDetail = "Key recovered for sector $sectorIndex",
                                ),
                            )
                        }
                    }
                }

                if (authSuccess && successfulKey != null) {
                    log.d {
                        "Sector $sectorIndex: AUTH OK key${if (isKeyA) "A" else "B"}=${successfulKey.hex()}"
                    }
                    recoveredKeys[sectorIndex] = Pair(successfulKey, isKeyA)

                    val blocks = ArrayList<RawClassicBlock>()
                    // FIXME: First read trailer block to get type of other blocks.
                    val firstBlockIndex = tech.sectorToBlock(sectorIndex)
                    for (blockIndex in 0 until tech.getBlockCountInSector(sectorIndex)) {
                        var data = tech.readBlock(firstBlockIndex + blockIndex)

                        // Sometimes the result is just a single byte 0x04
                        // Reauthenticate and retry if that happens (up to 3 times)
                        repeat(3) {
                            if (data.size == 1) {
                                if (isKeyA) {
                                    tech.authenticateSectorWithKeyA(sectorIndex, successfulKey)
                                } else {
                                    tech.authenticateSectorWithKeyB(sectorIndex, successfulKey)
                                }
                                data = tech.readBlock(firstBlockIndex + blockIndex)
                            }
                        }

                        blocks.add(RawClassicBlock.create(blockIndex, data))
                    }
                    sectors.add(
                        RawClassicSector.createData(
                            sectorIndex,
                            blocks,
                            keyA = if (isKeyA) successfulKey else null,
                            keyB = if (!isKeyA) successfulKey else null,
                        ),
                    )

                    // TODO: Metrodroid enhancement - retry with alternate key if blocks are unauthorized
                    // After reading, if blocks are unauthorized, retry authentication with Key B (if we used A)
                    // or Key A (if we used B) and re-read the sector. Requires tracking unauthorized blocks
                    // in RawClassicBlock (see Metrodroid ClassicReader.kt lines 118-139).
                } else {
                    log.d { "Sector $sectorIndex: UNAUTHORIZED (no key found)" }
                    sectors.add(RawClassicSector.createUnauthorized(sectorIndex))
                }
            } catch (ex: PN533TransportException) {
                throw ex
            } catch (ex: CardLostException) {
                // Card was lost during reading - return immediately with partial data
                sectors.add(RawClassicSector.createInvalid(sectorIndex, ex.message ?: "Card lost"))
                return RawClassicCard.create(tagId, Clock.System.now(), sectors, isPartialRead = true)
            } catch (ex: Exception) {
                sectors.add(RawClassicSector.createInvalid(sectorIndex, ex.message ?: "Unknown error"))
            }

            onPartialCard?.invoke(
                RawClassicCard.create(tagId, Clock.System.now(), ArrayList(sectors), isPartialRead = true),
            )
        }

        return RawClassicCard.create(tagId, Clock.System.now(), sectors)
    }

    /**
     * Parse a recovery progress message string into a [RecoveryPhase].
     */
    private fun parseRecoveryPhase(message: String): RecoveryPhase {
        val msg = message.trimStart()
        return when {
            msg.startsWith("Loading bitflip") ||
                msg.startsWith("Parity self-test") ||
                msg.startsWith("Running brute force benchmark") -> RecoveryPhase.Initializing
            msg.startsWith("Phase 1") ||
                msg.contains("unique nonces") ||
                msg.contains("first bytes") ||
                msg.contains("nonces]") ||
                msg.startsWith("Acquisition complete") -> RecoveryPhase.CollectingNonces
            msg.startsWith("Nonce processing") ||
                msg.startsWith("Loading saved nonces") ||
                msg.startsWith("Sum(a0)") ||
                msg.startsWith("Best first byte") ||
                msg.startsWith("Ignoring Sum") -> RecoveryPhase.ProcessingNonces
            msg.contains("brute force") ||
                msg.contains("Tuple") ||
                msg.contains("candidate") ||
                msg.contains("Verifying") ||
                msg.contains("guess:") ||
                msg.contains("pairs") ||
                msg.contains("GPU brute force") ||
                msg.contains("M/s") -> RecoveryPhase.BruteForcing
            msg.startsWith("Key recovered") -> RecoveryPhase.KeyFound
            msg.contains("failed") ||
                msg.contains("FATAL") ||
                msg.contains("Failed") -> RecoveryPhase.Failed
            else -> RecoveryPhase.Initializing
        }
    }
}
