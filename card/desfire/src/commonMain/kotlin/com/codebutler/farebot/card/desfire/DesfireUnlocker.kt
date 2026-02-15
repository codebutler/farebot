/*
 * DesfireUnlocker.kt
 *
 * Copyright 2018 Google
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

import com.codebutler.farebot.card.desfire.raw.RawDesfireFile

internal interface DesfireUnlocker {
    // get the order for file reading as some keys may depend on reading some files
    fun getOrder(
        desfireTag: DesfireProtocol,
        fileIds: IntArray,
    ): IntArray

    // Unlock a given file.
    fun unlock(
        desfireTag: DesfireProtocol,
        files: Map<Int, RawDesfireFile>,
        fileId: Int,
        authLog: MutableList<DesfireAuthLog>,
    )
}
