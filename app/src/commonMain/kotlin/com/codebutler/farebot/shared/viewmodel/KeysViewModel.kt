package com.codebutler.farebot.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.shared.ui.screen.KeyItem
import com.codebutler.farebot.shared.ui.screen.KeysUiState
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
class KeysViewModel(
    private val keysPersister: CardKeysPersister,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KeysUiState())
    val uiState: StateFlow<KeysUiState> = _uiState.asStateFlow()

    private val savedKeyMap = mutableMapOf<String, com.codebutler.farebot.persist.db.model.SavedKey>()

    fun loadKeys() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val savedKeys = keysPersister.getSavedKeys()
                val keys =
                    savedKeys.map { savedKey ->
                        val id = "${savedKey.cardId}_${savedKey.cardType}"
                        savedKeyMap[id] = savedKey
                        KeyItem(
                            id = id,
                            cardId = savedKey.cardId,
                            cardType = savedKey.cardType.toString(),
                        )
                    }
                _uiState.value = KeysUiState(keys = keys, isLoading = false)
            } catch (e: Throwable) {
                println("[KeysViewModel] Failed to load keys: $e")
                e.printStackTrace()
                _uiState.value = KeysUiState(isLoading = false)
            }
        }
    }

    fun toggleSelection(keyId: String) {
        val current = _uiState.value
        val newSelected =
            if (current.selectedIds.contains(keyId)) {
                current.selectedIds - keyId
            } else {
                current.selectedIds + keyId
            }
        _uiState.value =
            current.copy(
                selectedIds = newSelected,
                isSelectionMode = newSelected.isNotEmpty(),
            )
    }

    fun clearSelection() {
        _uiState.value =
            _uiState.value.copy(
                selectedIds = emptySet(),
                isSelectionMode = false,
            )
    }

    fun deleteSelected() {
        val selectedIds = _uiState.value.selectedIds.toList()
        viewModelScope.launch {
            for (id in selectedIds) {
                val savedKey = savedKeyMap[id] ?: continue
                keysPersister.delete(savedKey)
                savedKeyMap.remove(id)
            }
            clearSelection()
            loadKeys()
        }
    }

    fun deleteKey(keyId: String) {
        val savedKey = savedKeyMap[keyId] ?: return
        viewModelScope.launch {
            keysPersister.delete(savedKey)
            savedKeyMap.remove(keyId)
            loadKeys()
        }
    }
}
