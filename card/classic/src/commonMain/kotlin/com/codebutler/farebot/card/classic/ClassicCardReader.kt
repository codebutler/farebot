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
    fun readCard(
        tagId: ByteArray,
        tech: ClassicTechnology,
        cardKeys: ClassicCardKeys?,
    ): RawClassicCard {
        val sectors = ArrayList<RawClassicSector>()

        for (sectorIndex in 0 until tech.sectorCount) {
            try {
                var authSuccess = false
                var successfulKeyA: ByteArray? = null
                var successfulKeyB: ByteArray? = null

                // Try the default keys first
                if (!authSuccess && sectorIndex == 0) {
                    authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, PREAMBLE_KEY)
                    if (authSuccess) {
                        successfulKeyA = PREAMBLE_KEY
                    }
                }

                if (!authSuccess) {
                    authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, ClassicTechnology.KEY_DEFAULT)
                    if (authSuccess) {
                        successfulKeyA = ClassicTechnology.KEY_DEFAULT
                    }
                }

                if (cardKeys != null) {
                    // Try with a 1:1 sector mapping on our key list first
                    if (!authSuccess) {
                        val sectorKey: ClassicSectorKey? = cardKeys.keyForSector(sectorIndex)
                        if (sectorKey != null) {
                            authSuccess = tech.authenticateSectorWithKeyA(sectorIndex, sectorKey.keyA)
                            if (authSuccess) {
                                successfulKeyA = sectorKey.keyA
                            } else {
                                authSuccess = tech.authenticateSectorWithKeyB(sectorIndex, sectorKey.keyB)
                                if (authSuccess) {
                                    successfulKeyB = sectorKey.keyB
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
                                successfulKeyA = keys[keyIndex].keyA
                            } else {
                                authSuccess =
                                    tech.authenticateSectorWithKeyB(
                                        sectorIndex,
                                        keys[keyIndex].keyB,
                                    )
                                if (authSuccess) {
                                    successfulKeyB = keys[keyIndex].keyB
                                }
                            }

                            if (authSuccess) {
                                // Jump out if we have the key
                                break
                            }
                        }
                    }
                }

                if (authSuccess) {
                    val blocks = ArrayList<RawClassicBlock>()
                    // FIXME: First read trailer block to get type of other blocks.
                    val firstBlockIndex = tech.sectorToBlock(sectorIndex)
                    for (blockIndex in 0 until tech.getBlockCountInSector(sectorIndex)) {
                        val data = tech.readBlock(firstBlockIndex + blockIndex)
                        blocks.add(RawClassicBlock.create(blockIndex, data))
                    }
                    sectors.add(
                        RawClassicSector.createData(
                            sectorIndex,
                            blocks,
                            successfulKeyA,
                            successfulKeyB,
                        ),
                    )
                } else {
                    sectors.add(RawClassicSector.createUnauthorized(sectorIndex))
                }
            } catch (ex: Exception) {
                sectors.add(RawClassicSector.createInvalid(sectorIndex, ex.message ?: "Unknown error"))
            }
        }

        return RawClassicCard.create(tagId, Clock.System.now(), sectors)
    }
}
