/*
 * CardScanner.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.shared.nfc

import com.codebutler.farebot.card.RawCard
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class ScannedTag(val id: ByteArray, val techList: List<String>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScannedTag) return false
        return id.contentEquals(other.id) && techList == other.techList
    }
    override fun hashCode(): Int = id.contentHashCode() * 31 + techList.hashCode()
}

/**
 * Platform-agnostic interface for NFC card scanning.
 *
 * Supports two scanning modes:
 * - **Passive**: Cards arrive automatically (Android NFC foreground dispatch).
 *   Observe [scannedCards] flow.
 * - **Active**: User explicitly starts a scan session (iOS Core NFC).
 *   Call [startActiveScan] which emits results to [scannedCards].
 */
interface CardScanner {

    /** Whether this platform requires user-initiated scanning (e.g., iOS Core NFC). */
    val requiresActiveScan: Boolean get() = true

    /** Flow of raw tag detections before card reading. */
    val scannedTags: SharedFlow<ScannedTag>
        get() = MutableSharedFlow() // default empty

    /** Flow of scanned cards from any scanning mode. */
    val scannedCards: SharedFlow<RawCard<*>>

    /** Flow of scan errors. */
    val scanErrors: SharedFlow<Throwable>

    /** Whether scanning is currently in progress. */
    val isScanning: StateFlow<Boolean>

    /**
     * Start an active scan session (e.g., iOS NFC dialog).
     * Results are emitted to [scannedCards].
     * No-op on platforms with passive scanning.
     */
    fun startActiveScan()

    /** Stop the active scan session. */
    fun stopActiveScan()
}
