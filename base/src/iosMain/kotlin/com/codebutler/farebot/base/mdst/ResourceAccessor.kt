/*
 * ResourceAccessor.kt
 *
 * Copyright 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.base.mdst

import farebot.base.generated.resources.Res
import kotlinx.coroutines.runBlocking

actual object ResourceAccessor {
    actual fun openMdstFile(dbName: String): ByteArray? =
        try {
            runBlocking {
                Res.readBytes("files/$dbName.mdst")
            }
        } catch (_: Exception) {
            null
        }
}
