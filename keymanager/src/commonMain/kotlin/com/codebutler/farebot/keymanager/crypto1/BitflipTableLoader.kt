/*
 * BitflipTableLoader.kt
 *
 * Copyright 2026 Eric Butler <eric@codebutler.com>
 *
 * Platform-specific loader for the HBFT (Hardnested BitFlip Table) resource.
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

package com.codebutler.farebot.keymanager.crypto1

expect object BitflipTableLoader {
    /**
     * Load the even-half HBFT binary resource.
     * @return The raw bytes, or null if not available on this platform.
     */
    fun loadEvenBitflipResource(): ByteArray?

    /**
     * Load the odd-half HBFT binary resource.
     * @return The raw bytes, or null if not available on this platform.
     */
    fun loadOddBitflipResource(): ByteArray?
}
