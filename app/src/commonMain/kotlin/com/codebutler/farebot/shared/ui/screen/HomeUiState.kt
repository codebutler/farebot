package com.codebutler.farebot.shared.ui.screen

import com.codebutler.farebot.shared.platform.NfcStatus

data class HomeUiState(
    val nfcStatus: NfcStatus = NfcStatus.AVAILABLE,
    val isLoading: Boolean = false,
    val requiresActiveScan: Boolean = true,
)
