package com.codebutler.farebot.base.util

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Pure Kotlin currency formatter for multiplatform support.
 * Replaces java.text.NumberFormat.getCurrencyInstance().
 */
object CurrencyFormatter {
    /**
     * Formats a currency amount.
     *
     * @param amount The amount in the currency's minor unit (e.g., cents for USD)
     * @param currencyCode ISO 4217 currency code (e.g., "USD", "EUR", "JPY")
     * @return Formatted currency string (e.g., "$1.23", "€1,23", "¥123")
     */
    fun formatAmount(
        amount: Int,
        currencyCode: String,
    ): String = formatAmount(amount.toLong(), currencyCode)

    fun formatAmount(
        amount: Long,
        currencyCode: String,
    ): String {
        val config = CURRENCIES[currencyCode] ?: CurrencyConfig("", 2, ".", ",")
        return formatWithConfig(amount, config)
    }

    /**
     * Formats a currency amount with an explicit divisor.
     *
     * @param amount The amount in the smallest unit
     * @param currencyCode ISO 4217 currency code
     * @param divisor The divisor to convert from minor to major units (e.g., 100 for cents)
     * @return Formatted currency string
     */
    fun formatAmount(
        amount: Long,
        currencyCode: String,
        divisor: Int,
    ): String {
        val config = CURRENCIES[currencyCode] ?: CurrencyConfig("", 2, ".", ",")
        val decimalPlaces = decimalPlacesForDivisor(divisor)
        val adjustedConfig = config.copy(decimalPlaces = decimalPlaces)
        return formatWithConfig(amount, adjustedConfig)
    }

    /**
     * Formats a raw double value as currency.
     *
     * @param value The value already in the currency's major unit (e.g., 1.23 for $1.23)
     * @param currencyCode ISO 4217 currency code
     * @return Formatted currency string
     */
    fun formatValue(
        value: Double,
        currencyCode: String,
    ): String {
        val config = CURRENCIES[currencyCode] ?: CurrencyConfig("", 2, ".", ",")
        val minorUnits = (value * pow10(config.decimalPlaces)).roundToLong()
        return formatWithConfig(minorUnits, config)
    }

    private fun decimalPlacesForDivisor(divisor: Int): Int {
        if (divisor <= 1) return 0
        var d = divisor
        var places = 0
        while (d > 1) {
            d /= 10
            places++
        }
        return places
    }

    private fun formatWithConfig(
        minorUnits: Long,
        config: CurrencyConfig,
    ): String {
        val isNegative = minorUnits < 0
        val absAmount = abs(minorUnits)

        val result =
            if (config.decimalPlaces == 0) {
                formatWithGrouping(absAmount, config.groupSeparator)
            } else {
                val divisor = pow10(config.decimalPlaces)
                val major = absAmount / divisor
                val minor = absAmount % divisor
                val majorStr = formatWithGrouping(major, config.groupSeparator)
                val minorStr = minor.toString().padStart(config.decimalPlaces, '0')
                "$majorStr${config.decimalSeparator}$minorStr"
            }

        val prefix = if (isNegative) "-${config.symbol}" else config.symbol
        return "$prefix$result"
    }

    private fun formatWithGrouping(
        value: Long,
        groupSeparator: String,
    ): String {
        val str = value.toString()
        if (str.length <= 3) return str
        val sb = StringBuilder()
        var count = 0
        for (i in str.length - 1 downTo 0) {
            if (count > 0 && count % 3 == 0) {
                sb.insert(0, groupSeparator)
            }
            sb.insert(0, str[i])
            count++
        }
        return sb.toString()
    }

    private fun pow10(n: Int): Long {
        var result = 1L
        repeat(n) { result *= 10 }
        return result
    }

    private data class CurrencyConfig(
        val symbol: String,
        val decimalPlaces: Int,
        val decimalSeparator: String,
        val groupSeparator: String,
    )

    private val CURRENCIES =
        mapOf(
            "USD" to CurrencyConfig("$", 2, ".", ","),
            "AUD" to CurrencyConfig("$", 2, ".", ","),
            "CAD" to CurrencyConfig("$", 2, ".", ","),
            "SGD" to CurrencyConfig("$", 2, ".", ","),
            "NZD" to CurrencyConfig("$", 2, ".", ","),
            "EUR" to CurrencyConfig("\u20AC", 2, ",", "."),
            "GBP" to CurrencyConfig("\u00A3", 2, ".", ","),
            "JPY" to CurrencyConfig("\u00A5", 0, ".", ","),
            "CNY" to CurrencyConfig("\u00A5", 2, ".", ","),
            "HKD" to CurrencyConfig("HK$", 2, ".", ","),
            "TWD" to CurrencyConfig("NT$", 0, ".", ","),
            "IDR" to CurrencyConfig("Rp", 0, ",", "."),
            "BRL" to CurrencyConfig("R$", 2, ",", "."),
            "KRW" to CurrencyConfig("\u20A9", 0, ".", ","),
            "RUB" to CurrencyConfig("\u20BD", 2, ",", " "),
            "ILS" to CurrencyConfig("\u20AA", 2, ".", ","),
            "MYR" to CurrencyConfig("RM", 2, ".", ","),
            "DKK" to CurrencyConfig("kr", 2, ",", "."),
            "SEK" to CurrencyConfig("kr", 2, ",", " "),
            "NOK" to CurrencyConfig("kr", 2, ",", " "),
            "CLP" to CurrencyConfig("$", 0, ",", "."),
            "XXX" to CurrencyConfig("", 2, ".", ","),
        )
}
