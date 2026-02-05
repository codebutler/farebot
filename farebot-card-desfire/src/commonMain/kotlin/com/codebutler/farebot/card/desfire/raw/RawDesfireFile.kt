/*
 * RawDesfireFile.kt
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

import com.codebutler.farebot.card.desfire.DesfireFile
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import com.codebutler.farebot.card.desfire.DesfireFileSettings
import com.codebutler.farebot.card.desfire.DesfireFileSettings.Companion.BACKUP_DATA_FILE
import com.codebutler.farebot.card.desfire.DesfireFileSettings.Companion.CYCLIC_RECORD_FILE
import com.codebutler.farebot.card.desfire.DesfireFileSettings.Companion.LINEAR_RECORD_FILE
import com.codebutler.farebot.card.desfire.DesfireFileSettings.Companion.STANDARD_DATA_FILE
import com.codebutler.farebot.card.desfire.DesfireFileSettings.Companion.VALUE_FILE
import com.codebutler.farebot.card.desfire.InvalidDesfireFile
import com.codebutler.farebot.card.desfire.RecordDesfireFile
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.card.desfire.UnauthorizedDesfireFile
import com.codebutler.farebot.card.desfire.ValueDesfireFile

@Serializable
data class RawDesfireFile(
    val fileId: Int,
    val fileSettings: RawDesfireFileSettings,
    @Contextual val fileData: ByteArray?,
    val error: Error?
) {
    fun fileId(): Int = fileId
    fun fileSettings(): RawDesfireFileSettings = fileSettings
    fun fileData(): ByteArray? = fileData
    fun error(): Error? = error

    fun parse(): DesfireFile {
        val error = error
        if (error != null) {
            return if (error.type == Error.TYPE_UNAUTHORIZED) {
                UnauthorizedDesfireFile.create(fileId, fileSettings.parse(), error.message ?: "")
            } else {
                InvalidDesfireFile.create(fileId, fileSettings.parse(), error.message ?: "")
            }
        }
        val data = fileData ?: throw RuntimeException("fileData was null")
        val parsedFileSettings = fileSettings.parse()
        return when (parsedFileSettings.fileType) {
            STANDARD_DATA_FILE, BACKUP_DATA_FILE ->
                StandardDesfireFile.create(fileId, parsedFileSettings, data)
            LINEAR_RECORD_FILE, CYCLIC_RECORD_FILE ->
                RecordDesfireFile.create(fileId, parsedFileSettings, data)
            VALUE_FILE ->
                ValueDesfireFile.create(fileId, parsedFileSettings, data)
            else ->
                throw RuntimeException("Unknown file type: " + parsedFileSettings.fileType.toInt().toString(16))
        }
    }

    @Serializable
    data class Error(
        val type: Int,
        val message: String?
    ) {
        fun type(): Int = type
        fun message(): String? = message

        companion object {
            const val TYPE_NONE = 0
            const val TYPE_UNAUTHORIZED = 1
            const val TYPE_INVALID = 2

            fun create(type: Int, message: String): Error = Error(type, message)
        }
    }

    companion object {
        fun create(
            fileId: Int,
            fileSettings: RawDesfireFileSettings,
            fileData: ByteArray
        ): RawDesfireFile = RawDesfireFile(fileId, fileSettings, fileData, null)

        fun createUnauthorized(
            fileId: Int,
            fileSettings: RawDesfireFileSettings,
            errorMessage: String
        ): RawDesfireFile {
            val error = Error.create(Error.TYPE_UNAUTHORIZED, errorMessage)
            return RawDesfireFile(fileId, fileSettings, null, error)
        }

        fun createInvalid(
            fileId: Int,
            fileSettings: RawDesfireFileSettings,
            errorMessage: String
        ): RawDesfireFile {
            val error = Error.create(Error.TYPE_INVALID, errorMessage)
            return RawDesfireFile(fileId, fileSettings, null, error)
        }
    }
}
