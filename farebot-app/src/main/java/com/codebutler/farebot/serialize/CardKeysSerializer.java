package com.codebutler.farebot.serialize;

import android.support.annotation.NonNull;

import com.codebutler.farebot.key.CardKeys;

public interface CardKeysSerializer {

    @NonNull
    String serialize(@NonNull CardKeys cardKeys);

    @NonNull
    CardKeys deserialize(@NonNull String data);
}
