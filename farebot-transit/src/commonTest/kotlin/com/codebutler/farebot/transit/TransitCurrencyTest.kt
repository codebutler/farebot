/*
 * TransitCurrencyTest.kt
 *
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFailsWith

class TransitCurrencyTest {

    @Test
    fun testUSDFormat() {
        val currency = TransitCurrency.USD(350)
        val formatted = currency.formatCurrencyString()
        // Should format as $3.50 or equivalent
        assertEquals(350, currency.currency)
        assertEquals("USD", currency.currencyCode)
        assertEquals(100, currency.divisor)
    }

    @Test
    fun testAUDFormat() {
        val currency = TransitCurrency.AUD(500)
        assertEquals(500, currency.currency)
        assertEquals("AUD", currency.currencyCode)
        assertEquals(100, currency.divisor)
    }

    @Test
    fun testJPYFormat() {
        val currency = TransitCurrency.JPY(1000)
        assertEquals(1000, currency.currency)
        assertEquals("JPY", currency.currencyCode)
        assertEquals(1, currency.divisor)
    }

    @Test
    fun testTWDFormat() {
        val currency = TransitCurrency.TWD(245)
        assertEquals(245, currency.currency)
        assertEquals("TWD", currency.currencyCode)
        assertEquals(1, currency.divisor)
    }

    @Test
    fun testNegate() {
        val currency = TransitCurrency.USD(350)
        val negated = currency.negate()
        assertEquals(-350, negated.currency)
        assertEquals("USD", negated.currencyCode)
    }

    @Test
    fun testAddSameCurrency() {
        val a = TransitCurrency.USD(100)
        val b = TransitCurrency.USD(250)
        val sum = a + b
        assertEquals(TransitCurrency.USD(350), sum)
    }

    @Test
    fun testAddNull() {
        val a = TransitCurrency.USD(100)
        val sum = a + null
        assertEquals(a, sum)
    }

    @Test
    fun testAddDifferentCurrencyFails() {
        val a = TransitCurrency.USD(100)
        val b = TransitCurrency.AUD(100)
        assertFailsWith<IllegalArgumentException> {
            a + b
        }
    }

    @Test
    fun testAddDifferentDivisor() {
        val a = TransitCurrency(100, "USD", 100)  // $1.00
        val b = TransitCurrency(5, "USD", 10)      // $0.50
        val sum = a + b
        // 100/100 + 5/10 = 1.00 + 0.50 = 1.50
        // Should normalize: a has higher divisor, 5 * (100/10) = 50
        // Result: 100 + 50 = 150 / 100
        assertEquals(TransitCurrency(150, "USD", 100), sum)
    }

    @Test
    fun testEquality() {
        assertEquals(TransitCurrency.USD(350), TransitCurrency.USD(350))
        assertNotEquals(TransitCurrency.USD(350), TransitCurrency.USD(100))
        assertNotEquals(TransitCurrency.USD(350), TransitCurrency.AUD(350))
    }

    @Test
    fun testEqualityDifferentDivisor() {
        // $3.50 represented differently
        val a = TransitCurrency(350, "USD", 100)
        val b = TransitCurrency(35, "USD", 10)
        assertEquals(a, b)
    }

    @Test
    fun testFactoryMethods() {
        assertEquals("AUD", TransitCurrency.AUD(0).currencyCode)
        assertEquals("BRL", TransitCurrency.BRL(0).currencyCode)
        assertEquals("CAD", TransitCurrency.CAD(0).currencyCode)
        assertEquals("EUR", TransitCurrency.EUR(0).currencyCode)
        assertEquals("GBP", TransitCurrency.GBP(0).currencyCode)
        assertEquals("HKD", TransitCurrency.HKD(0).currencyCode)
        assertEquals("JPY", TransitCurrency.JPY(0).currencyCode)
        assertEquals("KRW", TransitCurrency.KRW(0).currencyCode)
        assertEquals("SGD", TransitCurrency.SGD(0).currencyCode)
        assertEquals("TWD", TransitCurrency.TWD(0).currencyCode)
        assertEquals("USD", TransitCurrency.USD(0).currencyCode)
    }

    @Test
    fun testZeroDivisorCurrencies() {
        // JPY, KRW, TWD, CLP, IDR should have divisor=1
        assertEquals(1, TransitCurrency.JPY(0).divisor)
        assertEquals(1, TransitCurrency.KRW(0).divisor)
        assertEquals(1, TransitCurrency.TWD(0).divisor)
        assertEquals(1, TransitCurrency.CLP(0).divisor)
        assertEquals(1, TransitCurrency.IDR(0).divisor)
    }

    @Test
    fun testToString() {
        val currency = TransitCurrency.USD(350)
        assertEquals("TransitCurrency.USD(350, 100)", currency.toString())
    }
}
