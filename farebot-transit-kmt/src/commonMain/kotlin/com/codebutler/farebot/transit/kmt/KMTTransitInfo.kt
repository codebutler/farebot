/*
 * KMTTransitInfo.kt
 *
 * Authors:
 * Bondan Sumbodo <sybond@gmail.com>
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
 */

package com.codebutler.farebot.transit.kmt

import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_kmt.generated.resources.Res
import farebot.farebot_transit_kmt.generated.resources.kmt_longname
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class KMTTransitInfo(
    override val trips: List<Trip>,
    private val serialNumberData: ByteArray,
    private val currentBalance: Int
) : TransitInfo() {

    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.IDR(currentBalance))

    override val serialNumber: String? = serialNumberData.decodeToString()

    override val subscriptions: List<Subscription>? = null

    override val cardName: String
        get() = runBlocking { getString(Res.string.kmt_longname) }

    fun getSerialNumberData(): ByteArray = serialNumberData

    fun getCurrentBalance(): Int = currentBalance

    companion object {
        fun create(trips: List<Trip>, serialNumberData: ByteArray, currentBalance: Int): KMTTransitInfo =
            KMTTransitInfo(trips, serialNumberData, currentBalance)
    }
}
