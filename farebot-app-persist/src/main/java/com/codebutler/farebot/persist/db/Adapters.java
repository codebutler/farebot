package com.codebutler.farebot.persist.db;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.CardType;
import com.squareup.sqldelight.ColumnAdapter;

import java.util.Date;

public final class Adapters {

    @NonNull
    public static final ColumnAdapter<CardType, String> CARD_TYPE_ADAPTER = new ColumnAdapter<CardType, String>() {
        @NonNull
        @Override
        public CardType decode(String databaseValue) {
            return CardType.valueOf(databaseValue);
        }

        @Override
        public String encode(@NonNull CardType value) {
            return value.name();
        }
    };

    @NonNull
    public static final ColumnAdapter<Date, Long> DATE_ADAPTER = new ColumnAdapter<Date, Long>() {
        @NonNull
        @Override
        public Date decode(Long databaseValue) {
            return new Date(databaseValue);
        }

        @Override
        public Long encode(@NonNull Date value) {
            return value.getTime();
        }
    };

    private Adapters() { }
}
