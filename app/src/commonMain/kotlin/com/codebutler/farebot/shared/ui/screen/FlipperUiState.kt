package com.codebutler.farebot.shared.ui.screen

data class FlipperUiState(
    val connectionState: FlipperConnectionState = FlipperConnectionState.Disconnected,
    val deviceInfo: Map<String, String> = emptyMap(),
    val currentPath: String = "/ext/nfc",
    val files: List<FlipperFileItem> = emptyList(),
    val isLoading: Boolean = false,
    val selectedFiles: Set<String> = emptySet(),
    val error: String? = null,
    val importProgress: ImportProgress? = null,
)

enum class FlipperConnectionState {
    Disconnected,
    Connecting,
    Connected,
}

data class FlipperFileItem(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val path: String,
)

data class ImportProgress(
    val currentFile: String,
    val currentIndex: Int,
    val totalFiles: Int,
)
