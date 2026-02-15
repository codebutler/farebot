/*
 * TransitCurrency.kt
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

package com.codebutler.farebot.transit

import com.codebutler.farebot.base.util.CurrencyFormatter

/**
 * Represents a monetary value on a transit card.
 *
 * @param currency The amount in the smallest unit of the currency (e.g., cents for USD)
 * @param currencyCode ISO 4217 3-letter currency code (e.g., "AUD", "JPY")
 * @param divisor Value to divide by to get the currency's major unit. Default is 100.
 *                For currencies with no fractional part (e.g., JPY, KRW), use 1.
 */
data class TransitCurrency(
    val currency: Int,
    val currencyCode: String,
    val divisor: Int = DEFAULT_DIVISOR,
) {
    constructor(currency: Int, currencyCode: String) : this(currency, currencyCode, DEFAULT_DIVISOR)

    fun formatCurrencyString(isBalance: Boolean = false): String =
        CurrencyFormatter.formatAmount(currency.toLong(), currencyCode, divisor)

    fun negate(): TransitCurrency = TransitCurrency(-currency, currencyCode, divisor)

    operator fun plus(other: TransitCurrency?): TransitCurrency =
        when {
            other == null -> this
            currencyCode != other.currencyCode ->
                throw IllegalArgumentException("Currency codes must be the same")
            divisor != other.divisor ->
                when {
                    divisor > other.divisor && (divisor % other.divisor == 0) ->
                        TransitCurrency(
                            currency + (other.currency * (divisor / other.divisor)),
                            currencyCode,
                            divisor,
                        )
                    other.divisor > divisor && (other.divisor % divisor == 0) ->
                        TransitCurrency(
                            other.currency + (currency * (other.divisor / divisor)),
                            currencyCode,
                            other.divisor,
                        )
                    else ->
                        TransitCurrency(
                            (currency * other.divisor) + (other.currency * divisor),
                            currencyCode,
                            divisor * other.divisor,
                        )
                }
            else ->
                TransitCurrency(currency + other.currency, currencyCode, divisor)
        }

    override fun equals(other: Any?): Boolean {
        if (other !is TransitCurrency) return false
        if (currencyCode != other.currencyCode) return false
        if (divisor == other.divisor) return currency == other.currency
        return currency * other.divisor == other.currency * divisor
    }

    override fun hashCode(): Int {
        var result = currencyCode.hashCode()
        result = 31 * result + currency * 100 / divisor
        return result
    }

    override fun toString(): String = "TransitCurrency.$currencyCode($currency, $divisor)"

    @Suppress("FunctionName")
    companion object {
        internal const val UNKNOWN_CURRENCY_CODE = "XXX"
        private const val DEFAULT_DIVISOR = 100

        fun AUD(cents: Int) = TransitCurrency(cents, "AUD")

        fun BRL(centavos: Int) = TransitCurrency(centavos, "BRL")

        fun CAD(cents: Int) = TransitCurrency(cents, "CAD")

        fun CLP(pesos: Int) = TransitCurrency(pesos, "CLP", 1)

        fun CNY(fen: Int) = TransitCurrency(fen, "CNY")

        fun DKK(ore: Int) = TransitCurrency(ore, "DKK")

        fun EUR(cents: Int) = TransitCurrency(cents, "EUR")

        fun GBP(pence: Int) = TransitCurrency(pence, "GBP")

        fun HKD(cents: Int) = TransitCurrency(cents, "HKD")

        fun IDR(cents: Int) = TransitCurrency(cents, "IDR", 1)

        fun ILS(agorot: Int) = TransitCurrency(agorot, "ILS")

        fun JPY(yen: Int) = TransitCurrency(yen, "JPY", 1)

        fun KRW(won: Int) = TransitCurrency(won, "KRW", 1)

        fun MYR(sen: Int) = TransitCurrency(sen, "MYR")

        fun NZD(cents: Int) = TransitCurrency(cents, "NZD")

        fun RUB(kopeyka: Int) = TransitCurrency(kopeyka, "RUB")

        fun SGD(cents: Int) = TransitCurrency(cents, "SGD")

        fun TWD(cents: Int) = TransitCurrency(cents, "TWD", 1)

        fun USD(cents: Int) = TransitCurrency(cents, "USD")

        fun XXX(cents: Int) = TransitCurrency(cents, UNKNOWN_CURRENCY_CODE)

        fun XXX(
            cents: Int,
            divisor: Int,
        ) = TransitCurrency(cents, UNKNOWN_CURRENCY_CODE, divisor)
    }
}
