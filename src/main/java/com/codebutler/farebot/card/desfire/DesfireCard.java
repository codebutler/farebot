/*
 * DesfireCard.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.desfire;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardRawDataFragmentClass;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.fragment.DesfireCardRawDataFragment;
import com.google.auto.value.AutoValue;

import java.util.Date;
import java.util.List;

@AutoValue
@CardRawDataFragmentClass(DesfireCardRawDataFragment.class)
public abstract class DesfireCard extends Card {

    @NonNull
    public static DesfireCard create(
            @NonNull ByteArray tagId,
            @NonNull Date scannedAt,
            @NonNull List<DesfireApplication> applications,
            @NonNull DesfireManufacturingData manufacturingData) {
        return new AutoValue_DesfireCard(
                tagId,
                scannedAt,
                applications,
                manufacturingData);
    }

    @NonNull
    public CardType getCardType() {
        return CardType.MifareDesfire;
    }

    @NonNull
    public abstract List<DesfireApplication> getApplications();

    @NonNull
    public abstract DesfireManufacturingData getManufacturingData();

    @Nullable
    public DesfireApplication getApplication(int appId) {
        for (DesfireApplication app : getApplications()) {
            if (app.getId() == appId) {
                return app;
            }
        }
        return null;
    }
}
