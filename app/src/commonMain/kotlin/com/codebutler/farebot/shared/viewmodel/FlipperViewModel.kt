package com.codebutler.farebot.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.flipper.FlipperRpcClient
import com.codebutler.farebot.flipper.FlipperKeyDictParser
import com.codebutler.farebot.flipper.FlipperTransport
import com.codebutler.farebot.flipper.FlipperTransportFactory
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.model.SavedCard
import com.codebutler.farebot.shared.serialize.CardImporter
import com.codebutler.farebot.shared.serialize.ImportResult
import com.codebutler.farebot.shared.ui.screen.FlipperConnectionState
import com.codebutler.farebot.shared.ui.screen.FlipperFileItem
import com.codebutler.farebot.shared.ui.screen.FlipperUiState
import com.codebutler.farebot.shared.ui.screen.ImportProgress
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
class FlipperViewModel(
    private val cardImporter: CardImporter,
    private val cardPersister: CardPersister,
    private val cardKeysPersister: CardKeysPersister,
    private val cardSerializer: CardSerializer,
    private val transportFactory: FlipperTransportFactory,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FlipperUiState())
    val uiState: StateFlow<FlipperUiState> = _uiState.asStateFlow()

    private var rpcClient: FlipperRpcClient? = null
    private var transport: FlipperTransport? = null

    fun connectUsb() {
        viewModelScope.launch {
            val transport = transportFactory.createUsbTransport()
            if (transport != null) {
                connect(transport)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "USB transport not available on this platform",
                )
            }
        }
    }

    fun connectBle() {
        viewModelScope.launch {
            val transport = transportFactory.createBleTransport()
            if (transport != null) {
                connect(transport)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Bluetooth transport not available on this platform",
                )
            }
        }
    }

    fun connect(transport: FlipperTransport) {
        this.transport = transport
        val client = FlipperRpcClient(transport)
        this.rpcClient = client

        _uiState.value = _uiState.value.copy(
            connectionState = FlipperConnectionState.Connecting,
            error = null,
        )

        viewModelScope.launch {
            try {
                client.connect()

                val deviceInfo = mutableMapOf<String, String>()
                try {
                    val info = client.getDeviceInfo()
                    deviceInfo.putAll(info)
                } catch (e: Exception) {
                    println("[FlipperViewModel] Failed to get device info: ${e.message}")
                }

                _uiState.value = _uiState.value.copy(
                    connectionState = FlipperConnectionState.Connected,
                    deviceInfo = deviceInfo,
                )

                navigateToDirectory("/ext/nfc")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    connectionState = FlipperConnectionState.Disconnected,
                    error = "Connection failed: ${e.message}",
                )
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                transport?.close()
            } catch (e: Exception) {
                println("[FlipperViewModel] Error closing transport: ${e.message}")
            }
            rpcClient = null
            transport = null
            _uiState.value = FlipperUiState()
        }
    }

    fun navigateToDirectory(path: String) {
        val client = rpcClient ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val entries = client.listDirectory(path)
                val files = entries.map { entry ->
                    FlipperFileItem(
                        name = entry.name,
                        isDirectory = entry.isDirectory,
                        size = entry.size,
                        path = "$path/${entry.name}",
                    )
                }.sortedWith(compareByDescending<FlipperFileItem> { it.isDirectory }.thenBy { it.name })

                _uiState.value = _uiState.value.copy(
                    currentPath = path,
                    files = files,
                    isLoading = false,
                    selectedFiles = emptySet(),
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to list directory: ${e.message}",
                )
            }
        }
    }

    fun navigateUp() {
        val current = _uiState.value.currentPath
        val parent = current.substringBeforeLast('/', "/ext")
        if (parent.isNotEmpty() && parent != current) {
            navigateToDirectory(parent)
        }
    }

    fun toggleFileSelection(path: String) {
        val current = _uiState.value.selectedFiles
        val newSelected = if (current.contains(path)) {
            current - path
        } else {
            current + path
        }
        _uiState.value = _uiState.value.copy(selectedFiles = newSelected)
    }

    fun importSelectedFiles() {
        val client = rpcClient ?: return
        val selectedPaths = _uiState.value.selectedFiles.toList()
        if (selectedPaths.isEmpty()) return

        viewModelScope.launch {
            for ((index, path) in selectedPaths.withIndex()) {
                val fileName = path.substringAfterLast('/')
                _uiState.value = _uiState.value.copy(
                    importProgress = ImportProgress(
                        currentFile = fileName,
                        currentIndex = index + 1,
                        totalFiles = selectedPaths.size,
                    ),
                )

                try {
                    val fileData = client.readFile(path)
                    val content = fileData.decodeToString()
                    val result = cardImporter.importCards(content)

                    if (result is ImportResult.Success) {
                        for (rawCard in result.cards) {
                            cardPersister.insertCard(
                                SavedCard(
                                    type = rawCard.cardType(),
                                    serial = rawCard.tagId().hex(),
                                    data = cardSerializer.serialize(rawCard),
                                ),
                            )
                        }
                        if (result.classicKeys != null) {
                            val keys = result.classicKeys.keys.flatMap { sectorKey ->
                                listOfNotNull(
                                    sectorKey.keyA.takeIf { it.any { b -> b != 0.toByte() } },
                                    sectorKey.keyB.takeIf { it.any { b -> b != 0.toByte() } },
                                )
                            }
                            if (keys.isNotEmpty()) {
                                cardKeysPersister.insertGlobalKeys(keys, "flipper_nfc_dump")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("[FlipperViewModel] Failed to import $path: ${e.message}")
                }
            }

            _uiState.value = _uiState.value.copy(
                importProgress = null,
                selectedFiles = emptySet(),
            )
        }
    }

    fun importKeyDictionary() {
        val client = rpcClient ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                importProgress = ImportProgress(
                    currentFile = "mf_classic_dict_user.nfc",
                    currentIndex = 1,
                    totalFiles = 1,
                ),
            )

            try {
                val dictPath = "/ext/nfc/assets/mf_classic_dict_user.nfc"
                val data = client.readFile(dictPath)
                val content = data.decodeToString()
                val keys = FlipperKeyDictParser.parse(content)

                if (keys.isNotEmpty()) {
                    cardKeysPersister.insertGlobalKeys(keys, "flipper_user_dict")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to import key dictionary: ${e.message}",
                )
            }

            _uiState.value = _uiState.value.copy(importProgress = null)
        }
    }
}
