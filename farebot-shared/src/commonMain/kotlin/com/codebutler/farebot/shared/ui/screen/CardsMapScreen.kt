package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import farebot.farebot_shared.generated.resources.Res
import farebot.farebot_shared.generated.resources.back
import farebot.farebot_shared.generated.resources.cards_map
import org.jetbrains.compose.resources.stringResource

data class CardsMapMarker(
    val name: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
)

data class CardsMapUiState(
    val markers: List<CardsMapMarker> = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsMapScreen(
    uiState: CardsMapUiState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.cards_map)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        }
    ) { padding ->
        PlatformCardsMap(
            markers = uiState.markers,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

@Composable
expect fun PlatformCardsMap(markers: List<CardsMapMarker>, modifier: Modifier)
