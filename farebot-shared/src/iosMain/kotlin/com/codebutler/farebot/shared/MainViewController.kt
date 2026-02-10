package com.codebutler.farebot.shared

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.codebutler.farebot.base.util.BundledDatabaseDriverFactory
import com.codebutler.farebot.base.util.DefaultStringResource
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.shared.serialize.FareBotSerializersModule
import com.codebutler.farebot.shared.serialize.KotlinxCardSerializer
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.DbCardKeysPersister
import com.codebutler.farebot.persist.db.DbCardPersister
import com.codebutler.farebot.persist.db.FareBotDb
import com.codebutler.farebot.shared.di.sharedModule
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.nfc.IosNfcScanner
import com.codebutler.farebot.shared.platform.IosPlatformActions
import com.codebutler.farebot.shared.serialize.CardImporter
import com.codebutler.farebot.shared.transit.TransitFactoryRegistry
import com.codebutler.farebot.shared.transit.createTransitFactoryRegistry
import com.codebutler.farebot.shared.ui.screen.ALL_SUPPORTED_CARDS
import com.codebutler.farebot.shared.platform.PlatformActions
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun MainViewController() = ComposeUIViewController {
    val platformActions = remember { IosPlatformActions() }

    FareBotApp(
        platformActions = platformActions,
        supportedCards = ALL_SUPPORTED_CARDS,
        supportedCardTypes = CardType.entries.toSet() - setOf(CardType.MifareClassic, CardType.CEPAS),
    )
}

fun handleImportedFileContent(content: String) {
    org.koin.mp.KoinPlatform.getKoin().get<CardImporter>().submitImport(content)
}

fun initKoin() {
    startKoin {
        modules(sharedModule, iosModule)
    }
}

private val iosModule = module {
    single<StringResource> { DefaultStringResource() }

    single {
        Json {
            serializersModule = FareBotSerializersModule
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    single<CardSerializer> { KotlinxCardSerializer(get()) }

    single {
        val driver = BundledDatabaseDriverFactory().createDriver("farebot.db", FareBotDb.Schema)
        FareBotDb(driver)
    }

    single<CardPersister> { DbCardPersister(get()) }

    single<CardKeysPersister> { DbCardKeysPersister(get()) }

    single<TransitFactoryRegistry> {
        createTransitFactoryRegistry(
            supportedCardTypes = CardType.entries.toSet() - setOf(CardType.MifareClassic),
        )
    }

    single<CardScanner> { IosNfcScanner() }

    single<PlatformActions> { IosPlatformActions() }
}
