/*
 * SuicaTransitFactory.java
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * Based on code from http://code.google.com/p/nfc-felica/
 * nfc-felica by Kazzz. See project URL for complete author information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Thanks to these resources for providing additional information about the Suica format:
 * http://www.denno.net/SFCardFan/
 * http://jennychan.web.fc2.com/format/suica.html
 * http://d.hatena.ne.jp/baroqueworksdev/20110206/1297001722
 * http://handasse.blogspot.com/2008/04/python-pasorisuica.html
 * http://sourceforge.jp/projects/felicalib/wiki/suica
 *
 * Some of these resources have been translated into English at:
 * https://github.com/codebutler/farebot/wiki/suica
 */

package com.codebutler.farebot.transit.suica;

import android.content.Context;
import android.support.annotation.NonNull;

import com.codebutler.farebot.card.felica.FelicaBlock;
import com.codebutler.farebot.card.felica.FelicaCard;
import com.codebutler.farebot.card.felica.FelicaDBUtil;
import com.codebutler.farebot.card.felica.FelicaService;
import com.codebutler.farebot.transit.TransitFactory;
import com.codebutler.farebot.transit.TransitIdentity;
import com.codebutler.farebot.transit.Trip;
import com.google.common.collect.ImmutableList;

import net.kazzz.felica.lib.FeliCaLib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SuicaTransitFactory implements TransitFactory<FelicaCard, SuicaTransitData> {

    @NonNull private final FelicaDBUtil mDBUtil;

    public SuicaTransitFactory(@NonNull Context context) {
        mDBUtil = new FelicaDBUtil(context);
    }

    @Override
    public boolean check(FelicaCard card) {
        return (card.getSystem(FeliCaLib.SYSTEMCODE_SUICA) != null);
    }

    @Override
    public TransitIdentity parseIdentity(@NonNull FelicaCard card) {
        // FIXME: Could be ICOCA, etc.
        return TransitIdentity.create("Suica", null);
    }

    @Override
    public SuicaTransitData parseData(@NonNull FelicaCard card) {
        FelicaService service = card.getSystem(FeliCaLib.SYSTEMCODE_SUICA).getService(FeliCaLib.SERVICE_SUICA_HISTORY);

        long previousBalance = -1;

        List<SuicaTrip> trips = new ArrayList<>();

        // Read blocks oldest-to-newest to calculate fare.
        List<FelicaBlock> blocks = service.getBlocks();
        for (int i = (blocks.size() - 1); i >= 0; i--) {
            FelicaBlock block = blocks.get(i);

            SuicaTrip trip = SuicaTrip.create(mDBUtil, block, previousBalance);
            previousBalance = trip.getBalance();

            if (trip.getTimestamp() == 0) {
                continue;
            }

            trips.add(trip);
        }

        // Return trips in descending order.
        Collections.reverse(trips);

        return SuicaTransitData.create(
                null, // FIXME: Find where this is on the card.
                ImmutableList.<Trip>copyOf(trips),
                null,
                null);
    }
}
