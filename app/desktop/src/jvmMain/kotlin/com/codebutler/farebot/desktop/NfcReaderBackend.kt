/*
 * NfcReaderBackend.kt
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

package com.codebutler.farebot.desktop

import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.shared.nfc.ScannedTag

/**
 * Abstraction for an NFC reader backend.
 *
 * Each backend represents a different hardware interface for reading
 * NFC cards (e.g., PC/SC via javax.smartcardio, or raw USB via PN533).
 * [DesktopCardScanner] launches one coroutine per backend; backends that
 * fail to find hardware throw from [scanLoop] and the error is logged.
 */
interface NfcReaderBackend {
    val name: String

    suspend fun scanLoop(
        onCardDetected: (ScannedTag) -> Unit,
        onCardRead: (RawCard<*>) -> Unit,
        onError: (Throwable) -> Unit,
    )
}
