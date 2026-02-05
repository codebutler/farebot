/*
 * DesfireCard.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * Contains improvements ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class DesfireCard(
    @Contextual override val tagId: ByteArray,
    override val scannedAt: Instant,
    val applications: List<DesfireApplication>,
    val manufacturingData: DesfireManufacturingData,
    val appListLocked: Boolean = false
) : Card() {

    override val cardType: CardType = CardType.MifareDesfire

    fun getApplication(appId: Int): DesfireApplication? =
        applications.firstOrNull { it.id == appId }

    override fun getAdvancedUi(stringResource: StringResource): FareBotUiTree {
        val cardUiBuilder = FareBotUiTree.builder(stringResource)
        val appsUiBuilder = cardUiBuilder.item().title("Applications")
        for (app in applications) {
            val appUiBuilder = appsUiBuilder.item()
                .title("Application: 0x${app.id.toString(16)}")
            val filesUiBuilder = appUiBuilder.item().title("Files")
            for (file in app.files) {
                val fileUiBuilder = filesUiBuilder.item()
                    .title("File: 0x${file.id.toString(16)}")
                val fileSettings = file.fileSettings
                val settingsUiBuilder = fileUiBuilder.item().title("Settings")
                settingsUiBuilder.item()
                    .title("Type")
                    .value(fileSettings.fileTypeName)
                if (fileSettings is StandardDesfireFileSettings) {
                    settingsUiBuilder.item()
                        .title("Size")
                        .value(fileSettings.fileSize)
                } else if (fileSettings is RecordDesfireFileSettings) {
                    settingsUiBuilder.item()
                        .title("Cur Records")
                        .value(fileSettings.curRecords)
                    settingsUiBuilder.item()
                        .title("Max Records")
                        .value(fileSettings.maxRecords)
                    settingsUiBuilder.item()
                        .title("Record Size")
                        .value(fileSettings.recordSize)
                } else if (fileSettings is ValueDesfireFileSettings) {
                    settingsUiBuilder.item()
                        .title("Range")
                        .value("${fileSettings.lowerLimit} - ${fileSettings.upperLimit}")
                    settingsUiBuilder.item()
                        .title("Limited Credit")
                        .value("${fileSettings.limitedCreditValue} (${if (fileSettings.limitedCreditEnabled) "enabled" else "disabled"})")
                }
                if (file is StandardDesfireFile) {
                    fileUiBuilder.item()
                        .title("Data")
                        .value(file.data)
                } else if (file is RecordDesfireFile) {
                    val recordsUiBuilder = fileUiBuilder.item()
                        .title("Records")
                    val records = file.records
                    for (i in records.indices) {
                        val record = records[i]
                        recordsUiBuilder.item()
                            .title("Record $i")
                            .value(record.data)
                    }
                } else if (file is ValueDesfireFile) {
                    fileUiBuilder.item()
                        .title("Value")
                        .value(file.value)
                } else if (file is InvalidDesfireFile) {
                    fileUiBuilder.item()
                        .title("Error")
                        .value(file.errorMessage)
                } else if (file is UnauthorizedDesfireFile) {
                    fileUiBuilder.item()
                        .title("Error")
                        .value(file.errorMessage)
                }
            }
        }

        val manufacturingDataUiBuilder = cardUiBuilder.item().title("Manufacturing Data")

        val hwInfoUiBuilder = manufacturingDataUiBuilder.item().title("Hardware Information")
        hwInfoUiBuilder.item().title("Vendor ID").value(manufacturingData.hwVendorID)
        hwInfoUiBuilder.item().title("Type").value(manufacturingData.hwType)
        hwInfoUiBuilder.item().title("Subtype").value(manufacturingData.hwSubType)
        hwInfoUiBuilder.item().title("Major Version").value(manufacturingData.hwMajorVersion)
        hwInfoUiBuilder.item().title("Minor Version").value(manufacturingData.hwMinorVersion)
        hwInfoUiBuilder.item().title("Storage Size").value(manufacturingData.hwStorageSize)
        hwInfoUiBuilder.item().title("Protocol").value(manufacturingData.hwProtocol)

        val swInfoUiBuilder = manufacturingDataUiBuilder.item().title("Software Information")
        swInfoUiBuilder.item().title("Vendor ID").value(manufacturingData.swVendorID)
        swInfoUiBuilder.item().title("Type").value(manufacturingData.swType)
        swInfoUiBuilder.item().title("Subtype").value(manufacturingData.swSubType)
        swInfoUiBuilder.item().title("Major Version").value(manufacturingData.swMajorVersion)
        swInfoUiBuilder.item().title("Minor Version").value(manufacturingData.swMinorVersion)
        swInfoUiBuilder.item().title("Storage Size").value(manufacturingData.swStorageSize)
        swInfoUiBuilder.item().title("Protocol").value(manufacturingData.swProtocol)

        val generalInfoUiBuilder = manufacturingDataUiBuilder.item().title("General Information")
        generalInfoUiBuilder.item().title("Serial Number").value(manufacturingData.uidHex)
        generalInfoUiBuilder.item().title("Batch Number").value(manufacturingData.batchNoHex)
        generalInfoUiBuilder.item().title("Week of Production").value(manufacturingData.weekProd.toString(16))
        generalInfoUiBuilder.item().title("Year of Production").value(manufacturingData.yearProd.toString(16))

        return cardUiBuilder.build()
    }

    companion object {
        fun create(
            tagId: ByteArray,
            scannedAt: Instant,
            applications: List<DesfireApplication>,
            manufacturingData: DesfireManufacturingData,
            appListLocked: Boolean = false
        ): DesfireCard = DesfireCard(tagId, scannedAt, applications, manufacturingData, appListLocked)
    }
}
