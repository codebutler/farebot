/*
 * ScanningSheet.kt
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

package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.shared.nfc.ReadingProgress
import farebot.app.generated.resources.Res
import farebot.app.generated.resources.cancel
import farebot.app.generated.resources.hold_card_near_reader
import farebot.app.generated.resources.reading_card
import farebot.app.generated.resources.reading_card_type
import org.jetbrains.compose.resources.stringResource

/**
 * Modal bottom sheet displayed while scanning/reading an NFC card.
 *
 * Shows an NFC icon, status text (card type and/or transit system name),
 * a progress bar, and an optional cancel button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanningSheet(
    isReadingCard: Boolean,
    detectedCardType: CardType?,
    identifiedTransitName: String?,
    readingProgress: ReadingProgress?,
    requiresActiveScan: Boolean,
    onCancel: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Default.Nfc,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Text(
                text =
                    when {
                        identifiedTransitName != null ->
                            stringResource(Res.string.reading_card_type, identifiedTransitName)
                        isReadingCard && detectedCardType != null ->
                            stringResource(Res.string.reading_card_type, detectedCardType.toString())
                        isReadingCard ->
                            stringResource(Res.string.reading_card)
                        else ->
                            stringResource(Res.string.hold_card_near_reader)
                    },
                style = MaterialTheme.typography.titleMedium,
            )

            if (readingProgress != null) {
                LinearProgressIndicator(
                    progress = { readingProgress.current.toFloat() / readingProgress.total.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
                Text(
                    text = "${readingProgress.current} / ${readingProgress.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
            }

            if (requiresActiveScan) {
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        }
    }
}
