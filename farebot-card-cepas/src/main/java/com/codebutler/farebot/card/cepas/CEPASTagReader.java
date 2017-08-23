/*
 * CEPASTagReader.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.card.cepas;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.card.TagReader;
import com.codebutler.farebot.card.cepas.raw.RawCEPASCard;
import com.codebutler.farebot.card.cepas.raw.RawCEPASHistory;
import com.codebutler.farebot.card.cepas.raw.RawCEPASPurse;
import com.codebutler.farebot.key.CardKeys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CEPASTagReader extends TagReader<IsoDep, RawCEPASCard, CardKeys> {

    public CEPASTagReader(@NonNull byte[] tagId, @NonNull Tag tag) {
        super(tagId, tag, null);
    }

    @NonNull
    @Override
    protected IsoDep getTech(@NonNull Tag tag) {
        return IsoDep.get(tag);
    }

    @NonNull
    @Override
    protected RawCEPASCard readTag(
            @NonNull byte[] tagId,
            @NonNull Tag tag,
            @NonNull IsoDep tech,
            @Nullable CardKeys cardKeys) throws Exception {
        RawCEPASPurse[] purses = new RawCEPASPurse[16];
        RawCEPASHistory[] histories = new RawCEPASHistory[16];

        CEPASProtocol protocol = new CEPASProtocol(tech);

        for (int purseId = 0; purseId < purses.length; purseId++) {
            purses[purseId] = protocol.getPurse(purseId);
        }

        for (int historyId = 0; historyId < histories.length; historyId++) {
            RawCEPASPurse rawCEPASPurse = purses[historyId];
            if (rawCEPASPurse.isValid()) {
                int recordCount = Integer.parseInt(Byte.toString(rawCEPASPurse.logfileRecordCount()));
                histories[historyId] = protocol.getHistory(historyId, recordCount);
            } else {
                histories[historyId] = RawCEPASHistory.create(historyId, "Invalid Purse");
            }
        }

        return RawCEPASCard.create(tag.getId(), new Date(), Arrays.asList(purses), Arrays.asList(histories));
    }
}
