/*
 * CardTestHelper.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2024 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.test

import com.codebutler.farebot.card.classic.ClassicBlock
import com.codebutler.farebot.card.classic.ClassicCard
import com.codebutler.farebot.card.classic.ClassicSector
import com.codebutler.farebot.card.classic.DataClassicSector
import com.codebutler.farebot.card.desfire.DesfireApplication
import com.codebutler.farebot.card.desfire.DesfireCard
import com.codebutler.farebot.card.desfire.DesfireFile
import com.codebutler.farebot.card.desfire.DesfireManufacturingData
import com.codebutler.farebot.card.desfire.RecordDesfireFile
import com.codebutler.farebot.card.desfire.RecordDesfireFileSettings
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.card.desfire.StandardDesfireFileSettings
import com.codebutler.farebot.card.felica.FeliCaIdm
import com.codebutler.farebot.card.felica.FeliCaPmm
import com.codebutler.farebot.card.felica.FelicaBlock
import com.codebutler.farebot.card.felica.FelicaCard
import com.codebutler.farebot.card.felica.FelicaService
import com.codebutler.farebot.card.felica.FelicaSystem
import kotlin.time.Instant

object CardTestHelper {
    private val TEST_TIME = Instant.fromEpochSeconds(1609459200) // 2021-01-01T00:00:00Z
    private val TEST_TAG_ID = byteArrayOf(0x01, 0x02, 0x03, 0x04)

    fun createDesfireManufacturingData(): DesfireManufacturingData =
        DesfireManufacturingData(
            hwVendorID = 0x04,
            hwType = 0x01,
            hwSubType = 0x01,
            hwMajorVersion = 0x01,
            hwMinorVersion = 0x00,
            hwStorageSize = 0x18,
            hwProtocol = 0x05,
            swVendorID = 0x04,
            swType = 0x01,
            swSubType = 0x01,
            swMajorVersion = 0x01,
            swMinorVersion = 0x00,
            swStorageSize = 0x18,
            swProtocol = 0x05,
            uid = ByteArray(7),
            batchNo = ByteArray(5),
            weekProd = 0,
            yearProd = 0,
        )

    fun standardFileSettings(fileSize: Int): StandardDesfireFileSettings =
        StandardDesfireFileSettings.create(
            fileType = 0x00,
            commSetting = 0x00,
            accessRights = byteArrayOf(0x00, 0x00),
            fileSize = fileSize,
        )

    fun recordFileSettings(
        recordSize: Int,
        maxRecords: Int,
        curRecords: Int,
    ): RecordDesfireFileSettings =
        RecordDesfireFileSettings.create(
            fileType = 0x04,
            commSetting = 0x00,
            accessRights = byteArrayOf(0x00, 0x00),
            recordSize = recordSize,
            maxRecords = maxRecords,
            curRecords = curRecords,
        )

    fun standardFile(
        fileId: Int,
        data: ByteArray,
    ): StandardDesfireFile = StandardDesfireFile(fileId, standardFileSettings(data.size), data)

    fun recordFile(
        fileId: Int,
        recordSize: Int,
        records: List<ByteArray>,
    ): RecordDesfireFile {
        val fullData = ByteArray(records.size * recordSize)
        records.forEachIndexed { index, record ->
            record.copyInto(fullData, index * recordSize)
        }
        val settings = recordFileSettings(recordSize, records.size, records.size)
        return RecordDesfireFile.create(fileId, settings, fullData)
    }

    fun desfireApp(
        appId: Int,
        files: List<DesfireFile>,
    ): DesfireApplication = DesfireApplication.create(appId, files)

    fun desfireCard(
        applications: List<DesfireApplication>,
        tagId: ByteArray = TEST_TAG_ID,
        scannedAt: Instant = TEST_TIME,
    ): DesfireCard = DesfireCard.create(tagId, scannedAt, applications, createDesfireManufacturingData())

    fun felicaCard(
        systems: List<FelicaSystem>,
        tagId: ByteArray = TEST_TAG_ID,
        scannedAt: Instant = TEST_TIME,
    ): FelicaCard =
        FelicaCard.create(
            tagId,
            scannedAt,
            FeliCaIdm(ByteArray(8)),
            FeliCaPmm(ByteArray(8)),
            systems,
        )

    fun felicaSystem(
        code: Int,
        services: List<FelicaService>,
    ): FelicaSystem = FelicaSystem.create(code, services)

    fun felicaService(
        serviceCode: Int,
        blocks: List<FelicaBlock>,
    ): FelicaService = FelicaService.create(serviceCode, blocks)

    fun felicaBlock(
        address: Int,
        data: ByteArray,
    ): FelicaBlock = FelicaBlock.create(address.toByte(), data)

    // --- Classic Card builders ---

    fun classicBlock(
        type: String,
        index: Int,
        data: ByteArray,
    ): ClassicBlock = ClassicBlock.create(type, index, data)

    fun classicSector(
        index: Int,
        blocks: List<ClassicBlock>,
        keyA: ByteArray? = null,
        keyB: ByteArray? = null,
    ): DataClassicSector = DataClassicSector(index, blocks, keyA, keyB)

    fun classicCard(
        sectors: List<ClassicSector>,
        tagId: ByteArray = TEST_TAG_ID,
        scannedAt: Instant = TEST_TIME,
    ): ClassicCard = ClassicCard.create(tagId, scannedAt, sectors)

    /**
     * Build a standard 16-sector Classic card from raw block data.
     * Each sector has 3 data blocks + 1 trailer block, all 16 bytes each.
     */
    fun classicCardFromSectorData(
        sectorData: Map<Int, List<ByteArray>>,
        tagId: ByteArray = TEST_TAG_ID,
        scannedAt: Instant = TEST_TIME,
        numSectors: Int = 16,
    ): ClassicCard {
        val sectors =
            (0 until numSectors).map { sectorIndex ->
                val blockData = sectorData[sectorIndex]
                if (blockData != null) {
                    val blocks =
                        blockData.mapIndexed { blockIndex, data ->
                            val type =
                                when {
                                    sectorIndex == 0 && blockIndex == 0 -> ClassicBlock.TYPE_MANUFACTURER
                                    blockIndex == blockData.size - 1 -> ClassicBlock.TYPE_TRAILER
                                    else -> ClassicBlock.TYPE_DATA
                                }
                            ClassicBlock.create(type, blockIndex, data)
                        }
                    DataClassicSector(sectorIndex, blocks)
                } else {
                    // Empty sector with zeroed blocks
                    val trailer =
                        ByteArray(16).also {
                            // Standard trailer: keyA(6) + access(4) + keyB(6)
                            for (i in 0..5) it[i] = 0xFF.toByte()
                            for (i in 10..15) it[i] = 0xFF.toByte()
                        }
                    val blocks =
                        (0..3).map { blockIndex ->
                            val type =
                                when {
                                    sectorIndex == 0 && blockIndex == 0 -> ClassicBlock.TYPE_MANUFACTURER
                                    blockIndex == 3 -> ClassicBlock.TYPE_TRAILER
                                    else -> ClassicBlock.TYPE_DATA
                                }
                            ClassicBlock.create(type, blockIndex, if (blockIndex == 3) trailer else ByteArray(16))
                        }
                    DataClassicSector(sectorIndex, blocks)
                }
            }
        return ClassicCard.create(tagId, scannedAt, sectors)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun hexToBytes(hex: String): ByteArray = hex.replace(" ", "").hexToByteArray()
}
