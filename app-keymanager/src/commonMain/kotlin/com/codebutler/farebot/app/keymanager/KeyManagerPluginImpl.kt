/*
 * KeyManagerPluginImpl.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2026 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.app.keymanager

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.codebutler.farebot.app.keymanager.ui.AddKeyScreen
import com.codebutler.farebot.app.keymanager.ui.KeysScreen
import com.codebutler.farebot.app.keymanager.viewmodel.AddKeyViewModel
import com.codebutler.farebot.app.keymanager.viewmodel.KeysViewModel
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicKeyRecovery
import com.codebutler.farebot.card.classic.key.ClassicCardKeys
import com.codebutler.farebot.keymanager.NestedAttackKeyRecovery
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.shared.nfc.CardScanner
import farebot.app_keymanager.generated.resources.Res
import farebot.app_keymanager.generated.resources.add_key
import farebot.app_keymanager.generated.resources.keys
import farebot.app_keymanager.generated.resources.keys_loaded
import farebot.app_keymanager.generated.resources.keys_required
import farebot.app_keymanager.generated.resources.locked_card
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.StringResource

/**
 * Provides all key management functionality.
 *
 * This class does NOT implement [com.codebutler.farebot.shared.plugin.KeyManagerPlugin]
 * directly to avoid a circular dependency (`:app-keymanager` cannot depend on `:app`).
 * Platform AppGraphs wrap this as a [KeyManagerPlugin] adapter.
 */
class KeyManagerPluginImpl(
    private val cardKeysPersister: CardKeysPersister,
    private val json: Json,
) {
    val classicKeyRecovery: ClassicKeyRecovery = NestedAttackKeyRecovery()

    fun navigateToKeys(navController: NavHostController) {
        navController.navigate(KEYS_ROUTE)
    }

    fun navigateToAddKey(
        navController: NavHostController,
        tagId: String? = null,
        cardType: CardType? = null,
    ) {
        navController.navigate(buildAddKeyRoute(tagId, cardType))
    }

    fun NavGraphBuilder.registerKeyRoutes(
        navController: NavHostController,
        cardKeysPersister: CardKeysPersister,
        cardScanner: CardScanner?,
        onPickFile: ((ByteArray?) -> Unit) -> Unit,
    ) {
        composable(KEYS_ROUTE) {
            val keysViewModel = viewModel { KeysViewModel(cardKeysPersister) }
            val uiState by keysViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                keysViewModel.loadKeys()
            }

            KeysScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onNavigateToAddKey = { navController.navigate(buildAddKeyRoute()) },
                onDeleteKey = { keyId -> keysViewModel.deleteKey(keyId) },
                onToggleSelection = { keyId -> keysViewModel.toggleSelection(keyId) },
                onClearSelection = { keysViewModel.clearSelection() },
                onSelectAll = { keysViewModel.selectAll() },
                onDeleteSelected = { keysViewModel.deleteSelected() },
            )
        }

        composable(
            route = ADD_KEY_ROUTE,
            arguments =
                listOf(
                    navArgument("tagId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("cardType") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) { backStackEntry ->
            val addKeyViewModel = viewModel { AddKeyViewModel(cardKeysPersister, cardScanner) }
            val uiState by addKeyViewModel.uiState.collectAsState()

            val prefillTagId = backStackEntry.arguments?.read { getStringOrNull("tagId") }
            val prefillCardTypeName = backStackEntry.arguments?.read { getStringOrNull("cardType") }

            LaunchedEffect(prefillTagId, prefillCardTypeName) {
                if (prefillTagId != null && prefillCardTypeName != null) {
                    val ct = CardType.entries.firstOrNull { it.name == prefillCardTypeName }
                    if (ct != null) {
                        addKeyViewModel.prefillCardData(prefillTagId, ct)
                    }
                }
            }

            LaunchedEffect(Unit) {
                addKeyViewModel.startObservingTags()
            }

            LaunchedEffect(Unit) {
                addKeyViewModel.keySaved.collect {
                    navController.popBackStack()
                }
            }

            AddKeyScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onSaveKey = { cardId, ct, keyData ->
                    addKeyViewModel.saveKey(cardId, ct, keyData)
                },
                onEnterManually = { addKeyViewModel.enterManualMode() },
                onImportFile = {
                    onPickFile { bytes ->
                        if (bytes != null) {
                            addKeyViewModel.importKeyFile(bytes)
                        }
                    }
                },
            )
        }
    }

    fun getCardKeysForTag(tagId: String): ClassicCardKeys? {
        val savedKey = cardKeysPersister.getForTagId(tagId) ?: return null
        return when (savedKey.cardType) {
            CardType.MifareClassic -> json.decodeFromString(ClassicCardKeys.serializer(), savedKey.keyData)
            else -> null
        }
    }

    fun getGlobalKeys(): List<ByteArray> =
        try {
            cardKeysPersister.getGlobalKeys()
        } catch (e: Exception) {
            println("[KeyManager] Failed to load global keys: ${e.message}")
            emptyList()
        }

    val lockedCardTitle: StringResource get() = Res.string.locked_card
    val keysRequiredMessage: StringResource get() = Res.string.keys_required
    val addKeyLabel: StringResource get() = Res.string.add_key
    val keysLabel: StringResource get() = Res.string.keys
    val keysLoadedLabel: StringResource get() = Res.string.keys_loaded

    companion object {
        private const val KEYS_ROUTE = "keys"
        private const val ADD_KEY_ROUTE = "add_key?tagId={tagId}&cardType={cardType}"

        private fun buildAddKeyRoute(
            tagId: String? = null,
            cardType: CardType? = null,
        ): String =
            buildString {
                append("add_key")
                val params = mutableListOf<String>()
                if (tagId != null) params.add("tagId=$tagId")
                if (cardType != null) params.add("cardType=${cardType.name}")
                if (params.isNotEmpty()) append("?${params.joinToString("&")}")
            }
    }
}
