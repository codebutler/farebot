package com.codebutler.farebot.shared.plugin

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.codebutler.farebot.app.keymanager.KeyManagerPluginImpl
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.ClassicKeyRecovery
import com.codebutler.farebot.card.classic.key.ClassicCardKeys
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.shared.nfc.CardScanner
import org.jetbrains.compose.resources.StringResource

fun KeyManagerPluginImpl.toKeyManagerPlugin(): KeyManagerPlugin {
    val impl = this
    return object : KeyManagerPlugin {
        override val classicKeyRecovery: ClassicKeyRecovery get() = impl.classicKeyRecovery

        override fun getCardKeysForTag(tagId: String): ClassicCardKeys? = impl.getCardKeysForTag(tagId)

        override fun getGlobalKeys(): List<ByteArray> = impl.getGlobalKeys()

        override fun navigateToKeys(navController: NavHostController) = impl.navigateToKeys(navController)

        override fun navigateToAddKey(
            navController: NavHostController,
            tagId: String?,
            cardType: CardType?,
        ) = impl.navigateToAddKey(navController, tagId, cardType)

        override fun NavGraphBuilder.registerKeyRoutes(
            navController: NavHostController,
            cardKeysPersister: CardKeysPersister,
            cardScanner: CardScanner?,
            onPickFile: ((ByteArray?) -> Unit) -> Unit,
        ) = with(impl) {
            registerKeyRoutes(navController, cardKeysPersister, cardScanner, onPickFile)
        }

        override val lockedCardTitle: StringResource get() = impl.lockedCardTitle
        override val keysRequiredMessage: StringResource get() = impl.keysRequiredMessage
        override val addKeyLabel: StringResource get() = impl.addKeyLabel
        override val keysLabel: StringResource get() = impl.keysLabel
        override val keysLoadedLabel: StringResource get() = impl.keysLoadedLabel
    }
}
