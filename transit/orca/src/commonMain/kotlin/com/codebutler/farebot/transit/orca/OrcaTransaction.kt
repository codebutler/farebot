/*
 * OrcaTransaction.kt
 *
 * Copyright (C) 2011-2013, 2019 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright (C) 2018 Karl Koscher <supersat@cs.washington.edu>
 * Copyright (C) 2014 Kramer Campbell <kramer@kramerc.com>
 * Copyright (C) 2015 Sean CyberKitsune McClenaghan <cyberkitsune09@gmail.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 *
 * Thanks to:
 * Karl Koscher <supersat@cs.washington.edu>
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

package com.codebutler.farebot.transit.orca

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.base.util.FormattedString
import com.codebutler.farebot.transit.Station
import com.codebutler.farebot.transit.Transaction
import com.codebutler.farebot.transit.TransitCurrency
import com.codebutler.farebot.transit.Trip
import farebot.transit.orca.generated.resources.*
import kotlin.time.Instant

class OrcaTransaction(
    private val mTimestamp: Long,
    private val mCoachNum: Int,
    private val mFtpType: Int,
    private val mFare: Int,
    private val mNewBalance: Int,
    private val mAgency: Int,
    private val mTransType: Int,
    private val mIsTopup: Boolean,
) : Transaction() {
    override val timestamp: Instant?
        get() = if (mTimestamp == 0L) null else Instant.fromEpochSeconds(mTimestamp)

    override val isTapOff: Boolean
        get() = !mIsTopup && mTransType == TRANS_TYPE_TAP_OUT

    override val isCancel: Boolean
        get() = !mIsTopup && mTransType == TRANS_TYPE_CANCEL_TRIP

    override val isTapOn: Boolean
        get() = !mIsTopup && mTransType == TRANS_TYPE_TAP_IN

    override val routeNames: List<String>
        get() =
            when {
                mIsTopup -> listOf("Top-up")
                isLink ->
                    super.routeNames.ifEmpty {
                        listOf("Link Light Rail")
                    }
                isSounder ->
                    super.routeNames.ifEmpty {
                        listOf("Sounder Train")
                    }
                isSeattleStreetcar ->
                    super.routeNames.ifEmpty {
                        listOf("Streetcar")
                    }
                mAgency == AGENCY_ST -> listOf("Express Bus")
                isMonorail -> listOf("Seattle Monorail")
                isWaterTaxi -> listOf("Water Taxi")
                isSwift ->
                    super.routeNames.ifEmpty {
                        listOf("Bus Rapid Transit")
                    }
                mAgency == AGENCY_KCM ->
                    when (mFtpType) {
                        FTP_TYPE_BUS -> listOf("Bus")
                        FTP_TYPE_BRT ->
                            super.routeNames.ifEmpty {
                                listOf("Bus Rapid Transit")
                            }
                        else -> emptyList()
                    }
                else -> emptyList()
            }

    override val fare: TransitCurrency?
        get() = TransitCurrency.USD(if (mIsTopup || mTransType == TRANS_TYPE_TAP_OUT) -mFare else mFare)

    override val station: Station?
        get() {
            if (mIsTopup) return null
            if (isSeattleStreetcar) return lookupMdstStation(ORCA_STR_STREETCAR, mCoachNum)
            if (isRapidRide || isSwift) return lookupMdstStation(ORCA_STR_BRT, mCoachNum)

            val id = (mAgency shl 16) or (mCoachNum and 0xffff)
            val s = lookupMdstStation(ORCA_STR, id)
            if (s != null) return s

            if (isLink || isSounder || mAgency == AGENCY_WSF) {
                return Station.unknown(mCoachNum.toString())
            }
            return null
        }

    override val vehicleID: String?
        get() =
            when {
                mIsTopup -> mCoachNum.toString()
                isLink ||
                    isSounder ||
                    mAgency == AGENCY_WSF ||
                    isSeattleStreetcar ||
                    isSwift ||
                    isRapidRide ||
                    isMonorail -> null
                else -> mCoachNum.toString()
            }

    override val mode: Trip.Mode
        get() =
            when {
                mIsTopup -> Trip.Mode.TICKET_MACHINE
                isMonorail -> Trip.Mode.MONORAIL
                isWaterTaxi -> Trip.Mode.FERRY
                else ->
                    when (mFtpType) {
                        FTP_TYPE_LINK -> Trip.Mode.METRO
                        FTP_TYPE_SOUNDER -> Trip.Mode.TRAIN
                        FTP_TYPE_FERRY -> Trip.Mode.FERRY
                        FTP_TYPE_STREETCAR -> Trip.Mode.TRAM
                        else -> Trip.Mode.BUS
                    }
            }

    override val agencyName: FormattedString?
        get() =
            when {
                mIsTopup -> null
                // Seattle Monorail Services uses KCM's agency ID.
                isMonorail -> FormattedString(Res.string.transit_orca_agency_sms)
                // The King County Water Taxi is now a separate agency but uses KCM's agency ID
                isWaterTaxi -> FormattedString(Res.string.transit_orca_agency_kcwt)
                else ->
                    MdstStationLookup.getOperatorName(ORCA_STR, mAgency, isShort = false)?.let { FormattedString(it) }
                        ?: getAgencyNameFallback(false)
                        ?: FormattedString(Res.string.transit_orca_agency_unknown, mAgency.toString())
            }

    override val shortAgencyName: FormattedString?
        get() =
            when {
                mIsTopup -> null
                // Seattle Monorail Services uses KCM's agency ID.
                isMonorail -> FormattedString(Res.string.transit_orca_agency_sms_short)
                // The King County Water Taxi is now a separate agency but uses KCM's agency ID
                isWaterTaxi -> FormattedString(Res.string.transit_orca_agency_kcwt_short)
                else ->
                    MdstStationLookup.getOperatorName(ORCA_STR, mAgency, isShort = true)?.let { FormattedString(it) }
                        ?: getAgencyNameFallback(true)
                        ?: FormattedString(Res.string.transit_orca_agency_unknown_short)
            }

    private fun getAgencyNameFallback(isShort: Boolean): FormattedString? =
        when (mAgency) {
            AGENCY_CT ->
                FormattedString(
                    if (isShort) Res.string.transit_orca_agency_ct_short else Res.string.transit_orca_agency_ct,
                )
            AGENCY_ET ->
                FormattedString(
                    if (isShort) Res.string.transit_orca_agency_et_short else Res.string.transit_orca_agency_et,
                )
            AGENCY_KCM ->
                FormattedString(
                    if (isShort) Res.string.transit_orca_agency_kcm_short else Res.string.transit_orca_agency_kcm,
                )
            AGENCY_KT ->
                FormattedString(
                    if (isShort) Res.string.transit_orca_agency_kt_short else Res.string.transit_orca_agency_kt,
                )
            AGENCY_PT ->
                FormattedString(
                    if (isShort) Res.string.transit_orca_agency_pt_short else Res.string.transit_orca_agency_pt,
                )
            AGENCY_ST ->
                FormattedString(
                    if (isShort) Res.string.transit_orca_agency_st_short else Res.string.transit_orca_agency_st,
                )
            AGENCY_WSF ->
                FormattedString(
                    if (isShort) Res.string.transit_orca_agency_wsf_short else Res.string.transit_orca_agency_wsf,
                )
            else -> null
        }

    override fun isSameTrip(other: Transaction): Boolean = other is OrcaTransaction && mAgency == other.mAgency

    /**
     * Returns raw debugging fields for this transaction.
     * Matches Metrodroid's getRawFields(RawLevel.ALL) output showing agency, type, ftp, coach, fare, newBal in hex.
     */
    fun getRawFields(): List<ListItemInterface> {
        val prefix = if (mIsTopup) "topup, " else ""
        return listOf(
            ListItem(
                Res.string.transit_orca_raw_fields,
                prefix +
                    listOf(
                        "agency" to mAgency,
                        "type" to mTransType,
                        "ftp" to mFtpType,
                        "coach" to mCoachNum,
                        "fare" to mFare,
                        "newBal" to mNewBalance,
                    ).joinToString { "${it.first} = 0x${it.second.toString(16)}" },
            ),
        )
    }

    private val isLink: Boolean
        get() = mAgency == AGENCY_ST && mFtpType == FTP_TYPE_LINK

    private val isSounder: Boolean
        get() = mAgency == AGENCY_ST && mFtpType == FTP_TYPE_SOUNDER

    private val isSeattleStreetcar: Boolean
        get() = mFtpType == FTP_TYPE_STREETCAR

    private val isMonorail: Boolean
        get() = mAgency == AGENCY_KCM && mFtpType == FTP_TYPE_PURSE_DEBIT && mCoachNum == COACH_NUM_MONORAIL

    private val isWaterTaxi: Boolean
        get() = mAgency == AGENCY_KCM && mFtpType == FTP_TYPE_PURSE_DEBIT && mCoachNum != COACH_NUM_MONORAIL

    private val isRapidRide: Boolean
        get() = mAgency == AGENCY_KCM && mFtpType == FTP_TYPE_BRT

    private val isSwift: Boolean
        get() = mAgency == AGENCY_CT && mFtpType == FTP_TYPE_BRT

    private fun lookupMdstStation(
        dbName: String,
        stationId: Int,
    ): Station? {
        val result = MdstStationLookup.getStation(dbName, stationId) ?: return null
        return Station(
            stationName = result.stationName,
            shortStationName = result.shortStationName,
            companyName = result.companyName,
            lineNames = result.lineNames,
            latitude = if (result.hasLocation) result.latitude else null,
            longitude = if (result.hasLocation) result.longitude else null,
        )
    }

    companion object {
        private const val ORCA_STR = "orca"
        private const val ORCA_STR_BRT = "orca_brt"
        private const val ORCA_STR_STREETCAR = "orca_streetcar"

        const val TRANS_TYPE_TAP_IN = 0x03
        const val TRANS_TYPE_TAP_OUT = 0x07
        const val TRANS_TYPE_PURSE_USE = 0x0c
        const val TRANS_TYPE_CANCEL_TRIP = 0x01
        const val TRANS_TYPE_PASS_USE = 0x60

        const val AGENCY_KCM = 0x04
        const val AGENCY_PT = 0x06
        const val AGENCY_ST = 0x07
        const val AGENCY_CT = 0x02
        const val AGENCY_WSF = 0x08
        const val AGENCY_ET = 0x03
        const val AGENCY_KT = 0x05

        const val FTP_TYPE_FERRY = 0x08
        const val FTP_TYPE_SOUNDER = 0x09
        const val FTP_TYPE_CUSTOMER_SERVICE = 0x0B
        const val FTP_TYPE_BUS = 0x80
        const val FTP_TYPE_LINK = 0xFB
        const val FTP_TYPE_STREETCAR = 0xF9
        const val FTP_TYPE_BRT = 0xFA
        const val FTP_TYPE_PURSE_DEBIT = 0xFE

        const val COACH_NUM_MONORAIL = 0x3

        fun parse(
            data: ByteArray,
            isTopup: Boolean,
        ): OrcaTransaction {
            val agency = ByteUtils.getBitsFromBuffer(data, 24, 4)
            val timestamp = ByteUtils.getBitsFromBuffer(data, 28, 32).toLong() and 0xFFFFFFFFL
            val ftpType = ByteUtils.getBitsFromBuffer(data, 60, 8)
            val coachNum = ByteUtils.getBitsFromBuffer(data, 68, 24)
            val fare = ByteUtils.getBitsFromBuffer(data, 120, 15)
            val transType = ByteUtils.getBitsFromBuffer(data, 136, 8)
            val newBalance = ByteUtils.getBitsFromBuffer(data, 272, 16)

            return OrcaTransaction(
                mTimestamp = timestamp,
                mCoachNum = coachNum,
                mFtpType = ftpType,
                mFare = fare,
                mNewBalance = newBalance,
                mAgency = agency,
                mTransType = transType,
                mIsTopup = isTopup,
            )
        }
    }
}
