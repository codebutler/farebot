package com.codebutler.farebot.serialize;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.RawCard;

public interface CardSerializer {

    @NonNull
    String serialize(@NonNull RawCard card);

    @NonNull
    RawCard deserialize(@NonNull String data);
}
