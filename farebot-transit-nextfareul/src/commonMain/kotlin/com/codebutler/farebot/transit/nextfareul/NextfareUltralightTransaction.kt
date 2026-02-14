/*
 * NextfareUltralightTransaction.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.transit.nextfareul

import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.base.util.getStringBlocking
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Transaction
import com.codebutler.farebot.transit.TransitCurrency
import farebot.farebot_transit_nextfareul.generated.resources.Res
import farebot.farebot_transit_nextfareul.generated.resources.nextfareul_unknown_route
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

abstract class NextfareUltralightTransaction(
    raw: ByteArray,
    private val baseDate: Int,
) : Transaction() {
    private val mTime: Int
    private val mDate: Int
    protected val mRoute: Int
    protected val mLocation: Int
    private val mMachineCode: Int
    private val mRecordType: Int
    private val mSeqNo: Int
    val balance: Int
    val expiry: Int

    init {
        val timeField = raw.byteArrayToIntReversed(0, 2)
        mRecordType = timeField and 0x1f
        mTime = timeField shr 5
        mDate = raw[2].toInt() and 0xff
        val seqnofield = raw.byteArrayToIntReversed(4, 3)
        mSeqNo = seqnofield and 0x7f
        balance = seqnofield shr 5 and 0x7ff
        expiry = raw[8].toInt()
        mLocation = raw.byteArrayToIntReversed(9, 2)
        mRoute = raw[11].toInt()
        mMachineCode = raw.byteArrayToInt(12, 2)
    }

    override val routeNames: List<String>
        get() = listOf(getStringBlocking(Res.string.nextfareul_unknown_route, mRoute.toString(16)))

    override val station: Station?
        get() =
            if (mLocation == 0) {
                null
            } else {
                Station.unknown(mLocation.toString())
            }

    override val timestamp: Instant?
        get() = NextfareUltralightTransitData.parseDateTime(timezone, baseDate, mDate, mTime)

    protected abstract val timezone: TimeZone

    protected abstract val isBus: Boolean

    override val isTapOff: Boolean
        get() = mRecordType == 6 && !isBus

    override val isTapOn: Boolean
        get() = (
            mRecordType == 2 ||
                mRecordType == 4 ||
                mRecordType == 6 &&
                isBus ||
                mRecordType == 0x12 ||
                mRecordType == 0x16
        )

    override val fare: TransitCurrency?
        get() = null

    // handle wraparound correctly
    fun isSeqNoGreater(other: NextfareUltralightTransaction) = mSeqNo - other.mSeqNo and 0x7f < 0x3f

    override fun shouldBeMerged(other: Transaction) =
        (
            other is NextfareUltralightTransaction &&
                other.mSeqNo == mSeqNo + 1 and 0x7f &&
                super.shouldBeMerged(other)
        )

    override fun isSameTrip(other: Transaction) =
        (
            other is NextfareUltralightTransaction &&
                !isBus &&
                !other.isBus &&
                mRoute == other.mRoute
        )

    override val agencyName: String?
        get() = null
}
