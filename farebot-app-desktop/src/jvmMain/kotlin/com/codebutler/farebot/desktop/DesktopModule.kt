package com.codebutler.farebot.desktop

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.codebutler.farebot.base.util.DefaultStringResource
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.DbCardKeysPersister
import com.codebutler.farebot.persist.db.DbCardPersister
import com.codebutler.farebot.persist.db.FareBotDb
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.platform.AppPreferences
import com.codebutler.farebot.shared.platform.JvmAppPreferences
import com.codebutler.farebot.shared.serialize.FareBotSerializersModule
import com.codebutler.farebot.shared.serialize.KotlinxCardSerializer
import com.codebutler.farebot.shared.transit.TransitFactoryRegistry
import com.codebutler.farebot.shared.transit.createTransitFactoryRegistry
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import java.io.File
import java.util.Properties

val desktopModule =
    module {
        single {
            Json {
                serializersModule = FareBotSerializersModule
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        }

        single<AppPreferences> { JvmAppPreferences() }

        single<CardSerializer> { KotlinxCardSerializer(get()) }

        single {
            val farebotDir = File(System.getProperty("user.home"), ".farebot").apply { mkdirs() }
            val dbFile = File(farebotDir, "farebot.db")
            val url = "jdbc:sqlite:${dbFile.absolutePath}"
            val driver = JdbcSqliteDriver(url, properties = Properties(), schema = FareBotDb.Schema)
            FareBotDb(driver)
        }

        single<CardPersister> { DbCardPersister(get()) }

        single<CardKeysPersister> { DbCardKeysPersister(get()) }

        single<TransitFactoryRegistry> { createTransitFactoryRegistry() }

        single<StringResource> { DefaultStringResource() }

        single<CardScanner> { DesktopCardScanner() }
    }
