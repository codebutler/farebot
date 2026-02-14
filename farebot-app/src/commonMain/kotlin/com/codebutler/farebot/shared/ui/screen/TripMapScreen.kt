package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.transit.Station
import farebot.farebot_app.generated.resources.Res
import farebot.farebot_app.generated.resources.back
import farebot.farebot_app.generated.resources.no_location_data
import farebot.farebot_app.generated.resources.station_from
import farebot.farebot_app.generated.resources.station_to
import farebot.farebot_app.generated.resources.trip_map
import farebot.farebot_app.generated.resources.unknown_station
import org.jetbrains.compose.resources.stringResource

data class TripMapUiState(
    val startStation: Station? = null,
    val endStation: Station? = null,
    val routeName: String? = null,
    val agencyName: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripMapScreen(
    uiState: TripMapUiState,
    onBack: () -> Unit,
) {
    val startName = uiState.startStation?.shortStationNameRaw ?: uiState.startStation?.stationName
    val endName = uiState.endStation?.shortStationNameRaw ?: uiState.endStation?.stationName
    val title =
        if (startName != null && endName != null) {
            "$startName \u2192 $endName"
        } else {
            uiState.routeName ?: stringResource(Res.string.trip_map)
        }
    val subtitle =
        listOfNotNull(uiState.agencyName, uiState.routeName)
            .joinToString(" ")
            .takeIf { it.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = title)
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
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
                    .padding(16.dp),
        ) {
            val hasStart = uiState.startStation?.hasLocation() == true
            val hasEnd = uiState.endStation?.hasLocation() == true

            if (hasStart || hasEnd) {
                // Station location details
                if (hasStart) {
                    StationCard(
                        label = stringResource(Res.string.station_from),
                        station = uiState.startStation,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (hasStart && hasEnd) {
                    // Visual connector between stations
                    Row(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Canvas(modifier = Modifier.size(width = 2.dp, height = 32.dp)) {
                            drawLine(
                                color = Color.Gray,
                                start = Offset(size.width / 2, 0f),
                                end = Offset(size.width / 2, size.height),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }

                if (hasEnd) {
                    StationCard(
                        label = stringResource(Res.string.station_to),
                        station = uiState.endStation,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Platform-specific map
                PlatformTripMap(uiState)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.no_location_data),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
expect fun PlatformTripMap(uiState: TripMapUiState)

@Composable
private fun StationCard(
    label: String,
    station: Station,
    color: Color,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2)
            drawCircle(color = Color.White, radius = size.minDimension / 4)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = station.stationName ?: stringResource(Res.string.unknown_station),
                style = MaterialTheme.typography.titleMedium,
            )
            val lineName = station.lineNames.firstOrNull()
            if (lineName != null) {
                Text(
                    text = lineName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
