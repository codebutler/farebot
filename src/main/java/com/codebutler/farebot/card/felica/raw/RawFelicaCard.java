package com.codebutler.farebot.card.felica.raw;

import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.card.RawCard;
import com.codebutler.farebot.card.felica.FelicaCard;
import com.codebutler.farebot.card.felica.FelicaSystem;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import net.kazzz.felica.lib.FeliCaLib;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class RawFelicaCard implements RawCard {

    @NonNull
    public static RawFelicaCard create(
            @NonNull byte[] tagId,
            @NonNull Date scannedAt,
            @NonNull FeliCaLib.IDm idm,
            @NonNull FeliCaLib.PMm pmm,
            @NonNull List<FelicaSystem> systems) {
        return new AutoValue_RawFelicaCard(ByteArray.create(tagId), scannedAt, idm, pmm, systems);
    }

    @NonNull
    @Override
    public CardType cardType() {
        return CardType.FeliCa;
    }

    @NonNull
    public static TypeAdapter<RawFelicaCard> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_RawFelicaCard.GsonTypeAdapter(gson);
    }

    @NonNull
    @Override
    public Card parse() {
        return FelicaCard.create(tagId(), scannedAt(), idm(), pmm(), systems());
    }

    @NonNull
    abstract FeliCaLib.IDm idm();

    @NonNull
    abstract FeliCaLib.PMm pmm();

    @NonNull
    public abstract List<FelicaSystem> systems();
}
