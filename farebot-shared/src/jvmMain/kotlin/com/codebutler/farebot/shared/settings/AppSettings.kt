package com.codebutler.farebot.shared.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.codebutler.farebot.shared.settings.AppSettingsKeys.DEFAULT_MFC_AUTH_RETRY
import com.codebutler.farebot.shared.settings.AppSettingsKeys.DEFAULT_MFC_FALLBACK

actual class AppSettings {

    // ========== Raw Data Display ==========

    private val _rawLevel = MutableStateFlow(RawLevel.NONE)
    actual val rawLevel: StateFlow<RawLevel> = _rawLevel.asStateFlow()

    actual fun setRawLevel(level: RawLevel) {
        _rawLevel.value = level
    }

    private val _showRawStationIds = MutableStateFlow(false)
    actual val showRawStationIds: StateFlow<Boolean> = _showRawStationIds.asStateFlow()

    actual fun setShowRawStationIds(enabled: Boolean) {
        _showRawStationIds.value = enabled
    }

    // ========== Obfuscation Settings ==========

    private val _hideCardNumbers = MutableStateFlow(false)
    actual val hideCardNumbers: StateFlow<Boolean> = _hideCardNumbers.asStateFlow()

    actual fun setHideCardNumbers(enabled: Boolean) {
        _hideCardNumbers.value = enabled
    }

    private val _obfuscateBalance = MutableStateFlow(false)
    actual val obfuscateBalance: StateFlow<Boolean> = _obfuscateBalance.asStateFlow()

    actual fun setObfuscateBalance(enabled: Boolean) {
        _obfuscateBalance.value = enabled
    }

    private val _obfuscateTripFares = MutableStateFlow(false)
    actual val obfuscateTripFares: StateFlow<Boolean> = _obfuscateTripFares.asStateFlow()

    actual fun setObfuscateTripFares(enabled: Boolean) {
        _obfuscateTripFares.value = enabled
    }

    private val _obfuscateTripDates = MutableStateFlow(false)
    actual val obfuscateTripDates: StateFlow<Boolean> = _obfuscateTripDates.asStateFlow()

    actual fun setObfuscateTripDates(enabled: Boolean) {
        _obfuscateTripDates.value = enabled
    }

    private val _obfuscateTripTimes = MutableStateFlow(false)
    actual val obfuscateTripTimes: StateFlow<Boolean> = _obfuscateTripTimes.asStateFlow()

    actual fun setObfuscateTripTimes(enabled: Boolean) {
        _obfuscateTripTimes.value = enabled
    }

    // ========== Language & Localization ==========

    private val _showBothLocalAndEnglish = MutableStateFlow(false)
    actual val showBothLocalAndEnglish: StateFlow<Boolean> = _showBothLocalAndEnglish.asStateFlow()

    actual fun setShowBothLocalAndEnglish(enabled: Boolean) {
        _showBothLocalAndEnglish.value = enabled
    }

    private val _convertTimezone = MutableStateFlow(false)
    actual val convertTimezone: StateFlow<Boolean> = _convertTimezone.asStateFlow()

    actual fun setConvertTimezone(enabled: Boolean) {
        _convertTimezone.value = enabled
    }

    private val _localisePlaces = MutableStateFlow(false)
    actual val localisePlaces: StateFlow<Boolean> = _localisePlaces.asStateFlow()

    actual fun setLocalisePlaces(enabled: Boolean) {
        _localisePlaces.value = enabled
    }

    private val _languageOverride = MutableStateFlow("")
    actual val languageOverride: StateFlow<String> = _languageOverride.asStateFlow()

    actual fun setLanguageOverride(languageCode: String) {
        _languageOverride.value = languageCode
    }

    // ========== Debug Options ==========

    private val _debugLogging = MutableStateFlow(false)
    actual val debugLogging: StateFlow<Boolean> = _debugLogging.asStateFlow()

    actual fun setDebugLogging(enabled: Boolean) {
        _debugLogging.value = enabled
    }

    private val _debugSpans = MutableStateFlow(false)
    actual val debugSpans: StateFlow<Boolean> = _debugSpans.asStateFlow()

    actual fun setDebugSpans(enabled: Boolean) {
        _debugSpans.value = enabled
    }

    private val _useIsoDateTimeStamps = MutableStateFlow(false)
    actual val useIsoDateTimeStamps: StateFlow<Boolean> = _useIsoDateTimeStamps.asStateFlow()

    actual fun setUseIsoDateTimeStamps(enabled: Boolean) {
        _useIsoDateTimeStamps.value = enabled
    }

    // ========== Card Reading Options ==========

    private val _mfcAuthRetry = MutableStateFlow(DEFAULT_MFC_AUTH_RETRY)
    actual val mfcAuthRetry: StateFlow<Int> = _mfcAuthRetry.asStateFlow()

    actual fun setMfcAuthRetry(count: Int) {
        _mfcAuthRetry.value = count
    }

    private val _mfcFallbackReader = MutableStateFlow(DEFAULT_MFC_FALLBACK)
    actual val mfcFallbackReader: StateFlow<String> = _mfcFallbackReader.asStateFlow()

    actual fun setMfcFallbackReader(reader: String) {
        _mfcFallbackReader.value = reader.lowercase()
    }

    private val _retrieveLeapKeys = MutableStateFlow(false)
    actual val retrieveLeapKeys: StateFlow<Boolean> = _retrieveLeapKeys.asStateFlow()

    actual fun setRetrieveLeapKeys(enabled: Boolean) {
        _retrieveLeapKeys.value = enabled
    }

    // ========== Accessibility ==========

    private val _speakBalance = MutableStateFlow(false)
    actual val speakBalance: StateFlow<Boolean> = _speakBalance.asStateFlow()

    actual fun setSpeakBalance(enabled: Boolean) {
        _speakBalance.value = enabled
    }

    // ========== Convenience Accessors ==========

    actual val language: String
        get() {
            val override = _languageOverride.value
            if (override.isNotEmpty()) return override
            return java.util.Locale.getDefault().language
        }

    actual val region: String?
        get() = java.util.Locale.getDefault().country.takeIf { it.isNotEmpty() }

    actual val appVersion: String
        get() = "dev"
}
