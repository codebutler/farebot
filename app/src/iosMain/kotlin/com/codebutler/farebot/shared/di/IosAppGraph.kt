package com.codebutler.farebot.shared.di

import com.codebutler.farebot.base.util.BundledDatabaseDriverFactory
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.DbCardKeysPersister
import com.codebutler.farebot.persist.db.DbCardPersister
import com.codebutler.farebot.persist.db.FareBotDb
import com.codebutler.farebot.shared.core.NavDataHolder
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.nfc.IosNfcScanner
import com.codebutler.farebot.shared.platform.Analytics
import com.codebutler.farebot.shared.platform.AppPreferences
import com.codebutler.farebot.shared.platform.IosAppPreferences
import com.codebutler.farebot.shared.platform.IosPlatformActions
import com.codebutler.farebot.shared.platform.NoOpAnalytics
import com.codebutler.farebot.shared.platform.PlatformActions
import com.codebutler.farebot.flipper.FlipperTransportFactory
import com.codebutler.farebot.flipper.IosFlipperTransportFactory
import com.codebutler.farebot.shared.serialize.CardImporter
import com.codebutler.farebot.shared.serialize.FareBotSerializersModule
import com.codebutler.farebot.shared.serialize.KotlinxCardSerializer
import com.codebutler.farebot.shared.transit.TransitFactoryRegistry
import com.codebutler.farebot.shared.transit.createTransitFactoryRegistry
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json

@DependencyGraph(AppScope::class)
abstract class IosAppGraph : AppGraph {
    abstract val platformActions: PlatformActions

    @Provides
    @SingleIn(AppScope::class)
    fun provideJson(): Json =
        Json {
            serializersModule = FareBotSerializersModule
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Provides
    @SingleIn(AppScope::class)
    fun provideAppPreferences(): AppPreferences = IosAppPreferences()

    @Provides
    @SingleIn(AppScope::class)
    fun provideCardSerializer(json: Json): CardSerializer = KotlinxCardSerializer(json)

    @Provides
    @SingleIn(AppScope::class)
    fun provideFareBotDb(): FareBotDb {
        val driver = BundledDatabaseDriverFactory().createDriver("farebot.db", FareBotDb.Schema)
        return FareBotDb(driver)
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideCardPersister(db: FareBotDb): CardPersister = DbCardPersister(db)

    @Provides
    @SingleIn(AppScope::class)
    fun provideCardKeysPersister(db: FareBotDb): CardKeysPersister = DbCardKeysPersister(db)

    @Provides
    @SingleIn(AppScope::class)
    fun provideTransitFactoryRegistry(): TransitFactoryRegistry = createTransitFactoryRegistry()

    @Provides
    @SingleIn(AppScope::class)
    fun provideCardScanner(): CardScanner = IosNfcScanner()

    @Provides
    @SingleIn(AppScope::class)
    fun providePlatformActions(): PlatformActions = IosPlatformActions()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAnalytics(): Analytics = NoOpAnalytics()

    @Provides
    @SingleIn(AppScope::class)
    fun provideNavDataHolder(): NavDataHolder = NavDataHolder()

    @Provides
    @SingleIn(AppScope::class)
    fun provideCardImporter(
        cardSerializer: CardSerializer,
        json: Json,
    ): CardImporter = CardImporter(cardSerializer, json)

    @Provides
    @SingleIn(AppScope::class)
    fun provideFlipperTransportFactory(): FlipperTransportFactory = IosFlipperTransportFactory()

    @Provides
    fun provideNullableCardScanner(scanner: CardScanner): CardScanner? = scanner
}
