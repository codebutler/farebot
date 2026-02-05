/*
 * FelicaSystem.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014, 2016 Eric Butler <eric@codebutler.com>
 *
 * Contains improvements ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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

package com.codebutler.farebot.card.felica

import kotlinx.serialization.Serializable

@Serializable
data class FelicaSystem(
    val code: Int,
    val services: List<FelicaService>,
    val skipped: Boolean = false,
    val allServiceCodes: Set<Int> = emptySet()
) {

    private val servicesByCode: Map<Int, FelicaService> by lazy {
        services.associateBy { it.serviceCode }
    }

    fun getService(serviceCode: Int): FelicaService? = servicesByCode[serviceCode]

    companion object {
        fun create(
            code: Int,
            services: List<FelicaService>,
            allServiceCodes: Set<Int> = emptySet()
        ): FelicaSystem {
            return FelicaSystem(code, services, allServiceCodes = allServiceCodes)
        }

        fun skipped(code: Int): FelicaSystem {
            return FelicaSystem(code, emptyList(), skipped = true)
        }
    }
}
