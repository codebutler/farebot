package com.codebutler.farebot.desktop

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
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
import com.codebutler.farebot.shared.platform.JvmAppPreferences
import com.codebutler.farebot.shared.platform.NoOpAnalytics
import com.codebutler.farebot.flipper.FlipperTransportFactory
import com.codebutler.farebot.flipper.JvmFlipperTransportFactory
import com.codebutler.farebot.shared.serialize.CardImporter
import com.codebutler.farebot.shared.serialize.FareBotSerializersModule
import com.codebutler.farebot.shared.serialize.KotlinxCardSerializer
import com.codebutler.farebot.shared.transit.TransitFactoryRegistry
import com.codebutler.farebot.shared.transit.createTransitFactoryRegistry
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Properties

@DependencyGraph(AppScope::class)
abstract class DesktopAppGraph : AppGraph {
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
    fun provideAppPreferences(): AppPreferences = JvmAppPreferences()

    @Provides
    @SingleIn(AppScope::class)
    fun provideCardSerializer(json: Json): CardSerializer = KotlinxCardSerializer(json)

    @Provides
    @SingleIn(AppScope::class)
    fun provideFareBotDb(): FareBotDb {
        val farebotDir = File(System.getProperty("user.home"), ".farebot").apply { mkdirs() }
        val dbFile = File(farebotDir, "farebot.db")
        val url = "jdbc:sqlite:${dbFile.absolutePath}"
        val driver = JdbcSqliteDriver(url, properties = Properties(), schema = FareBotDb.Schema)
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
    fun provideCardScanner(): CardScanner = DesktopCardScanner()

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
    fun provideFlipperTransportFactory(): FlipperTransportFactory = JvmFlipperTransportFactory()

    @Provides
    fun provideNullableCardScanner(scanner: CardScanner): CardScanner? = scanner
}
