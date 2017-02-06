package com.codebutler.farebot.transit;

import android.support.annotation.NonNull;

import com.codebutler.farebot.card.Card;

public interface TransitFactory<C extends Card, T extends TransitData> {

    boolean check(@NonNull C card);

    @NonNull
    TransitIdentity parseIdentity(@NonNull C card);

    @NonNull
    T parseData(@NonNull C card);
}
