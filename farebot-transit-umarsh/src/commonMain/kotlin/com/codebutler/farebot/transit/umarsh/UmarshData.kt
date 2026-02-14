/*
 * UmarshData.kt
 *
 * Copyright 2019 Google
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

package com.codebutler.farebot.transit.umarsh

import com.codebutler.farebot.base.util.getStringBlocking
import farebot.farebot_transit_umarsh.generated.resources.*
import farebot.farebot_transit_umarsh.generated.resources.Res

enum class UmarshDenomination {
    UNLIMITED,
    TRIPS,
    RUB,
}

data class UmarshTariff(
    val name: String,
    val cardName: String? = null,
    val denomination: UmarshDenomination? = null,
)

data class UmarshSystem(
    val cardName: String,
    val tariffs: Map<Int, UmarshTariff> = emptyMap(),
)

// Reference: https://github.com/micolous/metrodroid/wiki/Umarsh
val systemsMap =
    mapOf(
        12 to
            UmarshSystem(
                cardName = getStringBlocking(Res.string.card_name_yoshkar_ola),
                tariffs = mapOf(0x287f00 to UmarshTariff(name = getStringBlocking(Res.string.card_name_yoshkar_ola))),
            ),
        18 to
            UmarshSystem(
                cardName = getStringBlocking(Res.string.card_name_strizh),
                tariffs =
                    mapOf(
                        0x0a7f00 to UmarshTariff(name = getStringBlocking(Res.string.umarsh_adult)),
                        0x1e7f00 to UmarshTariff(name = getStringBlocking(Res.string.umarsh_student)),
                        0x247f00 to UmarshTariff(name = getStringBlocking(Res.string.umarsh_school)),
                        0x587f00 to UmarshTariff(name = getStringBlocking(Res.string.umarsh_adult)),
                    ),
            ),
        22 to
            UmarshSystem(
                cardName = getStringBlocking(Res.string.card_name_barnaul),
                tariffs = mapOf(0x0a002e to UmarshTariff(name = getStringBlocking(Res.string.barnaul_ewallet))),
            ),
        33 to UmarshSystem(cardName = getStringBlocking(Res.string.card_name_siticard_vladimir)),
        43 to
            UmarshSystem(
                cardName = getStringBlocking(Res.string.card_name_kirov),
                tariffs = mapOf(0x5000ff to UmarshTariff(name = getStringBlocking(Res.string.umarsh_adult))),
            ),
        52 to
            UmarshSystem(
                cardName = getStringBlocking(Res.string.card_name_siticard),
                tariffs =
                    mapOf(
                        0x0a7f00 to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.siticard_adult_60min_xfer_purse),
                                denomination = UmarshDenomination.RUB,
                            ),
                        0x0a007f to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.siticard_adult_60min_xfer_purse),
                                denomination = UmarshDenomination.RUB,
                            ),
                        0x21007f to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.siticard_purse_sarov),
                                denomination = UmarshDenomination.RUB,
                            ),
                        0x2564ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.siticard_edinyj_3_days),
                                denomination = UmarshDenomination.UNLIMITED,
                            ),
                        0x31002f to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.siticard_adult_90min_xfer_purse),
                                denomination = UmarshDenomination.RUB,
                            ),
                        0x33690f to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.siticard_edinyj_16_trips),
                                denomination = UmarshDenomination.TRIPS,
                            ),
                        0x34690f to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.siticard_edinyj_30_trips),
                                denomination = UmarshDenomination.TRIPS,
                            ),
                        0x3c7f00 to UmarshTariff(name = getStringBlocking(Res.string.siticard_aerial_tramway)),
                    ),
            ),
        55 to
            UmarshSystem(
                cardName = getStringBlocking(Res.string.card_name_omka),
                tariffs =
                    mapOf(
                        0x5700ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.umarsh_school),
                                denomination = UmarshDenomination.RUB,
                            ),
                        0x5780ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.umarsh_school),
                                denomination = UmarshDenomination.RUB,
                            ),
                        0x6300ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_russia_omsk_citizen),
                                denomination = UmarshDenomination.RUB,
                            ),
                        0x5800ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.umarsh_student),
                                denomination = UmarshDenomination.RUB,
                            ),
                        0x5900ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_russia_omsk_pensioner),
                                denomination = UmarshDenomination.RUB,
                            ),
                        0x15eaff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_russia_omsk_social),
                                denomination = UmarshDenomination.TRIPS,
                            ),
                        0x5500ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_russia_omsk_school_unlimited_15d),
                                denomination = UmarshDenomination.UNLIMITED,
                            ),
                        0x5600ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_russia_omsk_school_unlimited_1m),
                                denomination = UmarshDenomination.UNLIMITED,
                            ),
                        0x5b00ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_russia_omsk_adult_unlimited_15d),
                                denomination = UmarshDenomination.UNLIMITED,
                            ),
                        0x5c00ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_russia_omsk_adult_unlimited_1m),
                                denomination = UmarshDenomination.UNLIMITED,
                            ),
                        0x6000ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_russia_omsk_citizen),
                                denomination = UmarshDenomination.RUB,
                            ),
                        0x6100ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_russia_omsk_adult_60d_60t),
                                denomination = UmarshDenomination.TRIPS,
                            ),
                        0x6200ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_russia_omsk_adult_60d_30t),
                                denomination = UmarshDenomination.TRIPS,
                            ),
                        0x5300ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_russia_omsk_adult_60d_60t),
                                denomination = UmarshDenomination.TRIPS,
                            ),
                        0x5400ff to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_russia_omsk_adult_60d_30t),
                                denomination = UmarshDenomination.TRIPS,
                            ),
                    ),
            ),
        58 to
            UmarshSystem(
                cardName = getStringBlocking(Res.string.card_name_penza),
                tariffs = mapOf(0x1400ff to UmarshTariff(name = getStringBlocking(Res.string.umarsh_adult))),
            ),
        66 to
            UmarshSystem(
                cardName = getStringBlocking(Res.string.card_name_ekarta),
                tariffs =
                    mapOf(
                        0x42640f to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.monthly_subscription),
                                denomination = UmarshDenomination.UNLIMITED,
                            ),
                    ),
            ),
        91 to
            UmarshSystem(
                cardName = getStringBlocking(Res.string.card_name_crimea_trolleybus),
                tariffs =
                    mapOf(
                        0x3d7f00 to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_crimea_parus_school),
                                cardName = getStringBlocking(Res.string.card_name_crimea_parus_school),
                                denomination = UmarshDenomination.UNLIMITED,
                            ),
                        0x467f00 to
                            UmarshTariff(
                                name = getStringBlocking(Res.string.card_name_crimea_trolleybus),
                                cardName = getStringBlocking(Res.string.card_name_crimea_trolleybus),
                                denomination = UmarshDenomination.UNLIMITED,
                            ),
                    ),
            ),
    )
