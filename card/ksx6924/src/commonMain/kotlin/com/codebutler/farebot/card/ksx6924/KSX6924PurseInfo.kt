/*
 * KSX6924PurseInfo.kt
 *
 * Copyright 2019 Michael Farrell <micolous+git@gmail.com>
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
 *
 * References: https://github.com/micolous/metrodroid/wiki/T-Money
 */
package com.codebutler.farebot.card.ksx6924

import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.ui.ListItem
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToLong
import com.codebutler.farebot.base.util.convertBCDtoInteger
import com.codebutler.farebot.base.util.convertBCDtoLong
import com.codebutler.farebot.base.util.getHexString
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.base.util.hexString
import com.codebutler.farebot.base.util.sliceOffLen
import com.codebutler.farebot.card.ksx6924.KSX6924Utils.parseHexDate
import com.codebutler.farebot.transit.TransitBalance
import com.codebutler.farebot.transit.TransitCurrency
import farebot.card.ksx6924.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * `EFPURSE_INFO` -- FCI tag b0
 *
 * This class parses the purse info structure from a KSX6924 card.
 */
@Serializable
data class KSX6924PurseInfo(
    @Contextual val purseInfoData: ByteArray,
) {
    val cardType: Byte
        get() = purseInfoData[0]

    val alg: Byte
        get() = purseInfoData[1]

    val vk: Byte
        get() = purseInfoData[2]

    val idCenter: Byte
        get() = purseInfoData[3]

    val csn: String
        get() = purseInfoData.getHexString(4, 8)

    val idtr: Long
        get() = purseInfoData.convertBCDtoLong(12, 5)

    val issueDate: LocalDate?
        get() = parseHexDate(purseInfoData.byteArrayToLong(17, 4))

    val expiryDate: LocalDate?
        get() = parseHexDate(purseInfoData.byteArrayToLong(21, 4))

    val userCode: Byte
        get() = purseInfoData[26]

    val disRate: Byte
        get() = purseInfoData[27]

    val balMax: Long
        get() = purseInfoData.byteArrayToLong(27, 4)

    val bra: Int
        get() = purseInfoData.convertBCDtoInteger(31, 2)

    val mmax: Long
        get() = purseInfoData.byteArrayToLong(33, 4)

    val tcode: Byte
        get() = purseInfoData[37]

    val ccode: Byte
        get() = purseInfoData[38]

    val rfu: ByteArray
        get() = purseInfoData.sliceOffLen(39, 8)

    // Convenience functionality
    val serial: String
        get() = NumberUtils.groupString(csn, " ", 4, 4, 4)

    /**
     * Builds a [TransitBalance] from this purse info.
     *
     * @param balance The balance currency
     * @param label Optional label for the balance
     * @param tz The timezone for converting dates to Instants
     */
    fun buildTransitBalance(
        balance: TransitCurrency,
        tz: TimeZone,
        label: String? = null,
    ): TransitBalance =
        TransitBalance(
            balance = balance,
            name = label,
            validFrom = issueDate?.let { KSX6924Utils.localDateToInstant(it, tz) },
            validTo = expiryDate?.let { KSX6924Utils.localDateToInstant(it, tz) },
        )

    /**
     * Returns a list of [ListItem] for displaying the purse info fields.
     */
    fun getInfo(resolver: KSX6924PurseInfoResolver = KSX6924PurseInfoDefaultResolver): List<ListItem> =
        listOf(
            ListItem(Res.string.ksx6924_card_type, resolver.resolveCardType(cardType)),
            ListItem(Res.string.ksx6924_card_issuer, resolver.resolveIssuer(idCenter)),
            ListItem(Res.string.ksx6924_discount_type, resolver.resolveDisRate(disRate)),
        )

    suspend fun getAdvancedInfo(
        resolver: KSX6924PurseInfoResolver = KSX6924PurseInfoDefaultResolver,
    ): FareBotUiTree {
        val b = FareBotUiTree.builder()
        b.item().title(Res.string.ksx6924_crypto_algorithm).value(resolver.resolveCryptoAlgo(alg))
        b.item().title(Res.string.ksx6924_encryption_key_version).value(vk.hexString)
        b.item().title(Res.string.ksx6924_auth_id).value(idtr.hexString)
        b.item().title(Res.string.ksx6924_ticket_type).value(resolver.resolveUserCode(userCode))
        b.item().title(Res.string.ksx6924_max_balance).value(balMax.toString())
        b.item().title(Res.string.ksx6924_branch_code).value(bra.hexString)
        b.item().title(Res.string.ksx6924_one_time_limit).value(mmax.toString())
        b.item().title(Res.string.ksx6924_mobile_carrier).value(resolver.resolveTCode(tcode))
        b.item().title(Res.string.ksx6924_financial_institution).value(resolver.resolveCCode(ccode))
        b.item().title(Res.string.ksx6924_rfu).value(rfu.hex())
        return b.build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as KSX6924PurseInfo
        return purseInfoData.contentEquals(other.purseInfoData)
    }

    override fun hashCode(): Int = purseInfoData.contentHashCode()
}
