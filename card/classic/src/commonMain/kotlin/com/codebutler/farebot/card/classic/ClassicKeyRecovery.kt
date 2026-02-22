/*
 * ClassicKeyRecovery.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2026 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic

import com.codebutler.farebot.card.nfc.pn533.PN533ClassicTechnology

/**
 * Interface for MIFARE Classic key recovery.
 *
 * Decouples [ClassicCardReader] from specific key recovery implementations
 * (e.g., nested attack via Crypto1). Implementations live in the `:keymanager`
 * module, which is excluded from iOS builds.
 */
fun interface ClassicKeyRecovery {
    /**
     * Attempt to recover a key for the given sector using a known key from another sector.
     *
     * @param tech The PN533 Classic technology interface for hardware communication
     * @param sectorIndex The sector to recover a key for
     * @param knownKeys Map of sector index to (key bytes, isKeyA) for already-known keys
     * @param onProgress Optional callback for progress reporting
     * @return Pair of (recovered key bytes, isKeyA), or null if recovery failed
     */
    suspend fun attemptRecovery(
        tech: PN533ClassicTechnology,
        sectorIndex: Int,
        knownKeys: Map<Int, Pair<ByteArray, Boolean>>,
        onProgress: ((String) -> Unit)?,
    ): Pair<ByteArray, Boolean>?
}
