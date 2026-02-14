/*
 * EdyTransitInfo.kt
 *
 * Authors:
 * Chris Norden
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
 */

package com.codebutler.farebot.transit.edy

import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.edy.generated.resources.Res
import farebot.transit.edy.generated.resources.card_name_edy

class EdyTransitInfo(
    override val trips: List<Trip>,
    private val serialNumberData: ByteArray,
    private val currentBalance: Int,
) : TransitInfo() {
    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.JPY(currentBalance))

    override val serialNumber: String?
        get() {
            val serialNumber = serialNumberData
            val str = StringBuilder(20)
            for (i in 0..6 step 2) {
                str.append((serialNumber[i].toInt() and 0xFF).toString(16).padStart(2, '0').uppercase())
                str.append((serialNumber[i + 1].toInt() and 0xFF).toString(16).padStart(2, '0').uppercase())
                if (i < 6) {
                    str.append(" ")
                }
            }
            return str.toString()
        }

    override val subscriptions: List<Subscription>? = null

    override val cardName: String get() = getStringBlocking(Res.string.card_name_edy)

    fun getSerialNumberData(): ByteArray = serialNumberData

    fun getCurrentBalance(): Int = currentBalance

    companion object {
        const val FELICA_MODE_EDY_DEBIT = 0x20
        const val FELICA_MODE_EDY_CHARGE = 0x02
        const val FELICA_MODE_EDY_GIFT = 0x04

        fun create(
            trips: List<Trip>,
            serialNumberData: ByteArray,
            currentBalance: Int,
        ): EdyTransitInfo = EdyTransitInfo(trips, serialNumberData, currentBalance)
    }
}
