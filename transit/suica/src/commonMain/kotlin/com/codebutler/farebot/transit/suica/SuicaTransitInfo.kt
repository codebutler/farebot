/*
 * SuicaTransitInfo.kt
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.transit.suica

import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.suica.generated.resources.Res
import farebot.transit.suica.generated.resources.card_name_suica
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

class SuicaTransitInfo(
    override val serialNumber: String?,
    override val trips: List<Trip>,
    override val subscriptions: List<Subscription>?,
    override val cardName: String = getStringBlocking(Res.string.card_name_suica),
) : TransitInfo() {
    override val balance: TransitBalance?
        get() {
            if (trips.isNotEmpty()) {
                val suicaTrip = trips[0] as? SuicaTrip
                if (suicaTrip != null) {
                    val lastTs = suicaTrip.endTimestamp ?: suicaTrip.startTimestamp
                    val expiry = lastTs?.plus(10, DateTimeUnit.YEAR, TimeZone.of("Asia/Tokyo"))
                    return TransitBalance(
                        balance = TransitCurrency.JPY(suicaTrip.balance),
                        validTo = expiry,
                    )
                }
            }
            return null
        }
}
