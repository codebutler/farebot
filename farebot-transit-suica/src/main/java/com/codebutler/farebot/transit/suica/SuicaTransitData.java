/*
 * SuicaTransitData.java
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

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codebutler.farebot.transit.Refill;
import com.codebutler.farebot.transit.Subscription;
import com.codebutler.farebot.transit.TransitData;
import com.codebutler.farebot.transit.Trip;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class SuicaTransitData extends TransitData {

    @NonNull
    static SuicaTransitData create(
            @Nullable String serialNumber,
            @NonNull List<Trip> trips,
            @NonNull List<Refill> refills,
            @NonNull List<Subscription> subscriptions) {
        return new AutoValue_SuicaTransitData(serialNumber, trips, refills, subscriptions);
    }

    @NonNull
    @Override
    public String getBalanceString(@NonNull Resources resources) {
        if (getTrips().size() > 0) {
            return getTrips().get(0).getBalanceString();
        }
        return null;
    }

    @NonNull
    @Override
    public String getCardName(@NonNull Resources resources) {
        return "Suica"; // FIXME: Could be ICOCA, etc.
    }
}
