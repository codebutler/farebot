/*
 * CEPASCard.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
 * Copyright (C) 2012 tbonang <bonang@gmail.com>
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

import com.codebutler.farebot.card.Card;
import com.codebutler.farebot.card.CardType;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.ezlink.EZLinkTransitData;
import com.codebutler.farebot.util.Utils;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.Date;
import java.util.List;

@Root(name = "card")
public class CEPASCard extends Card {
    @ElementList(name = "purses") private List<CEPASPurse> mPurses;
    @ElementList(name = "histories") private List<CEPASHistory> mHistories;

    public static CEPASCard dumpTag(Tag tag) throws Exception {
        IsoDep tech = IsoDep.get(tag);

        tech.connect();

        CEPASPurse[] cepasPurses = new CEPASPurse[16];
        CEPASHistory[] cepasHistories = new CEPASHistory[16];

        try {
            CEPASProtocol cepasTag = new CEPASProtocol(tech);

            for (int purseId = 0; purseId < cepasPurses.length; purseId++) {
                cepasPurses[purseId] = cepasTag.getPurse(purseId);
            }

            for (int historyId = 0; historyId < cepasHistories.length; historyId++) {
                if (cepasPurses[historyId].isValid()) {
                    int recordCount = Integer.parseInt(Byte.toString(cepasPurses[historyId].getLogfileRecordCount()));
                    cepasHistories[historyId] = cepasTag.getHistory(historyId, recordCount);
                } else {
                    cepasHistories[historyId] = new CEPASHistory(historyId, (byte[]) null);
                }
            }
        } finally {
            if (tech.isConnected()) {
                tech.close();
            }
        }

        return new CEPASCard(tag.getId(), new Date(), cepasPurses, cepasHistories);
    }

    private CEPASCard(byte[] tagId, Date scannedAt, CEPASPurse[] purses, CEPASHistory[] histories) {
        super(CardType.CEPAS, tagId, scannedAt);
        mPurses = Utils.arrayAsList(purses);
        mHistories = Utils.arrayAsList(histories);
    }

    private CEPASCard() { /* For XML Serializer */ }

    @Override
    public TransitIdentity parseTransitIdentity() {
        if (EZLinkTransitData.check(this)) {
            return EZLinkTransitData.parseTransitIdentity(this);
        }
        return null;
    }

    @Override
    public TransitData parseTransitData() {
        if (EZLinkTransitData.check(this)) {
            return new EZLinkTransitData(this);
        }
        return null;
    }

    public CEPASPurse getPurse(int purse) {
        return mPurses.get(purse);
    }

    public CEPASHistory getHistory(int purse) {
        return mHistories.get(purse);
    }
}
