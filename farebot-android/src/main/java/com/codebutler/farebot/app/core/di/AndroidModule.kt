package com.codebutler.farebot.app.core.di

import android.content.SharedPreferences
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.codebutler.farebot.app.core.nfc.NfcStream
import com.codebutler.farebot.app.core.nfc.TagReaderFactory
import com.codebutler.farebot.app.core.serialize.CardKeysSerializer
import com.codebutler.farebot.app.core.serialize.KotlinxCardKeysSerializer
import com.codebutler.farebot.shared.serialize.FareBotSerializersModule
import com.codebutler.farebot.shared.serialize.KotlinxCardSerializer
import com.codebutler.farebot.app.core.transit.createAndroidTransitFactoryRegistry
import com.codebutler.farebot.app.feature.home.AndroidCardScanner
import com.codebutler.farebot.base.util.DefaultStringResource
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.DbCardKeysPersister
import com.codebutler.farebot.persist.db.DbCardPersister
import com.codebutler.farebot.persist.db.FareBotDb
import com.codebutler.farebot.shared.nfc.CardScanner
import com.codebutler.farebot.shared.settings.AppSettings
import com.codebutler.farebot.shared.transit.TransitFactoryRegistry
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<SharedPreferences> { androidContext().getSharedPreferences(androidContext().packageName + "_preferences", android.content.Context.MODE_PRIVATE) }

    single { AppSettings(androidContext()) }

    single {
        Json {
            serializersModule = FareBotSerializersModule
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    single<CardSerializer> { KotlinxCardSerializer(get()) }

    single<CardKeysSerializer> { KotlinxCardKeysSerializer(get()) }

    single {
        val driver = AndroidSqliteDriver(FareBotDb.Schema, androidContext(), "farebot.db")
        FareBotDb(driver)
    }

    single<CardPersister> { DbCardPersister(get()) }

    single<CardKeysPersister> { DbCardKeysPersister(get()) }

    single { NfcStream() }

    single { TagReaderFactory() }

    single<TransitFactoryRegistry> { createAndroidTransitFactoryRegistry(androidContext()) }

    single<StringResource> { DefaultStringResource() }

    single<CardScanner> {
        AndroidCardScanner(
            nfcStream = get(),
            tagReaderFactory = get(),
            cardKeysPersister = get(),
            cardKeysSerializer = get(),
        )
    }
}
