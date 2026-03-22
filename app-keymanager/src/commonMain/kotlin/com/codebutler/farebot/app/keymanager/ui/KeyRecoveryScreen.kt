package com.codebutler.farebot.app.keymanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.shared.nfc.ReadingProgress
import com.codebutler.farebot.shared.nfc.RecoveryPhase
import com.codebutler.farebot.shared.nfc.RecoveryProgressInfo
import farebot.app_keymanager.generated.resources.Res
import farebot.app_keymanager.generated.resources.back
import farebot.app_keymanager.generated.resources.brute_forcing
import farebot.app_keymanager.generated.resources.card_id
import farebot.app_keymanager.generated.resources.card_type
import farebot.app_keymanager.generated.resources.collecting_nonces
import farebot.app_keymanager.generated.resources.engine_label
import farebot.app_keymanager.generated.resources.initializing_attack
import farebot.app_keymanager.generated.resources.keys_recovered_count
import farebot.app_keymanager.generated.resources.processing_nonces
import farebot.app_keymanager.generated.resources.reading_sector_progress
import farebot.app_keymanager.generated.resources.recover_key
import farebot.app_keymanager.generated.resources.recover_key_instructions
import farebot.app_keymanager.generated.resources.recovering_sector_progress
import farebot.app_keymanager.generated.resources.recovery_failed
import farebot.app_keymanager.generated.resources.recovery_success
import farebot.app_keymanager.generated.resources.scanning_for_card
import farebot.app_keymanager.generated.resources.start_recovery
import farebot.app_keymanager.generated.resources.unknown_error
import org.jetbrains.compose.resources.stringResource

data class KeyRecoveryUiState(
    val tagId: String = "",
    val cardType: CardType = CardType.MifareClassic,
    val isRecovering: Boolean = false,
    val isWaitingForCard: Boolean = false,
    val readingProgress: ReadingProgress? = null,
    val recoveryProgress: RecoveryProgressInfo? = null,
    val error: String? = null,
    val success: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyRecoveryScreen(
    uiState: KeyRecoveryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.recover_key)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Default.VpnKey,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = stringResource(Res.string.recover_key),
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(
                text = stringResource(Res.string.recover_key_instructions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${stringResource(Res.string.card_type)}: ${uiState.cardType.name}",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = "${stringResource(Res.string.card_id)}: ${uiState.tagId}",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isRecovering) {
                val recovery = uiState.recoveryProgress
                val progress = uiState.readingProgress

                // Engine name header
                val engineName = recovery?.engineName
                if (engineName != null) {
                    Text(
                        text = stringResource(Res.string.engine_label, engineName),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                // Progress indicator
                val currentSector = recovery?.currentSector
                val totalSectors = recovery?.totalSectors
                if (currentSector != null && totalSectors != null && totalSectors > 0) {
                    LinearProgressIndicator(
                        progress = { currentSector.toFloat() / totalSectors.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress.current.toFloat() / progress.total.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                // Phase / sector progress text
                val phaseText =
                    when {
                        recovery != null -> {
                            val sectorText =
                                if (currentSector != null && totalSectors != null) {
                                    stringResource(
                                        Res.string.recovering_sector_progress,
                                        currentSector + 1,
                                        totalSectors,
                                    )
                                } else {
                                    null
                                }
                            val phaseLabel =
                                when (recovery.phase) {
                                    RecoveryPhase.Initializing -> stringResource(Res.string.initializing_attack)
                                    RecoveryPhase.CollectingNonces -> stringResource(Res.string.collecting_nonces)
                                    RecoveryPhase.ProcessingNonces -> stringResource(Res.string.processing_nonces)
                                    RecoveryPhase.BruteForcing -> stringResource(Res.string.brute_forcing)
                                    RecoveryPhase.KeyFound -> stringResource(Res.string.recovery_success)
                                    RecoveryPhase.Failed -> stringResource(Res.string.recovery_failed)
                                }
                            if (sectorText != null) "$sectorText — $phaseLabel" else phaseLabel
                        }
                        progress != null ->
                            stringResource(
                                Res.string.reading_sector_progress,
                                progress.current,
                                progress.total,
                            )
                        uiState.isWaitingForCard -> stringResource(Res.string.scanning_for_card)
                        else -> null
                    }
                if (phaseText != null) {
                    Text(
                        text = phaseText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Progress detail line
                val detail = recovery?.progressDetail
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }

                // Keys recovered count
                val keysRecovered = recovery?.recoveredKeys ?: 0
                if (keysRecovered > 0) {
                    Text(
                        text = stringResource(Res.string.keys_recovered_count, keysRecovered),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error.ifEmpty { stringResource(Res.string.unknown_error) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (uiState.success) {
                Text(
                    text = stringResource(Res.string.recovery_success),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (!uiState.isRecovering && uiState.error != null) {
                Button(onClick = onRetry) {
                    Text(stringResource(Res.string.start_recovery))
                }
            }
        }
    }
}
