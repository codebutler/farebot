/*
 * KSX6924PurseInfoResolver.kt
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

import com.codebutler.farebot.base.util.NumberUtils

/**
 * Default resolver singleton for [KSX6924PurseInfoResolver].
 *
 * This singleton cannot be subclassed -- one must instead subclass [KSX6924PurseInfoResolver].
 */
object KSX6924PurseInfoDefaultResolver : KSX6924PurseInfoResolver()

/**
 * Class for resolving IDs on a [KSX6924PurseInfo].
 *
 * The "default" implementation is [KSX6924PurseInfoDefaultResolver], which uses the default
 * implementations in this abstract class.
 *
 * For an example of a card-specific implementation, see TMoneyPurseInfoResolver.
 *
 * See https://github.com/micolous/metrodroid/wiki/T-Money for more information about these fields.
 */
abstract class KSX6924PurseInfoResolver {
    fun resolveCryptoAlgo(algo: Byte): String = getOrNone(cryptoAlgos[algo.toInt() and 0xFF], algo)

    fun resolveCardType(type: Byte): String = getOrNone(cardTypes[type.toInt() and 0xFF], type)

    /**
     * Maps an `IDCENTER` (issuer ID) into a name of the issuer.
     */
    protected open val issuers: Map<Int, String> = emptyMap()

    /**
     * Looks up the name of an issuer, and returns an "unknown" value when it is not known.
     */
    fun resolveIssuer(issuer: Byte): String = getOrNone(issuers[issuer.toInt() and 0xFF], issuer)

    /**
     * Maps a `USERCODE` (card holder type) into a name of the card type.
     */
    protected open val userCodes: Map<Int, String> = emptyMap()

    fun resolveUserCode(code: Byte): String = getOrNone(userCodes[code.toInt() and 0xFF], code)

    /**
     * Maps a `DISRATE` (discount rate ID) into a name of the type of discount.
     */
    protected open val disRates: Map<Int, String> = emptyMap()

    fun resolveDisRate(code: Byte): String = getOrNone(disRates[code.toInt() and 0xFF], code)

    /**
     * Maps a `TCODE` (telecommunications carrier ID) into a name of the carrier.
     */
    protected open val tCodes: Map<Int, String> = emptyMap()

    fun resolveTCode(code: Byte): String = getOrNone(tCodes[code.toInt() and 0xFF], code)

    /**
     * Maps a `CCODE` (credit card / bank ID) into a name of the entity.
     */
    protected open val cCodes: Map<Int, String> = emptyMap()

    fun resolveCCode(code: Byte): String = getOrNone(cCodes[code.toInt() and 0xFF], code)

    private fun getOrNone(
        res: String?,
        value: Byte,
    ): String {
        val hexId = NumberUtils.byteToHex(value)
        return when {
            res == null -> "Unknown ($hexId)"
            else -> res
        }
    }

    /**
     * Maps a `ALG` (encryption algorithm type) into a name of the algorithm.
     */
    private val cryptoAlgos: Map<Int, String> =
        mapOf(
            0x00 to "SEED",
            0x10 to "3DES",
        )

    /**
     * Maps a `CARDTYPE` (card type) into a name of the type of card.
     *
     * Specifically, this describes the payment terms of the card (pre-paid, post-paid, etc.)
     */
    private val cardTypes: Map<Int, String> =
        mapOf(
            0x00 to "Pre-paid",
            0x10 to "Post-pay",
            0x15 to "Mobile Post-pay",
        )
}
