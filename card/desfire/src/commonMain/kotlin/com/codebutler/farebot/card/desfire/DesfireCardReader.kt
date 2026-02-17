/*
 * DesfireCardReader.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
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

package com.codebutler.farebot.card.desfire

import com.codebutler.farebot.card.desfire.raw.RawDesfireApplication
import com.codebutler.farebot.card.desfire.raw.RawDesfireCard
import com.codebutler.farebot.card.desfire.raw.RawDesfireFile
import com.codebutler.farebot.card.desfire.raw.RawDesfireFileSettings
import com.codebutler.farebot.card.nfc.CardTransceiver
import kotlin.time.Clock

object DesfireCardReader {
    @Throws(Exception::class)
    suspend fun readCard(
        tagId: ByteArray,
        tech: CardTransceiver,
    ): RawDesfireCard {
        val desfireProtocol = DesfireProtocol(tech)

        val manufData = desfireProtocol.getManufacturingData()

        // Try to read app list, fall back to empty list if locked
        var appListLocked: Boolean
        val appIds: IntArray =
            try {
                val ids = desfireProtocol.getAppList()
                appListLocked = false
                ids
            } catch (e: UnauthorizedException) {
                // TODO: When DesfireCardTransitFactory infrastructure is added, probe hiddenAppIds
                // from all registered factories instead of using empty array
                appListLocked = true
                intArrayOf()
            }

        val apps = readApplications(desfireProtocol, appIds)
        return RawDesfireCard.create(tagId, Clock.System.now(), apps, manufData, appListLocked)
    }

    @Throws(Exception::class)
    private suspend fun readApplications(
        desfireProtocol: DesfireProtocol,
        appIds: IntArray,
    ): List<RawDesfireApplication> {
        val apps = ArrayList<RawDesfireApplication>()

        // TODO: When DesfireCardTransitFactory infrastructure is added:
        // - Find matching factory via earlyCheck(appIds)
        // - Create unlocker via factory.createUnlocker(appId, manufData)
        // - Use unlocker.getOrder() to reorder file IDs
        // - Call unlocker.unlock() before reading each file

        for (appId in appIds) {
            try {
                desfireProtocol.selectApp(appId)
            } catch (e: NotFoundException) {
                continue
            }

            val authLog = mutableListOf<DesfireAuthLog>()
            val files = readFiles(desfireProtocol, authLog)
            apps.add(RawDesfireApplication.create(appId, files.first, authLog, files.second))
        }
        return apps
    }

    @Throws(Exception::class)
    private suspend fun readFiles(
        desfireProtocol: DesfireProtocol,
        authLog: MutableList<DesfireAuthLog>,
    ): Pair<List<RawDesfireFile>, Boolean> {
        val files = ArrayList<RawDesfireFile>()

        // Try to read file list, fall back to scanning 0-31 if locked
        var dirListLocked: Boolean
        val fileIds: IntArray =
            try {
                val ids = desfireProtocol.getFileList()
                dirListLocked = false
                ids
            } catch (e: UnauthorizedException) {
                // Directory list is locked, scan all possible file IDs (0-31)
                dirListLocked = true
                IntArray(0x20) { it }
            }

        for (fileId in fileIds) {
            try {
                val settings = desfireProtocol.getFileSettings(fileId)
                files.add(readFile(desfireProtocol, fileId, settings))
            } catch (e: NotFoundException) {
                // File doesn't exist, skip it
                continue
            } catch (e: UnauthorizedException) {
                // File settings are locked, try reading without settings
                files.add(tryReadFileWithoutSettings(desfireProtocol, fileId))
            }
        }

        return Pair(files, dirListLocked)
    }

    @Throws(Exception::class)
    private suspend fun readFile(
        desfireProtocol: DesfireProtocol,
        fileId: Int,
        fileSettings: RawDesfireFileSettings,
    ): RawDesfireFile =
        try {
            val fileData = readFileData(desfireProtocol, fileId, fileSettings)
            RawDesfireFile.create(fileId, fileSettings, fileData)
        } catch (ex: UnauthorizedException) {
            RawDesfireFile.createUnauthorized(fileId, fileSettings, ex.message)
        } catch (ex: Exception) {
            RawDesfireFile.createInvalid(fileId, fileSettings, ex.toString())
        }

    @Throws(Exception::class)
    private suspend fun tryReadFileWithoutSettings(
        desfireProtocol: DesfireProtocol,
        fileId: Int,
    ): RawDesfireFile {
        // Try each read command and see which one works
        var lastException: Exception? = null

        // Try standard data file read
        try {
            val data = desfireProtocol.readFile(fileId)
            return RawDesfireFile.create(fileId, null, data)
        } catch (e: Exception) {
            lastException = e
        }

        // Try value file read
        try {
            val data = desfireProtocol.getValue(fileId)
            return RawDesfireFile.create(fileId, null, data)
        } catch (e: Exception) {
            lastException = e
        }

        // Try record file read
        try {
            val data = desfireProtocol.readRecord(fileId)
            return RawDesfireFile.create(fileId, null, data)
        } catch (e: Exception) {
            lastException = e
        }

        // All commands failed
        return if (lastException is UnauthorizedException) {
            RawDesfireFile.createUnauthorized(fileId, null, lastException.message)
        } else {
            RawDesfireFile.createInvalid(fileId, null, lastException.toString())
        }
    }

    @Throws(Exception::class)
    private suspend fun readFileData(
        desfireProtocol: DesfireProtocol,
        fileId: Int,
        settings: RawDesfireFileSettings,
    ): ByteArray =
        when (settings.fileType()) {
            DesfireFileSettings.STANDARD_DATA_FILE,
            DesfireFileSettings.BACKUP_DATA_FILE,
            -> desfireProtocol.readFile(fileId)

            DesfireFileSettings.VALUE_FILE -> desfireProtocol.getValue(fileId)

            DesfireFileSettings.CYCLIC_RECORD_FILE,
            DesfireFileSettings.LINEAR_RECORD_FILE,
            -> desfireProtocol.readRecord(fileId)

            else -> throw Exception("Unknown file type")
        }
}
