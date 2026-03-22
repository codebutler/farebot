/*
 * BaseCardScanner.kt
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

package com.codebutler.farebot.shared.nfc

import com.codebutler.farebot.card.RawCard
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Base implementation for [CardScanner] that provides shared flow
 * declarations and emit helpers.
 *
 * All platform scanners extend this to avoid duplicating flow/state
 * boilerplate. Platform-specific scan lifecycle (starting, stopping,
 * hardware access) is implemented in each subclass.
 */
abstract class BaseCardScanner : CardScanner {
    private val mutableScannedTags = MutableSharedFlow<ScannedTag>(extraBufferCapacity = 1)
    override val scannedTags: SharedFlow<ScannedTag> = mutableScannedTags.asSharedFlow()

    private val mutableScannedCards = MutableSharedFlow<RawCard<*>>(extraBufferCapacity = 1)
    override val scannedCards: SharedFlow<RawCard<*>> = mutableScannedCards.asSharedFlow()

    private val mutableScanErrors = MutableSharedFlow<Throwable>(extraBufferCapacity = 1)
    override val scanErrors: SharedFlow<Throwable> = mutableScanErrors.asSharedFlow()

    private val mutableIsScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = mutableIsScanning.asStateFlow()

    private val mutableReadingProgress = MutableStateFlow<ReadingProgress?>(null)
    override val readingProgress: StateFlow<ReadingProgress?> = mutableReadingProgress.asStateFlow()

    private val mutablePartialCardData = MutableStateFlow<RawCard<*>?>(null)
    override val partialCardData: StateFlow<RawCard<*>?> = mutablePartialCardData.asStateFlow()

    private val mutableRecoveryProgress = MutableStateFlow<RecoveryProgressInfo?>(null)
    override val recoveryProgress: StateFlow<RecoveryProgressInfo?> = mutableRecoveryProgress.asStateFlow()

    protected var recoveryMode = false

    override fun startRecoveryScan() {
        recoveryMode = true
        startActiveScan()
    }

    protected fun emitTag(tag: ScannedTag) {
        mutableScannedTags.tryEmit(tag)
    }

    protected fun emitCard(rawCard: RawCard<*>) {
        mutableReadingProgress.value = null
        mutablePartialCardData.value = null
        mutableRecoveryProgress.value = null
        mutableScannedCards.tryEmit(rawCard)
    }

    protected fun emitError(error: Throwable) {
        if (!mutableIsScanning.value) return
        mutableReadingProgress.value = null
        mutablePartialCardData.value = null
        mutableRecoveryProgress.value = null
        mutableScanErrors.tryEmit(error)
    }

    protected fun emitProgress(
        current: Int,
        total: Int,
    ) {
        mutableReadingProgress.value = ReadingProgress(current, total)
    }

    protected fun emitPartialCard(card: RawCard<*>) {
        mutablePartialCardData.value = card
    }

    protected fun emitRecoveryProgress(info: RecoveryProgressInfo) {
        mutableRecoveryProgress.value = info
    }

    protected fun setScanning(value: Boolean) {
        mutableIsScanning.value = value
    }

    protected fun resetScanState() {
        recoveryMode = false
        mutableIsScanning.value = false
        mutableReadingProgress.value = null
        mutablePartialCardData.value = null
        mutableRecoveryProgress.value = null
    }
}
