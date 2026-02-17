package com.codebutler.farebot.web

import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.flipper.FlipperTransportFactory
import com.codebutler.farebot.flipper.WebFlipperTransportFactory
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.shared.core.NavDataHolder
import com.codebutler.farebot.shared.di.AppGraph
import com.codebutler.farebot.shared.di.AppScope
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.platform.Analytics
import com.codebutler.farebot.shared.platform.AppPreferences
import com.codebutler.farebot.shared.platform.NoOpAnalytics
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
abstract class WebAppGraph : AppGraph {
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
    fun provideAppPreferences(): AppPreferences = WebAppPreferences()

    @Provides
    @SingleIn(AppScope::class)
    fun provideCardSerializer(json: Json): CardSerializer = KotlinxCardSerializer(json)

    @Provides
    @SingleIn(AppScope::class)
    fun provideCardPersister(json: Json): CardPersister = LocalStorageCardPersister(json)

    @Provides
    @SingleIn(AppScope::class)
    fun provideCardKeysPersister(json: Json): CardKeysPersister = LocalStorageCardKeysPersister(json)

    @Provides
    @SingleIn(AppScope::class)
    fun provideTransitFactoryRegistry(): TransitFactoryRegistry = createTransitFactoryRegistry()

    @Provides
    @SingleIn(AppScope::class)
    fun provideCardScanner(): CardScanner = WebCardScanner()

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
    fun provideFlipperTransportFactory(): FlipperTransportFactory = WebFlipperTransportFactory()

    @Provides
    fun provideNullableCardScanner(scanner: CardScanner): CardScanner? = scanner
}
