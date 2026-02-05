/*
 * OVChipSubscription.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2012-2013 Wilbert Duijvenvoorde <w.a.n.duijvenvoorde@gmail.com>
 * Copyright (C) 2012, 2014-2016 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.transit.ovc

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.transit.Subscription
import kotlin.time.Instant

class OVChipSubscription(
    private val idValue: Int,
    private val unknown1: Int,
    private val validFromDate: Long,
    private val validFromTime: Long,
    private val validToDate: Long,
    private val validToTime: Long,
    private val unknown2: Int,
    private val agency: Int,
    private val machineIdValue: Int,
    private val subscription: Int,
    private val subscriptionAddress: Int,
    private val type1: Int,
    private val type2: Int,
    private val used: Int,
    private val rest: Int
) : Subscription() {

    override val id: Int get() = idValue

    override val machineId: Int get() = machineIdValue

    override val validFrom: Instant?
        get() = if (validFromTime != 0L) {
            OVChipUtil.convertDate(validFromDate.toInt(), validFromTime.toInt())
        } else {
            OVChipUtil.convertDate(validFromDate.toInt())
        }

    override val validTo: Instant?
        get() = if (validToTime != 0L) {
            OVChipUtil.convertDate(validToDate.toInt(), validToTime.toInt())
        } else {
            OVChipUtil.convertDate(validToDate.toInt())
        }

    override val subscriptionName: String
        get() = SUBSCRIPTIONS[subscription]
            ?: "Unknown Subscription (0x${subscription.toLong().toString(16)})"

    override val agencyName: String
        get() = OVChipTransitInfo.getAgencyName(agency)

    override val shortAgencyName: String
        get() = OVChipTransitInfo.getShortAgencyName(agency)

    companion object {
        private val SUBSCRIPTIONS: Map<Int, String> = mapOf(
            /* NS */
            0x0005 to "OV-jaarkaart",
            0x0007 to "OV-Bijkaart 1e klas",
            0x0011 to "NS Businesscard",
            0x0019 to "Voordeelurenabonnement (twee jaar)",
            0x00AF to "Studenten OV-chipkaart week (2009)",
            0x00B0 to "Studenten OV-chipkaart weekend (2009)",
            0x00B1 to "Studentenkaart korting week (2009)",
            0x00B2 to "Studentenkaart korting weekend (2009)",
            0x00C9 to "Reizen op saldo bij NS, 1e klasse",
            0x00CA to "Reizen op saldo bij NS, 2de klasse",
            0x00CE to "Voordeelurenabonnement reizen op saldo",
            0x00E5 to "Reizen op saldo (tijdelijk eerste klas)",
            0x00E6 to "Reizen op saldo (tijdelijk tweede klas)",
            0x00E7 to "Reizen op saldo (tijdelijk eerste klas korting)",
            /* Arriva */
            0x059A to "Dalkorting",
            /* Veolia */
            0x0626 to "DALU Dalkorting",
            /* Connexxion */
            0x0692 to "Daluren Oost-Nederland",
            0x069C to "Daluren Oost-Nederland",
            /* DUO */
            0x09C6 to "Student weekend-vrij",
            0x09C7 to "Student week-korting",
            0x09C9 to "Student week-vrij",
            0x09CA to "Student weekend-korting",
            /* GVB */
            0x0BBD to "Fietssupplement"
        )

        fun create(subscriptionAddress: Int, data: ByteArray?, type1: Int, type2: Int, used: Int, rest: Int): OVChipSubscription {
            val d = data ?: ByteArray(48)

            var id = 0
            var company = 0
            var subscription = 0
            var unknown1 = 0
            var validFromDate = 0L
            var validFromTime = 0L
            var validToDate = 0L
            var validToTime = 0L
            var unknown2 = 0
            var machineId = 0

            var iBitOffset = 0
            val fieldbits = ByteUtils.getBitsFromBuffer(d, 0, 28)
            iBitOffset += 28
            var subfieldbits = 0

            if (fieldbits != 0x00) {
                if ((fieldbits and 0x0000200) != 0x00) {
                    company = ByteUtils.getBitsFromBuffer(d, iBitOffset, 8)
                    iBitOffset += 8
                }
                if ((fieldbits and 0x0000400) != 0x00) {
                    subscription = ByteUtils.getBitsFromBuffer(d, iBitOffset, 16)
                    iBitOffset += 24
                }
                if ((fieldbits and 0x0000800) != 0x00) {
                    id = ByteUtils.getBitsFromBuffer(d, iBitOffset, 24)
                    iBitOffset += 24
                }
                if ((fieldbits and 0x0002000) != 0x00) {
                    unknown1 = ByteUtils.getBitsFromBuffer(d, iBitOffset, 10)
                    iBitOffset += 10
                }
                if ((fieldbits and 0x0200000) != 0x00) {
                    subfieldbits = ByteUtils.getBitsFromBuffer(d, iBitOffset, 9)
                    iBitOffset += 9
                }
                if (subfieldbits != 0x00) {
                    if ((subfieldbits and 0x0000001) != 0x00) {
                        validFromDate = ByteUtils.getBitsFromBuffer(d, iBitOffset, 14).toLong()
                        iBitOffset += 14
                    }
                    if ((subfieldbits and 0x0000002) != 0x00) {
                        validFromTime = ByteUtils.getBitsFromBuffer(d, iBitOffset, 11).toLong()
                        iBitOffset += 11
                    }
                    if ((subfieldbits and 0x0000004) != 0x00) {
                        validToDate = ByteUtils.getBitsFromBuffer(d, iBitOffset, 14).toLong()
                        iBitOffset += 14
                    }
                    if ((subfieldbits and 0x0000008) != 0x00) {
                        validToTime = ByteUtils.getBitsFromBuffer(d, iBitOffset, 11).toLong()
                        iBitOffset += 11
                    }
                    if ((subfieldbits and 0x0000010) != 0x00) {
                        unknown2 = ByteUtils.getBitsFromBuffer(d, iBitOffset, 53)
                        iBitOffset += 53
                    }
                }
                if ((fieldbits and 0x0800000) != 0x00) {
                    machineId = ByteUtils.getBitsFromBuffer(d, iBitOffset, 24)
                    iBitOffset += 24
                }
            } else {
                throw IllegalArgumentException("Not valid")
            }

            return OVChipSubscription(
                idValue = id,
                unknown1 = unknown1,
                validFromDate = validFromDate,
                validFromTime = validFromTime,
                validToDate = validToDate,
                validToTime = validToTime,
                unknown2 = unknown2,
                agency = company,
                machineIdValue = machineId,
                subscription = subscription,
                subscriptionAddress = subscriptionAddress,
                type1 = type1,
                type2 = type2,
                used = used,
                rest = rest
            )
        }
    }
}
