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

import farebot.farebot_base.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi

actual object ResourceAccessor {
    @OptIn(ExperimentalResourceApi::class)
    actual fun openMdstFile(dbName: String): ByteArray? {
        return try {
            runBlocking {
                Res.readBytes("files/$dbName.mdst")
            }
        } catch (e: Exception) {
            null
        }
    }
}
