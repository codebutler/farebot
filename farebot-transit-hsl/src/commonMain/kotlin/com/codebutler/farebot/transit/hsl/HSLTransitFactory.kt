/*
 * HSLTransitFactory.kt
 *
 * Copyright (C) 2013 Lauri Andler <lauri.andler@gmail.com>
 * Copyright (C) 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.hsl

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.card.desfire.DesfireCard
import kotlin.time.Clock
import com.codebutler.farebot.card.desfire.DesfireRecord
import com.codebutler.farebot.card.desfire.RecordDesfireFile
import com.codebutler.farebot.card.desfire.StandardDesfireFile
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.Trip

class HSLTransitFactory(private val stringResource: StringResource) : TransitFactory<DesfireCard, HSLTransitInfo> {

    companion object {
        private const val EPOCH = 0x32C97ED0L
        private const val APP_ID_V1 = 0x1120ef
        private const val APP_ID_V2 = 0x1420ef

        internal fun createTrip(record: DesfireRecord, stringResource: StringResource): HSLTrip {
            val useData = record.data
            val usefulData = LongArray(useData.size)

            for (i in useData.indices) {
                usefulData[i] = useData[i].toLong() and 0xFF
            }

            val arvo = bitsToLong(0, 1, usefulData)
            val timestamp = cardDateToTimestamp(bitsToLong(1, 14, usefulData), bitsToLong(15, 11, usefulData))
            val expireTimestamp = cardDateToTimestamp(bitsToLong(26, 14, usefulData), bitsToLong(40, 11, usefulData))
            val fare = bitsToLong(51, 14, usefulData)
            val pax = bitsToLong(65, 5, usefulData)
            val newBalance = bitsToLong(70, 20, usefulData)

            return HSLTrip(
                timestampValue = timestamp,
                line = null,
                vehicleNumber = -1L,
                fareValue = fare,
                arvo = arvo,
                expireTimestamp = expireTimestamp,
                pax = pax,
                newBalance = newBalance,
                stringResource = stringResource,
            )
        }

        private fun createRefill(data: ByteArray): HSLRefill {
            val timestamp = cardDateToTimestamp(
                bitsToLong(20, 14, data),
                bitsToLong(34, 11, data)
            )
            val amount = bitsToLong(45, 20, data)
            return HSLRefill.create(timestamp, amount)
        }

        private fun bitsToLong(start: Int, len: Int, data: ByteArray): Long {
            var ret = 0L
            for (i in start until start + len) {
                val bit = ((data[i / 8].toLong() shr (7 - i % 8)) and 1)
                ret = ret or (bit shl ((start + len - 1) - i))
            }
            return ret
        }

        private fun bitsToLong(start: Int, len: Int, data: LongArray): Long {
            var ret = 0L
            for (i in start until start + len) {
                val bit = ((data[i / 8] shr (7 - i % 8)) and 1)
                ret = ret or (bit shl ((start + len - 1) - i))
            }
            return ret
        }

        private fun cardDateToTimestamp(day: Long, minute: Long): Long {
            return EPOCH + day * (60 * 60 * 24) + minute * 60
        }

        private fun getAppId(card: DesfireCard): Int? {
            if (card.getApplication(APP_ID_V1) != null) return APP_ID_V1
            if (card.getApplication(APP_ID_V2) != null) return APP_ID_V2
            return null
        }
    }

    override fun check(card: DesfireCard): Boolean {
        return getAppId(card) != null
    }

    override fun parseIdentity(card: DesfireCard): TransitIdentity {
        try {
            val appId = getAppId(card) ?: throw RuntimeException("No HSL application found")
            val data = (card.getApplication(appId)!!.getFile(0x08) as StandardDesfireFile).data
            val serial = ByteUtils.getHexString(data).substring(2, 20)
            return TransitIdentity.create("HSL", HSLTransitInfo.formatSerial(serial))
        } catch (ex: Exception) {
            throw RuntimeException("Error parsing HSL serial", ex)
        }
    }

    override fun parseInfo(card: DesfireCard): HSLTransitInfo {
        try {
            val appId = getAppId(card) ?: throw RuntimeException("No HSL application found")
            val app = card.getApplication(appId)!!

            var data = (app.getFile(0x08) as StandardDesfireFile).data
            val serialNumber = HSLTransitInfo.formatSerial(ByteUtils.getHexString(data).substring(2, 20))
            val applicationVersion = bitsToLong(0, 4, data).toInt()
            val applicationKeyVersion = bitsToLong(4, 4, data).toInt()
            val platformType = bitsToLong(80, 3, data).toInt()
            val securityLevel = bitsToLong(83, 1, data).toInt()

            data = (app.getFile(0x02) as StandardDesfireFile).data
            val balance = bitsToLong(0, 20, data).toInt()

            // Parse last refill from balance file
            val refillDate = bitsToLong(20, 14, data)
            val refillTime = bitsToLong(34, 11, data)
            val refillAmount = bitsToLong(45, 20, data)

            val trips = parseTrips(card, appId)

            // Add refill as a trip if date is present
            if (refillDate > 0) {
                val refillTimestamp = cardDateToTimestamp(refillDate, refillTime)
                trips.add(HSLTrip(
                    timestampValue = refillTimestamp,
                    line = null,
                    vehicleNumber = -1L,
                    fareValue = -refillAmount,  // Negative fare for refill
                    arvo = -1L,  // Neither arvo nor kausi
                    expireTimestamp = 0,
                    pax = 0,
                    newBalance = balance.toLong(),
                    stringResource = stringResource,
                    isRefill = true,
                ))
                trips.sortWith(Trip.Comparator())
            }

            var balanceIndex = -1
            for (i in trips.indices) {
                if (trips[i].arvo == 1L) {
                    balanceIndex = i
                    break
                }
            }

            data = (app.getFile(0x03) as StandardDesfireFile).data
            val arvoMystery1 = bitsToLong(0, 9, data)
            val arvoDiscoGroup = bitsToLong(9, 5, data)
            val arvoDuration = bitsToLong(14, 13, data)
            val arvoRegional = bitsToLong(27, 5, data)

            val arvoExit = cardDateToTimestamp(bitsToLong(32, 14, data), bitsToLong(46, 11, data))
            val arvoPurchasePrice = bitsToLong(68, 14, data)
            val arvoPurchase = cardDateToTimestamp(bitsToLong(88, 14, data), bitsToLong(102, 11, data))
            val arvoExpire = cardDateToTimestamp(bitsToLong(113, 14, data), bitsToLong(127, 11, data))
            val arvoPax = bitsToLong(138, 6, data)
            val arvoXfer = cardDateToTimestamp(bitsToLong(144, 14, data), bitsToLong(158, 11, data))
            val arvoVehicleNumber = bitsToLong(169, 14, data)
            val arvoUnknown = bitsToLong(183, 2, data)
            val arvoLineJORE = bitsToLong(185, 14, data)
            val arvoJOREExt = bitsToLong(199, 4, data)
            val arvoDirection = bitsToLong(203, 1, data)

            if (balanceIndex > -1) {
                trips[balanceIndex] = trips[balanceIndex].withLineAndVehicleNumber(
                    arvoLineJORE.toString(), arvoVehicleNumber
                )
            } else if (arvoPurchase > 2) {
                trips.add(HSLTrip(
                    timestampValue = arvoPurchase,
                    line = arvoLineJORE.toString(),
                    vehicleNumber = arvoVehicleNumber,
                    fareValue = arvoPurchasePrice,
                    arvo = 1,
                    expireTimestamp = arvoExpire,
                    pax = arvoPax,
                    newBalance = 0,
                    stringResource = stringResource,
                ))
                trips.sortWith(Trip.Comparator())
            }

            var seasonIndex = -1
            for (i in trips.indices) {
                if (trips[i].arvo == 0L) {
                    seasonIndex = i
                    break
                }
            }

            data = (app.getFile(0x01) as StandardDesfireFile).data

            var kausiNoData = false
            if (bitsToLong(19, 14, data) == 0L && bitsToLong(67, 14, data) == 0L) {
                kausiNoData = true
            }

            var kausiStart = cardDateToTimestamp(bitsToLong(19, 14, data), 0)
            var kausiEnd = cardDateToTimestamp(bitsToLong(33, 14, data), 0)
            var kausiPrevStart = cardDateToTimestamp(bitsToLong(67, 14, data), 0)
            var kausiPrevEnd = cardDateToTimestamp(bitsToLong(81, 14, data), 0)
            if (kausiPrevStart > kausiStart) {
                val temp = kausiStart
                val temp2 = kausiEnd
                kausiStart = kausiPrevStart
                kausiEnd = kausiPrevEnd
                kausiPrevStart = temp
                kausiPrevEnd = temp2
            }
            val hasKausi = kausiEnd > (Clock.System.now().epochSeconds.toDouble())
            val kausiPurchase = cardDateToTimestamp(bitsToLong(110, 14, data), bitsToLong(124, 11, data))
            val kausiPurchasePrice = bitsToLong(149, 15, data)
            val kausiLastUse = cardDateToTimestamp(bitsToLong(192, 14, data), bitsToLong(206, 11, data))
            val kausiVehicleNumber = bitsToLong(217, 14, data)
            val kausiUnknown = bitsToLong(231, 2, data)
            val kausiLineJORE = bitsToLong(233, 14, data)
            val kausiJOREExt = bitsToLong(247, 4, data)
            val kausiDirection = bitsToLong(241, 1, data)

            if (seasonIndex > -1) {
                trips[seasonIndex] = trips[seasonIndex].withLineAndVehicleNumber(
                    kausiLineJORE.toString(), kausiVehicleNumber
                )
            } else if (kausiVehicleNumber > 0) {
                trips.add(HSLTrip(
                    timestampValue = kausiPurchase,
                    line = kausiLineJORE.toString(),
                    vehicleNumber = kausiVehicleNumber,
                    fareValue = kausiPurchasePrice,
                    arvo = 0,
                    expireTimestamp = kausiPurchase,
                    pax = 1,
                    newBalance = 0,
                    stringResource = stringResource,
                ))
                trips.sortWith(Trip.Comparator())
            }

            return HSLTransitInfo(
                serialNumber = serialNumber,
                trips = trips,
                balanceValue = balance,
                applicationVersion = applicationVersion,
                applicationKeyVersion = applicationKeyVersion,
                platformType = platformType,
                securityLevel = securityLevel,
                hasKausi = hasKausi,
                kausiStart = kausiStart,
                kausiEnd = kausiEnd,
                kausiPrevStart = kausiPrevStart,
                kausiPrevEnd = kausiPrevEnd,
                kausiPurchasePrice = kausiPurchasePrice,
                kausiLastUse = kausiLastUse,
                kausiPurchase = kausiPurchase,
                kausiNoData = kausiNoData,
                arvoExit = arvoExit,
                arvoPurchase = arvoPurchase,
                arvoExpire = arvoExpire,
                arvoPax = arvoPax,
                arvoPurchasePrice = arvoPurchasePrice,
                arvoXfer = arvoXfer,
                arvoDiscoGroup = arvoDiscoGroup,
                arvoMystery1 = arvoMystery1,
                arvoDuration = arvoDuration,
                arvoRegional = arvoRegional,
                arvoJOREExt = arvoJOREExt,
                arvoVehicleNumber = arvoVehicleNumber,
                arvoUnknown = arvoUnknown,
                arvoLineJORE = arvoLineJORE,
                kausiVehicleNumber = kausiVehicleNumber,
                kausiUnknown = kausiUnknown,
                kausiLineJORE = kausiLineJORE,
                kausiJOREExt = kausiJOREExt,
                arvoDirection = arvoDirection,
                kausiDirection = kausiDirection,
                stringResource = stringResource,
            )
        } catch (ex: Exception) {
            throw RuntimeException("Error parsing HSL data", ex)
        }
    }

    private fun parseTrips(card: DesfireCard, appId: Int): MutableList<HSLTrip> {
        val file = card.getApplication(appId)!!.getFile(0x04)
        if (file is RecordDesfireFile) {
            val recordFile = card.getApplication(appId)!!.getFile(0x04) as RecordDesfireFile
            val useLog = mutableListOf<HSLTrip>()
            for (i in 0 until recordFile.records.size) {
                useLog.add(createTrip(recordFile.records[i], stringResource))
            }
            useLog.sortWith(Trip.Comparator())
            return useLog
        }
        return mutableListOf()
    }
}
