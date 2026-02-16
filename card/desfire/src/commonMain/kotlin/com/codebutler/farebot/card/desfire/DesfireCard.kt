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
import com.codebutler.farebot.base.ui.uiTree
import com.codebutler.farebot.card.Card
import com.codebutler.farebot.card.CardType
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class DesfireCard(
    @Contextual override val tagId: ByteArray,
    override val scannedAt: Instant,
    val applications: List<DesfireApplication>,
    val manufacturingData: DesfireManufacturingData,
    val appListLocked: Boolean = false,
) : Card() {
    override val cardType: CardType = CardType.MifareDesfire

    fun getApplication(appId: Int): DesfireApplication? = applications.firstOrNull { it.id == appId }

    override suspend fun getAdvancedUi(): FareBotUiTree = uiTree {
        item {
            title = "Applications"
            for (app in applications) {
                item {
                    title = "Application: 0x${app.id.toString(16)}"
                    item {
                        title = "Files"
                        for (file in app.files) {
                            item {
                                title = "File: 0x${file.id.toString(16)}"
                                val fileSettings = file.fileSettings
                                if (fileSettings != null) {
                                    item {
                                        title = "Settings"
                                        item { title = "Type"; value = fileSettings.fileTypeName }
                                        if (fileSettings is StandardDesfireFileSettings) {
                                            item { title = "Size"; value = fileSettings.fileSize }
                                        } else if (fileSettings is RecordDesfireFileSettings) {
                                            item { title = "Cur Records"; value = fileSettings.curRecords }
                                            item { title = "Max Records"; value = fileSettings.maxRecords }
                                            item { title = "Record Size"; value = fileSettings.recordSize }
                                        } else if (fileSettings is ValueDesfireFileSettings) {
                                            item { title = "Range"; value = "${fileSettings.lowerLimit} - ${fileSettings.upperLimit}" }
                                            item {
                                                title = "Limited Credit"
                                                value = "${fileSettings.limitedCreditValue} (${if (fileSettings.limitedCreditEnabled) "enabled" else "disabled"})"
                                            }
                                        }
                                    }
                                }
                                if (file is StandardDesfireFile) {
                                    item { title = "Data"; value = file.data }
                                } else if (file is RecordDesfireFile) {
                                    item {
                                        title = "Records"
                                        val records = file.records
                                        for (i in records.indices) {
                                            val record = records[i]
                                            item { title = "Record $i"; value = record.data }
                                        }
                                    }
                                } else if (file is ValueDesfireFile) {
                                    item { title = "Value"; value = file.value }
                                } else if (file is InvalidDesfireFile) {
                                    item { title = "Error"; value = file.errorMessage }
                                } else if (file is UnauthorizedDesfireFile) {
                                    item { title = "Error"; value = file.errorMessage }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            title = "Manufacturing Data"
            item {
                title = "Hardware Information"
                item { title = "Vendor ID"; value = manufacturingData.hwVendorID }
                item { title = "Type"; value = manufacturingData.hwType }
                item { title = "Subtype"; value = manufacturingData.hwSubType }
                item { title = "Major Version"; value = manufacturingData.hwMajorVersion }
                item { title = "Minor Version"; value = manufacturingData.hwMinorVersion }
                item { title = "Storage Size"; value = manufacturingData.hwStorageSize }
                item { title = "Protocol"; value = manufacturingData.hwProtocol }
            }
            item {
                title = "Software Information"
                item { title = "Vendor ID"; value = manufacturingData.swVendorID }
                item { title = "Type"; value = manufacturingData.swType }
                item { title = "Subtype"; value = manufacturingData.swSubType }
                item { title = "Major Version"; value = manufacturingData.swMajorVersion }
                item { title = "Minor Version"; value = manufacturingData.swMinorVersion }
                item { title = "Storage Size"; value = manufacturingData.swStorageSize }
                item { title = "Protocol"; value = manufacturingData.swProtocol }
            }
            item {
                title = "General Information"
                item { title = "Serial Number"; value = manufacturingData.uidHex }
                item { title = "Batch Number"; value = manufacturingData.batchNoHex }
                item { title = "Week of Production"; value = manufacturingData.weekProd.toString(16) }
                item { title = "Year of Production"; value = manufacturingData.yearProd.toString(16) }
            }
        }
    }

    companion object {
        fun create(
            tagId: ByteArray,
            scannedAt: Instant,
            applications: List<DesfireApplication>,
            manufacturingData: DesfireManufacturingData,
            appListLocked: Boolean = false,
        ): DesfireCard = DesfireCard(tagId, scannedAt, applications, manufacturingData, appListLocked)
    }
}
