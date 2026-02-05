/*
 * SettingsScreen.kt
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

package com.codebutler.farebot.shared.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codebutler.farebot.shared.settings.RawLevel
import com.codebutler.farebot.shared.viewmodel.SettingsViewModel
import farebot.farebot_shared.generated.resources.Res
import farebot.farebot_shared.generated.resources.back
import farebot.farebot_shared.generated.resources.ok
import farebot.farebot_shared.generated.resources.pref_category_debug
import farebot.farebot_shared.generated.resources.pref_category_general
import farebot.farebot_shared.generated.resources.pref_category_localization
import farebot.farebot_shared.generated.resources.pref_category_obfuscation
import farebot.farebot_shared.generated.resources.pref_category_obfuscation_desc
import farebot.farebot_shared.generated.resources.pref_category_raw_data
import farebot.farebot_shared.generated.resources.pref_convert_timezone
import farebot.farebot_shared.generated.resources.pref_convert_timezone_desc
import farebot.farebot_shared.generated.resources.pref_debug_logging
import farebot.farebot_shared.generated.resources.pref_debug_logging_desc
import farebot.farebot_shared.generated.resources.pref_debug_spans
import farebot.farebot_shared.generated.resources.pref_debug_spans_desc
import farebot.farebot_shared.generated.resources.pref_hide_card_numbers
import farebot.farebot_shared.generated.resources.pref_hide_card_numbers_desc
import farebot.farebot_shared.generated.resources.pref_iso_datetime
import farebot.farebot_shared.generated.resources.pref_iso_datetime_desc
import farebot.farebot_shared.generated.resources.pref_launch_from_background
import farebot.farebot_shared.generated.resources.pref_launch_from_background_desc
import farebot.farebot_shared.generated.resources.pref_localise_places
import farebot.farebot_shared.generated.resources.pref_localise_places_desc
import farebot.farebot_shared.generated.resources.pref_obfuscate_balance
import farebot.farebot_shared.generated.resources.pref_obfuscate_balance_desc
import farebot.farebot_shared.generated.resources.pref_obfuscate_trip_dates
import farebot.farebot_shared.generated.resources.pref_obfuscate_trip_dates_desc
import farebot.farebot_shared.generated.resources.pref_obfuscate_trip_fares
import farebot.farebot_shared.generated.resources.pref_obfuscate_trip_fares_desc
import farebot.farebot_shared.generated.resources.pref_obfuscate_trip_times
import farebot.farebot_shared.generated.resources.pref_obfuscate_trip_times_desc
import farebot.farebot_shared.generated.resources.pref_raw_level
import farebot.farebot_shared.generated.resources.pref_raw_level_all
import farebot.farebot_shared.generated.resources.pref_raw_level_desc
import farebot.farebot_shared.generated.resources.pref_raw_level_none
import farebot.farebot_shared.generated.resources.pref_raw_level_unknown
import farebot.farebot_shared.generated.resources.pref_show_local_and_english
import farebot.farebot_shared.generated.resources.pref_show_local_and_english_desc
import farebot.farebot_shared.generated.resources.pref_show_raw_station_ids
import farebot.farebot_shared.generated.resources.pref_show_raw_station_ids_desc
import farebot.farebot_shared.generated.resources.pref_speak_balance
import farebot.farebot_shared.generated.resources.pref_speak_balance_desc
import farebot.farebot_shared.generated.resources.preferences
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()

    // Collect all state flows
    val launchFromBackground by viewModel.launchFromBackground.collectAsState()
    val rawLevel by viewModel.rawLevel.collectAsState()
    val showRawStationIds by viewModel.showRawStationIds.collectAsState()
    val hideCardNumbers by viewModel.hideCardNumbers.collectAsState()
    val obfuscateBalance by viewModel.obfuscateBalance.collectAsState()
    val obfuscateTripFares by viewModel.obfuscateTripFares.collectAsState()
    val obfuscateTripDates by viewModel.obfuscateTripDates.collectAsState()
    val obfuscateTripTimes by viewModel.obfuscateTripTimes.collectAsState()
    val showBothLocalAndEnglish by viewModel.showBothLocalAndEnglish.collectAsState()
    val convertTimezone by viewModel.convertTimezone.collectAsState()
    val localisePlaces by viewModel.localisePlaces.collectAsState()
    val debugLogging by viewModel.debugLogging.collectAsState()
    val debugSpans by viewModel.debugSpans.collectAsState()
    val useIsoDateTimeStamps by viewModel.useIsoDateTimeStamps.collectAsState()
    val speakBalance by viewModel.speakBalance.collectAsState()

    var showRawLevelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.preferences)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // ========== General ==========
            if (viewModel.supportsLaunchFromBackground) {
                SettingsCategoryHeader(stringResource(Res.string.pref_category_general))

                SwitchPreference(
                    title = stringResource(Res.string.pref_launch_from_background),
                    description = stringResource(Res.string.pref_launch_from_background_desc),
                    checked = launchFromBackground,
                    onCheckedChange = viewModel::setLaunchFromBackground
                )
            }

            // ========== Raw Data Display ==========
            SettingsCategoryHeader(stringResource(Res.string.pref_category_raw_data))

            ListPreference(
                title = stringResource(Res.string.pref_raw_level),
                description = stringResource(Res.string.pref_raw_level_desc),
                currentValue = when (rawLevel) {
                    RawLevel.NONE -> stringResource(Res.string.pref_raw_level_none)
                    RawLevel.UNKNOWN_ONLY -> stringResource(Res.string.pref_raw_level_unknown)
                    RawLevel.ALL -> stringResource(Res.string.pref_raw_level_all)
                },
                onClick = { showRawLevelDialog = true }
            )

            SwitchPreference(
                title = stringResource(Res.string.pref_show_raw_station_ids),
                description = stringResource(Res.string.pref_show_raw_station_ids_desc),
                checked = showRawStationIds,
                onCheckedChange = viewModel::setShowRawStationIds
            )

            // ========== Obfuscation ==========
            SettingsCategoryHeader(
                title = stringResource(Res.string.pref_category_obfuscation),
                description = stringResource(Res.string.pref_category_obfuscation_desc)
            )

            SwitchPreference(
                title = stringResource(Res.string.pref_hide_card_numbers),
                description = stringResource(Res.string.pref_hide_card_numbers_desc),
                checked = hideCardNumbers,
                onCheckedChange = viewModel::setHideCardNumbers
            )

            SwitchPreference(
                title = stringResource(Res.string.pref_obfuscate_balance),
                description = stringResource(Res.string.pref_obfuscate_balance_desc),
                checked = obfuscateBalance,
                onCheckedChange = viewModel::setObfuscateBalance
            )

            SwitchPreference(
                title = stringResource(Res.string.pref_obfuscate_trip_fares),
                description = stringResource(Res.string.pref_obfuscate_trip_fares_desc),
                checked = obfuscateTripFares,
                onCheckedChange = viewModel::setObfuscateTripFares
            )

            SwitchPreference(
                title = stringResource(Res.string.pref_obfuscate_trip_dates),
                description = stringResource(Res.string.pref_obfuscate_trip_dates_desc),
                checked = obfuscateTripDates,
                onCheckedChange = viewModel::setObfuscateTripDates
            )

            SwitchPreference(
                title = stringResource(Res.string.pref_obfuscate_trip_times),
                description = stringResource(Res.string.pref_obfuscate_trip_times_desc),
                checked = obfuscateTripTimes,
                onCheckedChange = viewModel::setObfuscateTripTimes
            )

            // ========== Localization ==========
            SettingsCategoryHeader(stringResource(Res.string.pref_category_localization))

            SwitchPreference(
                title = stringResource(Res.string.pref_show_local_and_english),
                description = stringResource(Res.string.pref_show_local_and_english_desc),
                checked = showBothLocalAndEnglish,
                onCheckedChange = viewModel::setShowBothLocalAndEnglish
            )

            SwitchPreference(
                title = stringResource(Res.string.pref_convert_timezone),
                description = stringResource(Res.string.pref_convert_timezone_desc),
                checked = convertTimezone,
                onCheckedChange = viewModel::setConvertTimezone
            )

            SwitchPreference(
                title = stringResource(Res.string.pref_localise_places),
                description = stringResource(Res.string.pref_localise_places_desc),
                checked = localisePlaces,
                onCheckedChange = viewModel::setLocalisePlaces
            )

            // ========== Accessibility ==========
            SwitchPreference(
                title = stringResource(Res.string.pref_speak_balance),
                description = stringResource(Res.string.pref_speak_balance_desc),
                checked = speakBalance,
                onCheckedChange = viewModel::setSpeakBalance
            )

            // ========== Developer Options ==========
            SettingsCategoryHeader(stringResource(Res.string.pref_category_debug))

            SwitchPreference(
                title = stringResource(Res.string.pref_debug_logging),
                description = stringResource(Res.string.pref_debug_logging_desc),
                checked = debugLogging,
                onCheckedChange = viewModel::setDebugLogging
            )

            SwitchPreference(
                title = stringResource(Res.string.pref_debug_spans),
                description = stringResource(Res.string.pref_debug_spans_desc),
                checked = debugSpans,
                onCheckedChange = viewModel::setDebugSpans
            )

            SwitchPreference(
                title = stringResource(Res.string.pref_iso_datetime),
                description = stringResource(Res.string.pref_iso_datetime_desc),
                checked = useIsoDateTimeStamps,
                onCheckedChange = viewModel::setUseIsoDateTimeStamps
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Raw Level selection dialog
    if (showRawLevelDialog) {
        RawLevelDialog(
            currentLevel = rawLevel,
            onDismiss = { showRawLevelDialog = false },
            onSelect = { level ->
                viewModel.setRawLevel(level)
                showRawLevelDialog = false
            }
        )
    }
}

@Composable
private fun SettingsCategoryHeader(
    title: String,
    description: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchPreference(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
    HorizontalDivider()
}

@Composable
private fun ListPreference(
    title: String,
    description: String,
    currentValue: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = currentValue,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    HorizontalDivider()
}

@Composable
private fun RawLevelDialog(
    currentLevel: RawLevel,
    onDismiss: () -> Unit,
    onSelect: (RawLevel) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.pref_raw_level)) },
        text = {
            Column {
                RawLevelOption(
                    title = stringResource(Res.string.pref_raw_level_none),
                    selected = currentLevel == RawLevel.NONE,
                    onClick = { onSelect(RawLevel.NONE) }
                )
                RawLevelOption(
                    title = stringResource(Res.string.pref_raw_level_unknown),
                    selected = currentLevel == RawLevel.UNKNOWN_ONLY,
                    onClick = { onSelect(RawLevel.UNKNOWN_ONLY) }
                )
                RawLevelOption(
                    title = stringResource(Res.string.pref_raw_level_all),
                    selected = currentLevel == RawLevel.ALL,
                    onClick = { onSelect(RawLevel.ALL) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.ok))
            }
        }
    )
}

@Composable
private fun RawLevelOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// Legacy data class for backward compatibility during transition
data class SettingsUiState(
    val launchFromBackground: Boolean = false,
    val showLaunchFromBackground: Boolean = false,
)
