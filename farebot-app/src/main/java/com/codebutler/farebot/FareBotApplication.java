/*
 * FareBotApplication.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot;

import android.app.Application;
import android.os.StrictMode;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.CardUiDependencies;
import com.codebutler.farebot.card.cepas.CEPASTypeAdapterFactory;
import com.codebutler.farebot.card.classic.ClassicTypeAdapterFactory;
import com.codebutler.farebot.card.desfire.DesfireTypeAdapterFactory;
import com.codebutler.farebot.card.felica.FelicaTypeAdapterFactory;
import com.codebutler.farebot.card.serialize.CardSerializer;
import com.codebutler.farebot.card.ultralight.UltralightTypeAdapterFactory;
import com.codebutler.farebot.core.ByteArray;
import com.codebutler.farebot.serialize.gson.EpochDateTypeAdapter;
import com.codebutler.farebot.serialize.CardKeysSerializer;
import com.codebutler.farebot.persist.CardKeysPersister;
import com.codebutler.farebot.persist.CardPersister;
import com.codebutler.farebot.persist.db.DbCardKeysPersister;
import com.codebutler.farebot.persist.db.DbCardPersister;
import com.codebutler.farebot.persist.db.FareBotOpenHelper;
import com.codebutler.farebot.serialize.gson.CardKeysGsonTypeAdapterFactory;
import com.codebutler.farebot.serialize.gson.CardTypeGsonTypeAdapter;
import com.codebutler.farebot.serialize.gson.GsonCardKeysSerializer;
import com.codebutler.farebot.serialize.gson.GsonCardSerializer;
import com.codebutler.farebot.serialize.gson.RawCardGsonTypeAdapterFactory;
import com.codebutler.farebot.util.ExportHelper;
import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;

import io.fabric.sdk.android.Fabric;

public class FareBotApplication extends Application implements CardUiDependencies {

    public static final String PREF_LAST_READ_ID = "last_read_id";
    public static final String PREF_LAST_READ_AT = "last_read_at";

    private CardPersister mCardPersister;
    private CardSerializer mCardSerializer;
    private CardKeysPersister mCardKeysPersister;
    private CardKeysSerializer mCardKeysSerializer;
    private ExportHelper mExportHelper;
    private TagReaderFactory mTagReaderFactory;
    private TransitFactoryRegistry mTransitFactoryRegistry;

    @Override
    public void onCreate() {
        super.onCreate();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new EpochDateTypeAdapter())
                .registerTypeAdapterFactory(CEPASTypeAdapterFactory.create())
                .registerTypeAdapterFactory(ClassicTypeAdapterFactory.create())
                .registerTypeAdapterFactory(DesfireTypeAdapterFactory.create())
                .registerTypeAdapterFactory(FelicaTypeAdapterFactory.create())
                .registerTypeAdapterFactory(UltralightTypeAdapterFactory.create())
                .registerTypeAdapterFactory(new RawCardGsonTypeAdapterFactory())
                .registerTypeAdapterFactory(new CardKeysGsonTypeAdapterFactory())
                .registerTypeAdapter(ByteArray.class, new ByteArray.GsonTypeAdapter())
                .registerTypeAdapter(CardType.class, new CardTypeGsonTypeAdapter())
                .create();

        mCardSerializer = new GsonCardSerializer(gson);
        mCardKeysSerializer = new GsonCardKeysSerializer(gson);

        FareBotOpenHelper openHelper = new FareBotOpenHelper(this);
        mCardPersister = new DbCardPersister(openHelper);
        mCardKeysPersister = new DbCardKeysPersister(openHelper);

        mExportHelper = new ExportHelper(mCardPersister, mCardSerializer, gson);
        mTagReaderFactory = new TagReaderFactory();

        mTransitFactoryRegistry = new TransitFactoryRegistry(this);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
    }

    @NonNull
    public ExportHelper getExportHelper() {
        return mExportHelper;
    }

    @NonNull
    public CardPersister getCardPersister() {
        return mCardPersister;
    }

    @NonNull
    public CardSerializer getCardSerializer() {
        return mCardSerializer;
    }

    @NonNull
    public CardKeysPersister getCardKeysPersister() {
        return mCardKeysPersister;
    }

    @NonNull
    public CardKeysSerializer getCardKeysSerializer() {
        return mCardKeysSerializer;
    }

    @NonNull
    public TagReaderFactory getTagReaderFactory() {
        return mTagReaderFactory;
    }

    @NonNull
    public TransitFactoryRegistry getTransitFactoryRegistry() {
        return mTransitFactoryRegistry;
    }
}
