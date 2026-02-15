/*
 * RawDesfireApplication.kt
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

import com.codebutler.farebot.card.desfire.DesfireApplication
import com.codebutler.farebot.card.desfire.DesfireAuthLog
import kotlinx.serialization.Serializable

@Serializable
data class RawDesfireApplication(
    val appId: Int,
    val files: List<RawDesfireFile>,
    val authLog: List<DesfireAuthLog> = emptyList(),
    val dirListLocked: Boolean = false,
) {
    fun appId(): Int = appId

    fun files(): List<RawDesfireFile> = files

    fun parse(): DesfireApplication {
        val parsedFiles = files.map { it.parse() }
        return DesfireApplication.create(appId, parsedFiles, authLog, dirListLocked)
    }

    companion object {
        fun create(
            appId: Int,
            rawDesfireFiles: List<RawDesfireFile>,
            authLog: List<DesfireAuthLog> = emptyList(),
            dirListLocked: Boolean = false,
        ): RawDesfireApplication = RawDesfireApplication(appId, rawDesfireFiles, authLog, dirListLocked)
    }
}
