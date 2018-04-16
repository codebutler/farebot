package com.codebutler.farebot.persist.db.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.transit.registry.annotations.CardType;
import com.codebutler.farebot.persist.db.Adapters;
import com.google.auto.value.AutoValue;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;

import java.util.Date;

@AutoValue
public abstract class SavedCard implements SavedCardModel {

    @NonNull
    public static final Factory<SavedCard> FACTORY = new Factory<>(new Creator<SavedCard>() {
        @Override
        public SavedCard create(
                @Nullable Long id,
                @NonNull CardType type,
                @NonNull String serial,
                @NonNull String data,
                @Nullable Date scanned_at) {
            return new AutoValue_SavedCard(id, type, serial, data, scanned_at);
        }
    }, Adapters.CARD_TYPE_ADAPTER, Adapters.DATE_ADAPTER);

    @NonNull
    public static final SqlDelightStatement SELECT_ALL = FACTORY.select_all();

    @NonNull
    public static final RowMapper<SavedCard> SELECT_ALL_MAPPER = FACTORY.select_allMapper();

    @NonNull
    public static SavedCard create(
            @NonNull CardType type,
            @NonNull String serial,
            @NonNull String data) {
        return new AutoValue_SavedCard(null, type, serial, data, new Date());
    }
}
