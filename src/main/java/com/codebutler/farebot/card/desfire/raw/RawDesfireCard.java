package com.codebutler.farebot.card.desfire.raw;

import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.desfire.DesfireApplication;
import com.codebutler.farebot.card.desfire.DesfireCard;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

@AutoValue
public abstract class RawDesfireCard implements RawCard {

    @NonNull
    public static RawDesfireCard create(
            @NonNull byte[] tagId,
            @NonNull Date date,
            @NonNull List<RawDesfireApplication> apps,
            @NonNull RawDesfireManufacturingData manufData) {
        return new AutoValue_RawDesfireCard(ByteArray.create(tagId), date, apps, manufData);
    }

    @NonNull
    public static TypeAdapter<RawDesfireCard> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawDesfireCard.GsonTypeAdapter(gson);
    }

    @NonNull
    @Override
    public CardType cardType() {
        return CardType.MifareDesfire;
    }

    @NonNull
    @Override
    public DesfireCard parse() {
        List<DesfireApplication> applications = newArrayList(transform(applications(),
                new Function<RawDesfireApplication, DesfireApplication>() {
                    @Override
                    public DesfireApplication apply(RawDesfireApplication rawDesfireApplication) {
                        return rawDesfireApplication.parse();
                    }
                }));
        return DesfireCard.create(tagId(), scannedAt(), applications, manufacturingData().parse());
    }

    @NonNull
    public abstract List<RawDesfireApplication> applications();

    @NonNull
    public abstract RawDesfireManufacturingData manufacturingData();
}
