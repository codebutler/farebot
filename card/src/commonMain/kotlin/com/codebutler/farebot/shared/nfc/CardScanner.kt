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

import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

data class ReadingProgress(
    val current: Int,
    val total: Int,
)

/**
 * Structured key recovery progress info, surfaced from the hardnested attack engine.
 */
data class RecoveryProgressInfo(
    /** Current recovery phase. */
    val phase: RecoveryPhase = RecoveryPhase.Initializing,
    /** The sector currently being recovered, if applicable. */
    val currentSector: Int? = null,
    /** Total number of sectors on the card. */
    val totalSectors: Int? = null,
    /** Name of the brute force engine in use (e.g., "Metal GPU: Apple M2 Max"). */
    val engineName: String? = null,
    /** Number of keys recovered so far across all sectors. */
    val recoveredKeys: Int = 0,
    /** Latest detail message from the attack engine. */
    val progressDetail: String? = null,
)

/** Phases of the hardnested key recovery process. */
enum class RecoveryPhase {
    Initializing,
    CollectingNonces,
    ProcessingNonces,
    BruteForcing,
    KeyFound,
    Failed,
}

data class ScannedTag(
    val id: ByteArray,
    val cardType: CardType? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScannedTag) return false
        return id.contentEquals(other.id) && cardType == other.cardType
    }

    override fun hashCode(): Int = id.contentHashCode() * 31 + (cardType?.hashCode() ?: 0)
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

    /** Reading progress — non-null when a card read is in progress with known total. */
    val readingProgress: StateFlow<ReadingProgress?>
        get() = MutableStateFlow(null)

    /** Partial card data emitted during reading for incremental transit identification. */
    val partialCardData: StateFlow<RawCard<*>?>
        get() = MutableStateFlow(null)

    /** Whether this scanner supports MIFARE Classic key recovery (requires PN533 raw access). */
    val supportsKeyRecovery: Boolean get() = false

    /** Key recovery progress detail — latest status message from the recovery engine. */
    val recoveryProgress: StateFlow<RecoveryProgressInfo?>
        get() = MutableStateFlow(null)

    /**
     * Start an active scan session (e.g., iOS NFC dialog).
     * Results are emitted to [scannedCards].
     * No-op on platforms with passive scanning.
     */
    fun startActiveScan()

    /**
     * Start an active scan with MIFARE Classic key recovery enabled.
     * Defaults to [startActiveScan] on scanners that don't support recovery.
     */
    fun startRecoveryScan() {
        startActiveScan()
    }

    /** Stop the active scan session. */
    fun stopActiveScan()
}
