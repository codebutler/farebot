/*
 * FareBotApplicationModule.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.app.core.app

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.codebutler.farebot.app.core.nfc.TagReaderFactory
import com.codebutler.farebot.app.core.serialize.CardKeysSerializer
import com.codebutler.farebot.app.core.serialize.gson.ByteArrayGsonTypeAdapter
import com.codebutler.farebot.app.core.serialize.gson.CardKeysGsonTypeAdapterFactory
import com.codebutler.farebot.app.core.serialize.gson.CardTypeGsonTypeAdapter
import com.codebutler.farebot.app.core.serialize.gson.EpochDateTypeAdapter
import com.codebutler.farebot.app.core.serialize.gson.GsonCardKeysSerializer
import com.codebutler.farebot.app.core.serialize.gson.GsonCardSerializer
import com.codebutler.farebot.app.core.serialize.gson.RawCardGsonTypeAdapterFactory
import com.codebutler.farebot.app.core.transit.TransitFactoryRegistry
import com.codebutler.farebot.app.core.util.ExportHelper
import com.codebutler.farebot.base.util.ByteArray
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.cepas.CEPASTypeAdapterFactory
import com.codebutler.farebot.card.classic.ClassicTypeAdapterFactory
import com.codebutler.farebot.card.desfire.DesfireTypeAdapterFactory
import com.codebutler.farebot.card.felica.FelicaTypeAdapterFactory
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.card.ultralight.UltralightTypeAdapterFactory
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.DbCardKeysPersister
import com.codebutler.farebot.persist.db.DbCardPersister
import com.codebutler.farebot.persist.db.FareBotDb
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import java.util.Date

@Module
class FareBotApplicationModule {

    @Provides
    fun provideSharedPreferences(application: FareBotApplication): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(application)

    @Provides
    fun provideGson(): Gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, EpochDateTypeAdapter())
            .registerTypeAdapterFactory(CEPASTypeAdapterFactory.create())
            .registerTypeAdapterFactory(ClassicTypeAdapterFactory.create())
            .registerTypeAdapterFactory(DesfireTypeAdapterFactory.create())
            .registerTypeAdapterFactory(FelicaTypeAdapterFactory.create())
            .registerTypeAdapterFactory(UltralightTypeAdapterFactory.create())
            .registerTypeAdapterFactory(RawCardGsonTypeAdapterFactory())
            .registerTypeAdapterFactory(CardKeysGsonTypeAdapterFactory())
            .registerTypeAdapter(ByteArray::class.java, ByteArrayGsonTypeAdapter())
            .registerTypeAdapter(CardType::class.java, CardTypeGsonTypeAdapter())
            .create()

    @Provides
    fun provideCardSerializer(gson: Gson): CardSerializer = GsonCardSerializer(gson)

    @Provides
    fun provideCardKeysSerializer(gson: Gson): CardKeysSerializer = GsonCardKeysSerializer(gson)

    @Provides
    fun provideFareBotDb(application: FareBotApplication): FareBotDb = FareBotDb.getInstance(application)

    @Provides
    fun provideCardPersister(db: FareBotDb): CardPersister = DbCardPersister(db)

    @Provides
    fun provideCardKeysPersister(db: FareBotDb): CardKeysPersister = DbCardKeysPersister(db)

    @Provides
    fun provideExportHelper(cardPersister: CardPersister, cardSerializer: CardSerializer, gson: Gson): ExportHelper =
            ExportHelper(cardPersister, cardSerializer, gson)

    @Provides
    fun provideTagReaderFactory(): TagReaderFactory {
        return TagReaderFactory()
    }

    @Provides
    fun provideTransitFactoryRegistry(application: FareBotApplication): TransitFactoryRegistry =
            TransitFactoryRegistry(application)
}
