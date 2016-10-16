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
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.TagReaderFactory;
import com.codebutler.farebot.card.felica.FelicaDBUtil;
import com.codebutler.farebot.persist.CardPersister;
import com.codebutler.farebot.serialize.CardJsonSerializer;
import com.codebutler.farebot.serialize.CardSerializer;
import com.codebutler.farebot.serialize.EpochDateTypeAdapter;
import com.codebutler.farebot.serialize.FareBotTypeAdapterFactory;
import com.codebutler.farebot.transit.TransitFactoryRegistry;
import com.codebutler.farebot.transit.ovc.OVChipDBUtil;
import com.codebutler.farebot.transit.seq_go.SeqGoDBUtil;
import com.codebutler.farebot.util.ExportHelper;
import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;

import io.fabric.sdk.android.Fabric;

public class FareBotApplication extends Application {

    public static final String PREF_LAST_READ_ID = "last_read_id";
    public static final String PREF_LAST_READ_AT = "last_read_at";

    private static FareBotApplication sInstance;

    private FelicaDBUtil mFelicaDBUtil;
    private OVChipDBUtil mOVChipDBUtil;
    private SeqGoDBUtil mSeqGoDBUtil;
    private CardJsonSerializer mCardJsonSerializer;
    private boolean mMifareClassicSupport;
    private ExportHelper mExportHelper;
    private CardPersister mCardPersister;
    private TagReaderFactory mTagReaderFactory;
    private TransitFactoryRegistry mTransitFactoryRegistry;

    public FareBotApplication() {
        sInstance = this;
    }

    @Deprecated
    public static FareBotApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mFelicaDBUtil = new FelicaDBUtil(this);
        mOVChipDBUtil = new OVChipDBUtil(this);
        mSeqGoDBUtil = new SeqGoDBUtil(this);

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new EpochDateTypeAdapter())
                .registerTypeAdapterFactory(FareBotTypeAdapterFactory.create())
                .registerTypeAdapterFactory(new RawCard.GsonTypeAdapterFactory())
                .registerTypeAdapter(ByteArray.class, new ByteArray.GsonTypeAdapter())
                .registerTypeAdapter(CardType.class, new CardType.GsonTypeAdapter())
                .create();

        mCardJsonSerializer = new CardJsonSerializer(gson);

        // Check for Mifare Classic support
        mMifareClassicSupport = this.getPackageManager().hasSystemFeature("com.nxp.mifare");

        mCardPersister = new CardPersister(this, mCardJsonSerializer);
        mExportHelper = new ExportHelper(this, mCardPersister, gson);
        mTagReaderFactory = new TagReaderFactory();

        mTransitFactoryRegistry = new TransitFactoryRegistry();

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
    public FelicaDBUtil getFelicaDBUtil() {
        return mFelicaDBUtil;
    }

    @NonNull
    public OVChipDBUtil getOVChipDBUtil() {
        return mOVChipDBUtil;
    }

    @NonNull
    public SeqGoDBUtil getSeqGoDBUtil() {
        return mSeqGoDBUtil;
    }

    @NonNull
    public CardSerializer getCardSerializer() {
        return mCardJsonSerializer;
    }

    @NonNull
    public CardPersister getCardPersister() {
        return mCardPersister;
    }

    @NonNull
    public TagReaderFactory getTagReaderFactory() {
        return mTagReaderFactory;
    }

    public TransitFactoryRegistry getTransitFactoryRegistry() {
        return mTransitFactoryRegistry;
    }

    public boolean getMifareClassicSupport() {
        return mMifareClassicSupport;
    }
}
