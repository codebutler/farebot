package com.codebutler.farebot.shared.ui.screen

import androidx.compose.animation.Crossfade
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
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.card.CardType
import farebot.app.generated.resources.Res
import farebot.app.generated.resources.add_key
import farebot.app.generated.resources.back
import farebot.app.generated.resources.card_id
import farebot.app.generated.resources.card_type
import farebot.app.generated.resources.enter_manually
import farebot.app.generated.resources.hold_nfc_card
import farebot.app.generated.resources.import_file_button
import farebot.app.generated.resources.key_data
import farebot.app.generated.resources.nfc
import farebot.app.generated.resources.tap_your_card
import org.jetbrains.compose.resources.stringResource

data class AddKeyUiState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val hasNfc: Boolean = false,
    val detectedTagId: String? = null,
    val detectedCardType: CardType? = null,
    val importedKeyData: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKeyScreen(
    uiState: AddKeyUiState,
    onBack: () -> Unit,
    onSaveKey: (cardId: String, cardType: CardType, keyData: String) -> Unit,
    onEnterManually: () -> Unit = {},
    onImportFile: () -> Unit = {},
) {
    val isAutoDetected = uiState.detectedTagId != null && uiState.detectedTagId.isNotEmpty()
    var cardId by remember(uiState.detectedTagId) {
        mutableStateOf(uiState.detectedTagId ?: "")
    }
    var keyData by remember(uiState.importedKeyData) {
        mutableStateOf(uiState.importedKeyData ?: "")
    }
    var selectedCardType by remember(uiState.detectedCardType) {
        mutableStateOf(uiState.detectedCardType ?: CardType.MifareClassic)
    }
    var cardTypeExpanded by remember { mutableStateOf(false) }

    val cardTypes =
        remember {
            listOf(
                CardType.MifareClassic,
                CardType.MifareDesfire,
                CardType.FeliCa,
                CardType.CEPAS,
            )
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.add_key)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Crossfade(targetState = uiState.detectedTagId != null) { showForm ->
            if (!showForm && uiState.hasNfc) {
                // NFC splash - waiting for tag
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Default.Nfc,
                        contentDescription = stringResource(Res.string.nfc),
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.tap_your_card),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.hold_nfc_card),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = onEnterManually) {
                        Text(stringResource(Res.string.enter_manually))
                    }
                }
            } else {
                // Key entry form
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                ) {
                    OutlinedTextField(
                        value = cardId,
                        onValueChange = { if (!isAutoDetected) cardId = it },
                        label = { Text(stringResource(Res.string.card_id)) },
                        singleLine = true,
                        readOnly = isAutoDetected,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = cardTypeExpanded,
                        onExpandedChange = { if (!isAutoDetected) cardTypeExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedCardType.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.card_type)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardTypeExpanded) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        )
                        if (!isAutoDetected) {
                            ExposedDropdownMenu(
                                expanded = cardTypeExpanded,
                                onDismissRequest = { cardTypeExpanded = false },
                            ) {
                                cardTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.toString()) },
                                        onClick = {
                                            selectedCardType = type
                                            cardTypeExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = keyData,
                        onValueChange = { keyData = it },
                        label = { Text(stringResource(Res.string.key_data)) },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onImportFile,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.import_file_button))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onSaveKey(cardId, selectedCardType, keyData) },
                        enabled = cardId.isNotBlank() && keyData.isNotBlank() && !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.add_key))
                    }
                }
            }
        }
    }
}
