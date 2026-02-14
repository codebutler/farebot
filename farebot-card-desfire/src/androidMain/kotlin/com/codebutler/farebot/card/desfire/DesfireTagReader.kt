/*
 * DesfireTagReader.kt
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

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.codebutler.farebot.card.TagReader
import com.codebutler.farebot.card.desfire.raw.RawDesfireApplication
import com.codebutler.farebot.card.desfire.raw.RawDesfireCard
import com.codebutler.farebot.card.desfire.raw.RawDesfireFile
import com.codebutler.farebot.card.desfire.raw.RawDesfireFileSettings
import com.codebutler.farebot.card.nfc.AndroidCardTransceiver
import com.codebutler.farebot.card.nfc.CardTransceiver
import com.codebutler.farebot.key.CardKeys
import java.io.IOException
import java.util.ArrayList
import kotlin.time.Clock

class DesfireTagReader(
    tagId: ByteArray,
    tag: Tag,
) : TagReader<CardTransceiver, RawDesfireCard, CardKeys>(tagId, tag, null) {
    override fun getTech(tag: Tag): CardTransceiver = AndroidCardTransceiver(IsoDep.get(tag))

    @Throws(Exception::class)
    override fun readTag(
        tagId: ByteArray,
        tag: Tag,
        tech: CardTransceiver,
        cardKeys: CardKeys?,
    ): RawDesfireCard {
        val desfireProtocol = DesfireProtocol(tech)
        val apps = readApplications(desfireProtocol)
        val manufData = desfireProtocol.getManufacturingData()
        return RawDesfireCard.create(tagId, Clock.System.now(), apps, manufData)
    }

    @Throws(Exception::class)
    private fun readApplications(desfireProtocol: DesfireProtocol): List<RawDesfireApplication> {
        val apps = ArrayList<RawDesfireApplication>()
        for (appId in desfireProtocol.getAppList()) {
            desfireProtocol.selectApp(appId)
            apps.add(RawDesfireApplication.create(appId, readFiles(desfireProtocol)))
        }
        return apps
    }

    @Throws(Exception::class)
    private fun readFiles(desfireProtocol: DesfireProtocol): List<RawDesfireFile> {
        val files = ArrayList<RawDesfireFile>()
        for (fileId in desfireProtocol.getFileList()) {
            val settings = desfireProtocol.getFileSettings(fileId)
            files.add(readFile(desfireProtocol, fileId, settings))
        }
        return files
    }

    @Throws(Exception::class)
    private fun readFile(
        desfireProtocol: DesfireProtocol,
        fileId: Int,
        fileSettings: RawDesfireFileSettings,
    ): RawDesfireFile =
        try {
            val fileData = readFileData(desfireProtocol, fileId, fileSettings)
            RawDesfireFile.create(fileId, fileSettings, fileData)
        } catch (ex: DesfireAccessControlException) {
            RawDesfireFile.createUnauthorized(fileId, fileSettings, ex.message ?: "Access denied")
        } catch (ex: IOException) {
            throw ex
        } catch (ex: Exception) {
            RawDesfireFile.createInvalid(fileId, fileSettings, ex.toString())
        }

    @Throws(Exception::class)
    private fun readFileData(
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
