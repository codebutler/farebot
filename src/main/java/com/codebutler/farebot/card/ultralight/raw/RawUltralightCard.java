package com.codebutler.farebot.card.ultralight.raw;

import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.ultralight.UltralightCard;
import com.codebutler.farebot.card.ultralight.UltralightPage;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class RawUltralightCard implements RawCard<UltralightCard> {

    @NonNull
    public static RawUltralightCard create(
            @NonNull byte[] tagId,
            @NonNull Date scannedAt,
            @NonNull List<UltralightPage> pages,
            int type) {
        return new AutoValue_RawUltralightCard(ByteArray.create(tagId), scannedAt, pages, type);
    }

    @NonNull
    public static TypeAdapter<RawUltralightCard> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawUltralightCard.GsonTypeAdapter(gson);
    }

    @NonNull
    @Override
    public CardType cardType() {
        return CardType.MifareUltralight;
    }

    @NonNull
    @Override
    public UltralightCard parse() {
        return UltralightCard.create(tagId(), scannedAt(), pages(), ultralightType());
    }

    @NonNull
    public abstract List<UltralightPage> pages();

    abstract int ultralightType();
}
