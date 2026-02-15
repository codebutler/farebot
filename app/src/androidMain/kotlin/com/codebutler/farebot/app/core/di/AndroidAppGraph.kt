package com.codebutler.farebot.app.core.di

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.codebutler.farebot.app.core.nfc.NfcStream
import com.codebutler.farebot.app.core.nfc.TagReaderFactory
import com.codebutler.farebot.app.core.platform.AndroidAppPreferences
import com.codebutler.farebot.app.feature.home.AndroidCardScanner
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.DbCardKeysPersister
import com.codebutler.farebot.persist.db.DbCardPersister
import com.codebutler.farebot.persist.db.FareBotDb
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
abstract class AndroidAppGraph : AppGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides context: Context,
        ): AndroidAppGraph
    }

    abstract val nfcStream: NfcStream

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
    fun provideAppPreferences(context: Context): AppPreferences = AndroidAppPreferences(context)

    @Provides
    @SingleIn(AppScope::class)
    fun provideCardSerializer(json: Json): CardSerializer = KotlinxCardSerializer(json)

    @Provides
    @SingleIn(AppScope::class)
    fun provideFareBotDb(context: Context): FareBotDb {
        val driver = AndroidSqliteDriver(FareBotDb.Schema, context, "farebot.db")
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
    fun provideNfcStream(): NfcStream = NfcStream()

    @Provides
    @SingleIn(AppScope::class)
    fun provideTagReaderFactory(): TagReaderFactory = TagReaderFactory()

    @Provides
    @SingleIn(AppScope::class)
    fun provideTransitFactoryRegistry(): TransitFactoryRegistry = createTransitFactoryRegistry()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAnalytics(): Analytics = NoOpAnalytics()

    @Provides
    @SingleIn(AppScope::class)
    fun provideNavDataHolder(): NavDataHolder = NavDataHolder()

    @Provides
    @SingleIn(AppScope::class)
    fun provideCardScanner(
        nfcStream: NfcStream,
        tagReaderFactory: TagReaderFactory,
        cardKeysPersister: CardKeysPersister,
        json: Json,
    ): CardScanner =
        AndroidCardScanner(
            nfcStream = nfcStream,
            tagReaderFactory = tagReaderFactory,
            cardKeysPersister = cardKeysPersister,
            json = json,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun provideCardImporter(
        cardSerializer: CardSerializer,
        json: Json,
    ): CardImporter = CardImporter(cardSerializer, json)

    @Provides
    fun provideNullableCardScanner(scanner: CardScanner): CardScanner? = scanner
}

fun createAndroidGraph(context: Context): AndroidAppGraph {
    val factory = dev.zacsweers.metro.createGraphFactory<AndroidAppGraph.Factory>()
    return factory.create(context)
}
