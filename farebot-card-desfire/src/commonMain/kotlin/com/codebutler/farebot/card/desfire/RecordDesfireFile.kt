/*
 * RecordDesfireFile.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2014, 2016 Eric Butler <eric@codebutler.com>
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

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class RecordDesfireFile(
    override val id: Int,
    @Contextual override val fileSettings: DesfireFileSettings,
    val records: List<DesfireRecord>,
    @Deprecated("Use records instead.")
    @Contextual val data: ByteArray
) : DesfireFile {

    companion object {
        fun create(
            fileId: Int,
            fileSettings: DesfireFileSettings,
            fileData: ByteArray
        ): RecordDesfireFile {
            val settings = fileSettings as RecordDesfireFileSettings
            val records = (0 until settings.curRecords).map { i ->
                val start = settings.recordSize * i
                val end = start + settings.recordSize
                DesfireRecord.create(fileData.copyOfRange(start, end))
            }
            return RecordDesfireFile(
                fileId,
                fileSettings,
                records,
                fileData
            )
        }
    }
}
