/*
 * FelicaCard.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2013 Chris Norden <thisiscnn@gmail.com>
 * Copyright (C) 2016 Michael Farrell <micolous+git@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.card.felica;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.base.ui.FareBotUiTree;
import com.codebutler.farebot.base.util.ByteArray;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import net.kazzz.felica.lib.FeliCaLib;

import java.util.Date;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class FelicaCard extends Card {

    @NonNull
    public static FelicaCard create(
            @NonNull ByteArray tagId,
            @NonNull Date scannedAt,
            @NonNull FeliCaLib.IDm idm,
            @NonNull FeliCaLib.PMm pmm,
            @NonNull List<FelicaSystem> systems) {
        return new AutoValue_FelicaCard(
                tagId,
                scannedAt,
                idm,
                pmm,
                systems);
    }

    @NonNull
    public CardType getCardType() {
        return CardType.FeliCa;
    }

    @NonNull
    public static TypeAdapter<FelicaCard> typeAdapter(@NonNull Gson gson) {
        return new AutoValue_FelicaCard.GsonTypeAdapter(gson);
    }

    @NonNull
    public abstract FeliCaLib.IDm getIDm();

    @NonNull
    public abstract FeliCaLib.PMm getPMm();

    @NonNull
    public abstract List<FelicaSystem> getSystems();

    @Nullable
    public FelicaSystem getSystem(int systemCode) {
        for (FelicaSystem system : getSystems()) {
            if (system.getCode() == systemCode) {
                return system;
            }
        }
        return null;
    }

    @NonNull
    @Override
    public FareBotUiTree getAdvancedUi(Context context) {
        FareBotUiTree.Builder cardUiBuilder = FareBotUiTree.builder(context);
        cardUiBuilder.item().title("IDm").value(getIDm());
        cardUiBuilder.item().title("PMm").value(getPMm());
        FareBotUiTree.Item.Builder systemsUiBuilder = cardUiBuilder.item().title("Systems");
        for (FelicaSystem system : getSystems()) {
            FareBotUiTree.Item.Builder systemUiBuilder = systemsUiBuilder.item()
                    .title(String.format("System: %s", Integer.toHexString(system.getCode())));
            for (FelicaService service : system.getServices()) {
                FareBotUiTree.Item.Builder serviceUiBuilder = systemUiBuilder.item()
                        .title((String.format(
                                "Service: 0x%s (%s)",
                                Integer.toHexString(service.getServiceCode()),
                                FelicaUtils.getFriendlyServiceName(system.getCode(), service.getServiceCode()))));
                for (FelicaBlock block : service.getBlocks()) {
                    serviceUiBuilder.item()
                            .title(String.format(Locale.US, "Block %02d", block.getAddress()))
                            .value(block.getData());
                }
            }
        }
        return cardUiBuilder.build();
    }
}
