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

import com.codebutler.farebot.card.TagReader;
import com.codebutler.farebot.card.cepas.raw.RawCEPASCard;
import com.codebutler.farebot.card.cepas.raw.RawCEPASHistory;
import com.codebutler.farebot.card.cepas.raw.RawCEPASPurse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CEPASTagReader extends TagReader<IsoDep, RawCEPASCard> {

    public CEPASTagReader(@NonNull byte[] tagId, @NonNull Tag tag) {
        super(tagId, tag);
    }

    @NonNull
    @Override
    protected IsoDep getTech(@NonNull Tag tag) {
        return IsoDep.get(tag);
    }

    @NonNull
    @Override
    protected RawCEPASCard readTag(@NonNull byte[] tagId, @NonNull Tag tag, @NonNull IsoDep tech) throws Exception {
        List<RawCEPASPurse> purses = new ArrayList<>(16);
        List<RawCEPASHistory> histories = new ArrayList<>(16);

        CEPASProtocol protocol = new CEPASProtocol(tech);

        for (int purseId = 0; purseId < purses.size(); purseId++) {
            purses.set(purseId, protocol.getPurse(purseId));
        }

        for (int historyId = 0; historyId < histories.size(); historyId++) {
            RawCEPASPurse rawCEPASPurse = purses.get(historyId);
            if (rawCEPASPurse.isValid()) {
                int recordCount = Integer.parseInt(Byte.toString(rawCEPASPurse.logfileRecordCount()));
                histories.set(historyId, protocol.getHistory(historyId, recordCount));
            } else {
                histories.set(historyId, RawCEPASHistory.create(historyId, "Invalid Purse"));
            }
        }

        return RawCEPASCard.create(tag.getId(), new Date(), purses, histories);
    }
}
