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

/**
 * Platform-specific accessor for reading bundled MdST station database files.
 */
expect object ResourceAccessor {
    /**
     * Opens an MdST file from bundled assets and returns its contents as a ByteArray.
     * @param dbName The name of the MdST file (without extension), e.g. "orca"
     * @return The file contents, or null if the file could not be found.
     */
    fun openMdstFile(dbName: String): ByteArray?
}
