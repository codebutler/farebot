/*
 * NorticTransitInfo.kt
 *
 * Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018-2022 Google Inc.
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
 */

package com.codebutler.farebot.transit.serialonly

import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.ui.ListItemInterface
import com.codebutler.farebot.base.util.Luhn
import com.codebutler.farebot.base.util.NumberUtils
import farebot.farebot_transit_serialonly.generated.resources.Res
import farebot.farebot_transit_serialonly.generated.resources.country
import farebot.farebot_transit_serialonly.generated.resources.country_code_format
import farebot.farebot_transit_serialonly.generated.resources.expiry_date
import farebot.farebot_transit_serialonly.generated.resources.owner_company
import farebot.farebot_transit_serialonly.generated.resources.retailer_company
import farebot.farebot_transit_serialonly.generated.resources.unknown_company
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString

class NorticTransitInfo(
    private val mCountry: Int,
    private val mFormat: Int,
    private val mCardIdSelector: Int,
    private val mSerial: Long,
    private val mValidityEndDate: Int,
    private val mOwnerCompany: Int,
    private val mRetailerCompany: Int,
    private val mCardKeyVersion: Int
) : SerialOnlyTransitInfo() {

    override val extraInfo: List<ListItemInterface>
        get() {
            // Convert validity end date: days since 1997-01-01
            val expiryStr = try {
                val epoch = LocalDate(1997, 1, 1)
                val expiryDate = epoch.toEpochDays() + mValidityEndDate
                LocalDate.fromEpochDays(expiryDate).toString()
            } catch (_: Exception) {
                "Day $mValidityEndDate since 1997-01-01"
            }

            return listOf(
                ListItem(Res.string.country, runBlocking { getString(Res.string.country_code_format, mCountry) }),
                ListItem(Res.string.expiry_date, expiryStr),
                ListItem(Res.string.owner_company, getCompanyName(mOwnerCompany)),
                ListItem(Res.string.retailer_company, getCompanyName(mRetailerCompany))
            )
        }

    override val reason get() = Reason.LOCKED
    override val cardName get() = getName(mOwnerCompany)
    override val serialNumber get() = formatSerial(mOwnerCompany, mSerial)

    companion object {
        private val operators = mapOf(
            1 to "Ruter",
            120 to "Länstrafiken Norrbotten",
            121 to "LLT Luleå Lokaltrafik",
            160 to "AtB",
            190 to "Troms fylkestraffikk"
        )

        private fun getCompanyName(company: Int): String =
            operators[company] ?: runBlocking { getString(Res.string.unknown_company, company) }

        internal fun getName(ownerCompany: Int): String = when (ownerCompany) {
            1 -> "Ruter Travelcard"
            120 -> "Norrbotten Bus Pass"
            121 -> "LLT Bus Pass"
            160 -> "t:card"
            190 -> "Tromskortet"
            else -> "Nortic"
        }

        internal fun formatSerial(ownerCompany: Int, serial: Long): String = when (ownerCompany) {
            1 -> {
                val luhn = Luhn.calculateLuhn(serial.toString())
                NumberUtils.groupString(
                    "02003" + NumberUtils.zeroPad(serial, 10) + luhn,
                    " ", 4, 4, 4
                )
            }
            160, 190 -> {
                val partial = NumberUtils.zeroPad(ownerCompany / 10, 2) +
                    NumberUtils.zeroPad(ownerCompany, 3) +
                    NumberUtils.zeroPad(serial, 10)
                val luhn = Luhn.calculateLuhn(partial)
                NumberUtils.groupString(partial + luhn, " ", 4, 4, 4)
            }
            else -> serial.toString()
        }
    }
}
