/*
 * RawDesfireFileSettings.kt
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

package com.codebutler.farebot.card.desfire.raw

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.card.desfire.DesfireFileSettings
import com.codebutler.farebot.card.desfire.DesfireFileSettings.Companion.BACKUP_DATA_FILE
import com.codebutler.farebot.card.desfire.DesfireFileSettings.Companion.CYCLIC_RECORD_FILE
import com.codebutler.farebot.card.desfire.DesfireFileSettings.Companion.LINEAR_RECORD_FILE
import com.codebutler.farebot.card.desfire.DesfireFileSettings.Companion.STANDARD_DATA_FILE
import com.codebutler.farebot.card.desfire.DesfireFileSettings.Companion.VALUE_FILE
import com.codebutler.farebot.card.desfire.RecordDesfireFileSettings
import com.codebutler.farebot.card.desfire.StandardDesfireFileSettings
import com.codebutler.farebot.card.desfire.ValueDesfireFileSettings

@Serializable
data class RawDesfireFileSettings(
    @Contextual val data: ByteArray
) {
    fun fileType(): Byte = data[0]

    fun parse(): DesfireFileSettings {
        val bytes = data
        var offset = 0

        val fileType = bytes[offset++]
        val commSetting = bytes[offset++]

        val accessRights = ByteArray(2)
        bytes.copyInto(accessRights, 0, offset, offset + accessRights.size)
        offset += accessRights.size

        return when (fileType) {
            STANDARD_DATA_FILE, BACKUP_DATA_FILE ->
                createStandardDesfireFileSettings(fileType, commSetting, accessRights, bytes, offset)
            LINEAR_RECORD_FILE, CYCLIC_RECORD_FILE ->
                createRecordDesfireFileSettings(fileType, commSetting, accessRights, bytes, offset)
            VALUE_FILE ->
                createValueDesfireFileSettings(fileType, commSetting, accessRights, bytes, offset)
            else ->
                throw RuntimeException("Unknown file type: " + fileType.toInt().toString(16))
        }
    }

    private fun createStandardDesfireFileSettings(
        fileType: Byte,
        commSetting: Byte,
        accessRights: ByteArray,
        bytes: ByteArray,
        startOffset: Int
    ): StandardDesfireFileSettings {
        val buf = ByteArray(3)
        bytes.copyInto(buf, 0, startOffset, startOffset + buf.size)
        buf.reverse()
        val fileSize = ByteUtils.byteArrayToInt(buf)
        return StandardDesfireFileSettings.create(fileType, commSetting, accessRights, fileSize)
    }

    private fun createRecordDesfireFileSettings(
        fileType: Byte,
        commSetting: Byte,
        accessRights: ByteArray,
        bytes: ByteArray,
        startOffset: Int
    ): RecordDesfireFileSettings {
        var offset = startOffset
        var buf = ByteArray(3)
        bytes.copyInto(buf, 0, offset, offset + buf.size)
        offset += buf.size
        buf.reverse()
        val recordSize = ByteUtils.byteArrayToInt(buf)

        buf = ByteArray(3)
        bytes.copyInto(buf, 0, offset, offset + buf.size)
        offset += buf.size
        buf.reverse()
        val maxRecords = ByteUtils.byteArrayToInt(buf)

        buf = ByteArray(3)
        bytes.copyInto(buf, 0, offset, offset + buf.size)
        buf.reverse()
        val curRecords = ByteUtils.byteArrayToInt(buf)

        return RecordDesfireFileSettings.create(
            fileType,
            commSetting,
            accessRights,
            recordSize,
            maxRecords,
            curRecords
        )
    }

    private fun createValueDesfireFileSettings(
        fileType: Byte,
        commSetting: Byte,
        accessRights: ByteArray,
        bytes: ByteArray,
        startOffset: Int
    ): ValueDesfireFileSettings {
        var offset = startOffset
        var buf = ByteArray(4)
        bytes.copyInto(buf, 0, offset, offset + buf.size)
        offset += buf.size
        buf.reverse()
        val lowerLimit = ByteUtils.byteArrayToInt(buf)

        buf = ByteArray(4)
        bytes.copyInto(buf, 0, offset, offset + buf.size)
        offset += buf.size
        buf.reverse()
        val upperLimit = ByteUtils.byteArrayToInt(buf)

        buf = ByteArray(4)
        bytes.copyInto(buf, 0, offset, offset + buf.size)
        offset += buf.size
        buf.reverse()
        val limitedCreditValue = ByteUtils.byteArrayToInt(buf)

        val limitedCreditEnabled = bytes[offset] != 0x00.toByte()

        return ValueDesfireFileSettings.create(
            fileType,
            commSetting,
            accessRights,
            lowerLimit,
            upperLimit,
            limitedCreditValue,
            limitedCreditEnabled
        )
    }

    companion object {
        fun create(data: ByteArray): RawDesfireFileSettings =
            RawDesfireFileSettings(data)
    }
}
