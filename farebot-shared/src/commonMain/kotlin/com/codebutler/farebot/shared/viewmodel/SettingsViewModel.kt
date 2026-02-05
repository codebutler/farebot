/*
 * SettingsViewModel.kt
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

package com.codebutler.farebot.shared.viewmodel

import androidx.lifecycle.ViewModel
import com.codebutler.farebot.shared.platform.PlatformActions
import com.codebutler.farebot.shared.settings.AppSettings
import com.codebutler.farebot.shared.settings.RawLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val appSettings: AppSettings,
    private val platformActions: PlatformActions?,
) : ViewModel() {

    // ========== Platform-specific settings ==========

    private val _launchFromBackground = MutableStateFlow(platformActions?.isLaunchFromBackgroundEnabled() ?: false)
    val launchFromBackground: StateFlow<Boolean> = _launchFromBackground.asStateFlow()

    val supportsLaunchFromBackground: Boolean = platformActions?.supportsLaunchFromBackground() ?: false

    fun setLaunchFromBackground(enabled: Boolean) {
        platformActions?.setLaunchFromBackgroundEnabled(enabled)
        _launchFromBackground.value = enabled
    }

    // ========== Raw Data Display ==========

    val rawLevel: StateFlow<RawLevel> = appSettings.rawLevel

    fun setRawLevel(level: RawLevel) {
        appSettings.setRawLevel(level)
    }

    val showRawStationIds: StateFlow<Boolean> = appSettings.showRawStationIds

    fun setShowRawStationIds(enabled: Boolean) {
        appSettings.setShowRawStationIds(enabled)
    }

    // ========== Obfuscation Settings ==========

    val hideCardNumbers: StateFlow<Boolean> = appSettings.hideCardNumbers

    fun setHideCardNumbers(enabled: Boolean) {
        appSettings.setHideCardNumbers(enabled)
    }

    val obfuscateBalance: StateFlow<Boolean> = appSettings.obfuscateBalance

    fun setObfuscateBalance(enabled: Boolean) {
        appSettings.setObfuscateBalance(enabled)
    }

    val obfuscateTripFares: StateFlow<Boolean> = appSettings.obfuscateTripFares

    fun setObfuscateTripFares(enabled: Boolean) {
        appSettings.setObfuscateTripFares(enabled)
    }

    val obfuscateTripDates: StateFlow<Boolean> = appSettings.obfuscateTripDates

    fun setObfuscateTripDates(enabled: Boolean) {
        appSettings.setObfuscateTripDates(enabled)
    }

    val obfuscateTripTimes: StateFlow<Boolean> = appSettings.obfuscateTripTimes

    fun setObfuscateTripTimes(enabled: Boolean) {
        appSettings.setObfuscateTripTimes(enabled)
    }

    // ========== Language & Localization ==========

    val showBothLocalAndEnglish: StateFlow<Boolean> = appSettings.showBothLocalAndEnglish

    fun setShowBothLocalAndEnglish(enabled: Boolean) {
        appSettings.setShowBothLocalAndEnglish(enabled)
    }

    val convertTimezone: StateFlow<Boolean> = appSettings.convertTimezone

    fun setConvertTimezone(enabled: Boolean) {
        appSettings.setConvertTimezone(enabled)
    }

    val localisePlaces: StateFlow<Boolean> = appSettings.localisePlaces

    fun setLocalisePlaces(enabled: Boolean) {
        appSettings.setLocalisePlaces(enabled)
    }

    val languageOverride: StateFlow<String> = appSettings.languageOverride

    fun setLanguageOverride(languageCode: String) {
        appSettings.setLanguageOverride(languageCode)
    }

    // ========== Debug Options ==========

    val debugLogging: StateFlow<Boolean> = appSettings.debugLogging

    fun setDebugLogging(enabled: Boolean) {
        appSettings.setDebugLogging(enabled)
    }

    val debugSpans: StateFlow<Boolean> = appSettings.debugSpans

    fun setDebugSpans(enabled: Boolean) {
        appSettings.setDebugSpans(enabled)
    }

    val useIsoDateTimeStamps: StateFlow<Boolean> = appSettings.useIsoDateTimeStamps

    fun setUseIsoDateTimeStamps(enabled: Boolean) {
        appSettings.setUseIsoDateTimeStamps(enabled)
    }

    // ========== Card Reading Options ==========

    val mfcAuthRetry: StateFlow<Int> = appSettings.mfcAuthRetry

    fun setMfcAuthRetry(count: Int) {
        appSettings.setMfcAuthRetry(count)
    }

    val retrieveLeapKeys: StateFlow<Boolean> = appSettings.retrieveLeapKeys

    fun setRetrieveLeapKeys(enabled: Boolean) {
        appSettings.setRetrieveLeapKeys(enabled)
    }

    // ========== Accessibility ==========

    val speakBalance: StateFlow<Boolean> = appSettings.speakBalance

    fun setSpeakBalance(enabled: Boolean) {
        appSettings.setSpeakBalance(enabled)
    }

    // ========== App Info ==========

    val appVersion: String = appSettings.appVersion
}
