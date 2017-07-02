/*
 * ClassicCard.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.classic;

import android.content.Context;
import android.support.annotation.NonNull;

import com.codebutler.farebot.base.ui.FareBotUiTree;
import com.codebutler.farebot.base.util.ByteArray;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardType;
import com.google.auto.value.AutoValue;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class ClassicCard extends Card {

    @NonNull
    public static ClassicCard create(
            @NonNull ByteArray tagId,
            @NonNull Date scannedAt,
            @NonNull List<ClassicSector> sectors) {
        return new AutoValue_ClassicCard(tagId, scannedAt, sectors);
    }

    @NonNull
    public CardType getCardType() {
        return CardType.MifareClassic;
    }

    @NonNull
    public abstract List<ClassicSector> getSectors();

    public ClassicSector getSector(int index) {
        return getSectors().get(index);
    }

    @NonNull
    @Override
    public FareBotUiTree getAdvancedUi(Context context) {
        FareBotUiTree.Builder cardUiBuilder = FareBotUiTree.builder(context);
        for (ClassicSector sector : getSectors()) {
            String sectorIndexString = Integer.toHexString(sector.getIndex());
            FareBotUiTree.Item.Builder sectorUiBuilder = cardUiBuilder.item();
            if (sector instanceof UnauthorizedClassicSector) {
                sectorUiBuilder.title(context.getString(
                        R.string.classic_unauthorized_sector_title_format, sectorIndexString));
            } else if (sector instanceof InvalidClassicSector) {
                InvalidClassicSector errorSector = (InvalidClassicSector) sector;
                sectorUiBuilder.title(context.getString(
                        R.string.classic_invalid_sector_title_format, sectorIndexString, errorSector.getError()));
            } else {
                DataClassicSector dataClassicSector = (DataClassicSector) sector;
                sectorUiBuilder.title(context.getString(R.string.classic_sector_title_format, sectorIndexString));
                for (ClassicBlock block : dataClassicSector.getBlocks()) {
                    sectorUiBuilder.item()
                            .title(context.getString(
                                    R.string.classic_block_title_format,
                                    String.valueOf(block.getIndex())))
                            .value(block.getData());
                }
            }
        }
        return cardUiBuilder.build();
    }
}
