package com.codebutler.farebot.base.util

object Luhn {
    /**
     * Given a partial card number, calculate the Luhn check digit.
     *
     * @param partialCardNumber Partial card number.
     * @return Final digit for card number.
     */
    fun calculateLuhn(partialCardNumber: String): Int {
        val checkDigit = luhnChecksum(partialCardNumber + "0")
        return if (checkDigit == 0) 0 else 10 - checkDigit
    }

    /**
     * Given a complete card number, validate the Luhn check digit.
     *
     * @param cardNumber Complete card number.
     * @return true if valid, false if invalid.
     */
    fun validateLuhn(cardNumber: String): Boolean = luhnChecksum(cardNumber) == 0

    private fun luhnChecksum(cardNumber: String): Int {
        val digits = digitsOf(cardNumber)
        // even digits, counting from the last digit on the card
        val evenDigits = IntArray((cardNumber.length + 1) / 2)
        var checksum = 0
        var p = 0
        val q = cardNumber.length - 1

        for (i in cardNumber.indices) {
            if (i % 2 == 1) {
                // we treat it as a 1-indexed array
                // so the first digit is odd
                evenDigits[p++] = digits[q - i]
            } else {
                checksum += digits[q - i]
            }
        }

        for (d in evenDigits) {
            checksum += sum(digitsOf(d * 2))
        }

        return checksum % 10
    }

    private fun digitsOf(integer: Int): IntArray = digitsOf(integer.toLong())

    private fun digitsOf(integer: Long): IntArray = digitsOf(integer.toString())

    private fun digitsOf(integer: String): IntArray {
        val out = IntArray(integer.length)
        for (index in integer.indices) {
            out[index] = integer[index].digitToInt()
        }
        return out
    }

    /**
     * Sum an array of integers.
     *
     * @param ints Input array of integers.
     * @return All the values added together.
     */
    private fun sum(ints: IntArray): Int {
        var sum = 0
        for (i in ints) {
            sum += i
        }
        return sum
    }
}
