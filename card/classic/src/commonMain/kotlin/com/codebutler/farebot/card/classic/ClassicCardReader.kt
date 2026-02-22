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

import com.codebutler.farebot.card.CardLostException
import com.codebutler.farebot.card.classic.key.ClassicCardKeys
import com.codebutler.farebot.card.classic.key.ClassicSectorKey
import com.codebutler.farebot.card.classic.raw.RawClassicBlock
import com.codebutler.farebot.card.classic.raw.RawClassicCard
import com.codebutler.farebot.card.classic.raw.RawClassicSector
import com.codebutler.farebot.card.nfc.ClassicTechnology
import kotlin.time.Clock

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
    ): RawClassicCard {
        val sectors = ArrayList<RawClassicSector>()

        for (sectorIndex in 0 until tech.sectorCount) {
            try {
                var authSuccess = false
                var successfulKey: ByteArray? = null
                var isKeyA = true

                // Try the default keys first
                if (!authSuccess && sectorIndex == 0) {
                    authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, PREAMBLE_KEY)
                    if (authSuccess) {
                        successfulKey = PREAMBLE_KEY
                        isKeyA = true
                    }
                }

                if (!authSuccess) {
                    authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, ClassicTechnology.KEY_DEFAULT)
                    if (authSuccess) {
                        successfulKey = ClassicTechnology.KEY_DEFAULT
                        isKeyA = true
                    }
                }

                if (cardKeys != null) {
                    // Try with a 1:1 sector mapping on our key list first
                    if (!authSuccess) {
                        val sectorKey: ClassicSectorKey? = cardKeys.keyForSector(sectorIndex)
                        if (sectorKey != null) {
                            authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, sectorKey.keyA)
                            if (authSuccess) {
                                successfulKey = sectorKey.keyA
                                isKeyA = true
                            } else {
                                authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, sectorKey.keyB)
                                if (authSuccess) {
                                    successfulKey = sectorKey.keyB
                                    isKeyA = false
                                }
                            }
                        }
                    }

                    if (!authSuccess) {
                        // Be a little more forgiving on the key list.  Lets try all the keys!
                        //
                        // This takes longer, of course, but means that users aren't scratching
                        // their heads when we don't get the right key straight away.
                        val keys: List<ClassicSectorKey> = cardKeys.keys

                        for (keyIndex in keys.indices) {
                            if (keyIndex == sectorIndex) {
                                // We tried this before
                                continue
                            }

                            authSuccess =
                                tech.authenticateSectorWithKeyA(
                                    sectorIndex,
                                    keys[keyIndex].keyA,
                                )

                            if (authSuccess) {
                                successfulKey = keys[keyIndex].keyA
                                isKeyA = true
                            } else {
                                authSuccess =
                                    tech.authenticateSectorWithKeyB(
                                        sectorIndex,
                                        keys[keyIndex].keyB,
                                    )

                                if (authSuccess) {
                                    successfulKey = keys[keyIndex].keyB
                                    isKeyA = false
                                }
                            }

                            if (authSuccess) {
                                // Jump out if we have the key
                                break
                            }
                        }
                    }
                }

                // Try global dictionary keys
                if (!authSuccess && !globalKeys.isNullOrEmpty()) {
                    for (globalKey in globalKeys) {
                        authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, globalKey)
                        if (authSuccess) {
                            successfulKey = globalKey
                            isKeyA = true
                            break
                        }
                        authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, globalKey)
                        if (authSuccess) {
                            successfulKey = globalKey
                            isKeyA = false
                            break
                        }
                    }
                }

                if (authSuccess && successfulKey != null) {
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
                    sectors.add(RawClassicSector.createData(sectorIndex, blocks))

                    // TODO: Metrodroid enhancement - retry with alternate key if blocks are unauthorized
                    // After reading, if blocks are unauthorized, retry authentication with Key B (if we used A)
                    // or Key A (if we used B) and re-read the sector. Requires tracking unauthorized blocks
                    // in RawClassicBlock (see Metrodroid ClassicReader.kt lines 118-139).
                } else {
                    sectors.add(RawClassicSector.createUnauthorized(sectorIndex))
                }
            } catch (ex: CardLostException) {
                // Card was lost during reading - return immediately with partial data
                sectors.add(RawClassicSector.createInvalid(sectorIndex, ex.message ?: "Card lost"))
                return RawClassicCard.create(tagId, Clock.System.now(), sectors, isPartialRead = true)
            } catch (ex: Exception) {
                sectors.add(RawClassicSector.createInvalid(sectorIndex, ex.message ?: "Unknown error"))
            }
        }

        return RawClassicCard.create(tagId, Clock.System.now(), sectors)
    }
}
