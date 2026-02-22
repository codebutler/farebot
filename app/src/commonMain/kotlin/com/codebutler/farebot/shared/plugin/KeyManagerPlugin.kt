/*
 * KeyManagerPlugin.kt
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

package com.codebutler.farebot.shared.plugin

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicKeyRecovery
import com.codebutler.farebot.card.classic.key.ClassicCardKeys
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.shared.nfc.CardScanner
import org.jetbrains.compose.resources.StringResource

/**
 * Plugin interface for key management functionality.
 *
 * Implementations live in `:app-keymanager`, which is excluded from iOS builds.
 * On iOS, [AppGraph.keyManagerPlugin] returns null and all key-related
 * UI elements are hidden.
 */
interface KeyManagerPlugin {
    /** Register key-related navigation routes (Keys, AddKey). */
    fun NavGraphBuilder.registerKeyRoutes(
        navController: NavHostController,
        cardKeysPersister: CardKeysPersister,
        cardScanner: CardScanner?,
        onPickFile: ((ByteArray?) -> Unit) -> Unit,
    )

    /** [ClassicKeyRecovery] for use by scanner backends. */
    val classicKeyRecovery: ClassicKeyRecovery

    /** Get saved keys for a specific tag ID. */
    fun getCardKeysForTag(tagId: String): ClassicCardKeys?

    /** Get all global dictionary keys. */
    fun getGlobalKeys(): List<ByteArray>

    /** Navigate to the Keys list screen. */
    fun navigateToKeys(navController: NavHostController)

    /** Navigate to the Add Key screen. */
    fun navigateToAddKey(
        navController: NavHostController,
        tagId: String? = null,
        cardType: CardType? = null,
    )

    // String resources needed by app code (resolved at call site)
    val lockedCardTitle: StringResource
    val keysRequiredMessage: StringResource
    val addKeyLabel: StringResource
    val keysLabel: StringResource
    val keysLoadedLabel: StringResource
}
