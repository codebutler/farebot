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

import com.codebutler.farebot.base.ui.HeaderListItem
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Subscription
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.TransitInfo
import com.codebutler.farebot.transit.Trip
import farebot.transit.kmt.generated.resources.Res
import farebot.transit.kmt.generated.resources.kmt_last_trx_amount
import farebot.transit.kmt.generated.resources.kmt_longname
import farebot.transit.kmt.generated.resources.kmt_other_data
import farebot.transit.kmt.generated.resources.kmt_transaction_counter

class KMTTransitInfo(
    override val trips: List<Trip>,
    private val serialNumberData: ByteArray,
    private val currentBalance: Int,
    private val transactionCounter: Int = 0,
    private val lastTransAmount: Int = 0,
) : TransitInfo() {
    override val balance: TransitBalance
        get() = TransitBalance(balance = TransitCurrency.IDR(currentBalance))

    override val serialNumber: String? = serialNumberData.decodeToString()

    override val subscriptions: List<Subscription>? = null

    override val cardName: String
        get() = getStringBlocking(Res.string.kmt_longname)

    override val info: List<ListItemInterface>
        get() =
            listOf(
                HeaderListItem(Res.string.kmt_other_data),
                ListItem(Res.string.kmt_transaction_counter, transactionCounter.toString()),
                ListItem(
                    Res.string.kmt_last_trx_amount,
                    TransitCurrency.IDR(lastTransAmount).formatCurrencyString(isBalance = false),
                ),
            )

    companion object {
        fun create(
            trips: List<Trip>,
            serialNumberData: ByteArray,
            currentBalance: Int,
            transactionCounter: Int = 0,
            lastTransAmount: Int = 0,
        ): KMTTransitInfo = KMTTransitInfo(trips, serialNumberData, currentBalance, transactionCounter, lastTransAmount)
    }
}
