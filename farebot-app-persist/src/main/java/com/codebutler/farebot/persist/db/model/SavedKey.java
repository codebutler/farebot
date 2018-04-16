package com.codebutler.farebot.persist.db.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.persist.db.Adapters;
import com.google.auto.value.AutoValue;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.util.Date;

@AutoValue
public abstract class SavedKey implements SavedKeyModel {

    @NonNull
    public static final Factory<SavedKey> FACTORY = new Factory<>(new Creator<SavedKey>() {
        @Override
        public SavedKey create(
                @Nullable Long id,
                @NonNull String card_id,
                @NonNull CardType card_type,
                @NonNull String key_data,
                Date created_at) {
            return new AutoValue_SavedKey(id, card_id, card_type, key_data, created_at);
        }
    }, Adapters.CARD_TYPE_ADAPTER, Adapters.DATE_ADAPTER);

    @NonNull
    public static final RowMapper<SavedKey> SELECT_ALL_MAPPER = FACTORY.select_allMapper();

    @NonNull
    public static final SqlDelightStatement SELECT_ALL = FACTORY.select_all();

    @NonNull
    public static SavedKey create(
            @NonNull String card_id,
            @NonNull CardType card_type,
            @NonNull String key_data) {
        return new AutoValue_SavedKey(null, card_id, card_type, key_data, new Date());
    }
}
