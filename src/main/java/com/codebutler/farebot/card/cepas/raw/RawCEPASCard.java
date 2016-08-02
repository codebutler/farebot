package com.codebutler.farebot.card.cepas.raw;

import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.cepas.CEPASCard;
import com.codebutler.farebot.card.cepas.CEPASHistory;
import com.codebutler.farebot.card.cepas.CEPASPurse;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

@AutoValue
public abstract class RawCEPASCard implements RawCard<CEPASCard> {

    @NonNull
    public static RawCEPASCard create(
            @NonNull byte[] tagId,
            @NonNull Date scannedAt,
            @NonNull List<RawCEPASPurse> purses,
            @NonNull List<RawCEPASHistory> histories) {
        return new AutoValue_RawCEPASCard(ByteArray.create(tagId), scannedAt, purses, histories);
    }

    @NonNull
    public static TypeAdapter<RawCEPASCard> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawCEPASCard.GsonTypeAdapter(gson);
    }

    @NonNull
    @Override
    public CardType cardType() {
        return CardType.CEPAS;
    }

    @NonNull
    @Override
    public CEPASCard parse() {
        List<CEPASPurse> purses = newArrayList(transform(purses(),
                new Function<RawCEPASPurse, CEPASPurse>() {
                    @Override
                    public CEPASPurse apply(RawCEPASPurse rawCEPASPurse) {
                        return rawCEPASPurse.parse();
                    }
                }));
        List<CEPASHistory> histories = newArrayList(transform(histories(),
                new Function<RawCEPASHistory, CEPASHistory>() {
                    @Override
                    public CEPASHistory apply(RawCEPASHistory rawCEPASHistory) {
                        return rawCEPASHistory.parse();
                    }
                }));
        return CEPASCard.create(tagId(), scannedAt(), purses, histories);
    }

    @NonNull
    abstract List<RawCEPASPurse> purses();

    @NonNull
    abstract List<RawCEPASHistory> histories();
}
