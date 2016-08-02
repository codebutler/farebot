package com.codebutler.farebot.card.classic.raw;

import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.classic.ClassicCard;
import com.codebutler.farebot.card.classic.ClassicSector;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class RawClassicCard implements RawCard<ClassicCard> {

    @NonNull
    public static RawClassicCard create(
            @NonNull byte[] tagId,
            @NonNull Date scannedAt,
            @NonNull List<RawClassicSector> sectors) {
        return new AutoValue_RawClassicCard(ByteArray.create(tagId), scannedAt, sectors);
    }

    @NonNull
    public static TypeAdapter<RawClassicCard> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawClassicCard.GsonTypeAdapter(gson);
    }

    @NonNull
    @Override
    public CardType cardType() {
        return CardType.MifareClassic;
    }

    @NonNull
    @Override
    public ClassicCard parse() {
        List<ClassicSector> sectors = Lists.newArrayList(Iterables.transform(sectors(),
                new Function<RawClassicSector, ClassicSector>() {
                    @Override
                    public ClassicSector apply(RawClassicSector rawClassicSector) {
                        return rawClassicSector.parse();
                    }
                }));
        return ClassicCard.create(tagId(), scannedAt(), sectors);
    }

    @NonNull
    public abstract List<RawClassicSector> sectors();
}
