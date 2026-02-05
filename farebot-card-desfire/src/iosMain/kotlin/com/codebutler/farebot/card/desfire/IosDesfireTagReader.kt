/*
 * IosDesfireTagReader.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

import com.codebutler.farebot.card.desfire.raw.RawDesfireApplication
import com.codebutler.farebot.card.desfire.raw.RawDesfireCard
import com.codebutler.farebot.card.desfire.raw.RawDesfireFile
import com.codebutler.farebot.card.desfire.raw.RawDesfireFileSettings
import com.codebutler.farebot.card.nfc.CardTransceiver
import kotlin.time.Clock

/**
 * iOS implementation of the DESFire tag reader.
 *
 * DESFire cards appear as NFCMiFareTag on iOS. The [CardTransceiver] wraps the
 * iOS tag and provides raw APDU transceive. The actual protocol logic is shared
 * via [DesfireProtocol] in commonMain.
 */
class IosDesfireTagReader(
    private val tagId: ByteArray,
    private val transceiver: CardTransceiver,
) {

    fun readTag(): RawDesfireCard {
        transceiver.connect()
        try {
            val protocol = DesfireProtocol(transceiver)
            val apps = readApplications(protocol)
            val manufData = protocol.getManufacturingData()
            return RawDesfireCard.create(tagId, Clock.System.now(), apps, manufData)
        } finally {
            if (transceiver.isConnected) {
                try {
                    transceiver.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun readApplications(protocol: DesfireProtocol): List<RawDesfireApplication> {
        val apps = mutableListOf<RawDesfireApplication>()
        val appList = protocol.getAppList()
        for (appId in appList) {
            protocol.selectApp(appId)
            apps.add(RawDesfireApplication.create(appId, readFiles(protocol)))
        }
        return apps
    }

    private fun readFiles(protocol: DesfireProtocol): List<RawDesfireFile> {
        val files = mutableListOf<RawDesfireFile>()
        for (fileId in protocol.getFileList()) {
            val settings = protocol.getFileSettings(fileId)
            files.add(readFile(protocol, fileId, settings))
        }
        return files
    }

    private fun readFile(
        protocol: DesfireProtocol,
        fileId: Int,
        fileSettings: RawDesfireFileSettings,
    ): RawDesfireFile {
        return try {
            val fileData = readFileData(protocol, fileId, fileSettings)
            RawDesfireFile.create(fileId, fileSettings, fileData)
        } catch (ex: DesfireAccessControlException) {
            RawDesfireFile.createUnauthorized(fileId, fileSettings, ex.message ?: "Access denied")
        } catch (ex: Exception) {
            RawDesfireFile.createInvalid(fileId, fileSettings, ex.toString())
        }
    }

    private fun readFileData(
        protocol: DesfireProtocol,
        fileId: Int,
        settings: RawDesfireFileSettings,
    ): ByteArray {
        return when (settings.fileType()) {
            DesfireFileSettings.STANDARD_DATA_FILE,
            DesfireFileSettings.BACKUP_DATA_FILE -> protocol.readFile(fileId)
            DesfireFileSettings.VALUE_FILE -> protocol.getValue(fileId)
            DesfireFileSettings.CYCLIC_RECORD_FILE,
            DesfireFileSettings.LINEAR_RECORD_FILE -> protocol.readRecord(fileId)
            else -> throw Exception("Unknown file type")
        }
    }
}
