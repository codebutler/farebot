/*
 * DesfireFileSettings.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

abstract class DesfireFileSettings {
    abstract val fileType: Byte
    abstract val commSetting: Byte
    abstract val accessRights: ByteArray

    // FIXME: Localize
    val fileTypeName: String
        get() =
            when (fileType) {
                STANDARD_DATA_FILE -> "Standard"
                BACKUP_DATA_FILE -> "Backup"
                VALUE_FILE -> "Value"
                LINEAR_RECORD_FILE -> "Linear Record"
                CYCLIC_RECORD_FILE -> "Cyclic Record"
                else -> "Unknown"
            }

    companion object {
        // DesfireFile Types
        const val STANDARD_DATA_FILE: Byte = 0x00
        const val BACKUP_DATA_FILE: Byte = 0x01
        const val VALUE_FILE: Byte = 0x02
        const val LINEAR_RECORD_FILE: Byte = 0x03
        const val CYCLIC_RECORD_FILE: Byte = 0x04
    }
}
