package com.codebutler.farebot.card;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.serialize.CardSerializer;

public interface CardUiDependencies {

    @NonNull
    CardSerializer getCardSerializer();
}
