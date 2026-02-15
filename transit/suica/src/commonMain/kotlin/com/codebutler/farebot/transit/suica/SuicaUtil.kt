/*
 * SuicaUtil.kt
 *
 * Based on code from http://code.google.com/p/nfc-felica/
 * nfc-felica by Kazzz. See project URL for complete author information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Thanks to these resources for providing additional information about the Suica format:
 * http://www.denno.net/SFCardFan/
 * http://jennychan.web.fc2.com/format/suica.html
 * http://d.hatena.ne.jp/baroqueworksdev/20110206/1297001722
 * http://handasse.blogspot.com/2008/04/python-pasorisuica.html
 * http://sourceforge.jp/projects/felicalib/wiki/suica
 *
 * Some of these resources have been translated into English at:
 * https://github.com/micolous/metrodroid/wiki/Suica
 */

package com.codebutler.farebot.transit.suica

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.transit.Station
import farebot.transit.suica.generated.resources.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant
import com.codebutler.farebot.base.util.FormattedString

internal object SuicaUtil {
    internal val TZ = TimeZone.of("Asia/Tokyo")

    // Service code sets unique to each IC card type.
    // Cards are identified by the presence of service codes that don't appear on other card types.
    // Source: https://www.wdic.org/w/RAIL/%E3%82%B5%E3%82%A4%E3%83%90%E3%83%8D%E8%A6%8F%E6%A0%BC%20(IC%E3%82%AB%E3%83%BC%E3%83%89)
    private val HAYAKAKEN_SERVICES =
        setOf(
            0x1f88,
            0x1f8a,
            0x2048,
            0x204a,
            0x2448,
            0x244a,
            0x2488,
            0x248a,
            0x24c8,
            0x24ca,
            0x2508,
            0x250a,
            0x2548,
            0x254a,
        )
    private val ICOCA_SERVICES =
        setOf(
            0x1a48,
            0x1a4a,
            0x1a88,
            0x1a8a,
            0x9608,
            0x960a,
        )
    private val KITACA_SERVICES =
        setOf(
            0x1848,
            0x184b,
            0x2088,
            0x208b,
            0x20c8,
            0x20cb,
            0x2108,
            0x210b,
            0x2148,
            0x214b,
            0x2188,
            0x218b,
        )
    private val MANACA_SERVICES =
        setOf(
            0x9888,
            0x988b,
            0x98cc,
            0x98cf,
            0x9908,
            0x990a,
            0x9948,
            0x994a,
            0x9988,
            0x998b,
        )
    private val NIMOCA_SERVICES =
        setOf(
            0x1f48,
            0x1f4a,
            0x1f88,
            0x1f8a,
            0x1fc8,
            0x1fca,
            0x2008,
            0x200a,
            0x2048,
            0x204a,
        )
    private val PASMO_SERVICES =
        setOf(
            0x1848,
            0x184b,
            0x1908,
            0x190a,
            0x1948,
            0x194b,
            0x1988,
            0x198b,
            0x1cc8,
            0x1cca,
            0x1d08,
            0x1d0a,
            0x2308,
            0x230a,
            0x2348,
            0x234b,
            0x2388,
            0x238b,
            0x23c8,
            0x23cb,
        )
    private val PITAPA_SERVICES =
        setOf(
            0x1b88,
            0x1b8a,
            0x9748,
            0x974a,
        )
    private val SUGOCA_SERVICES =
        setOf(
            0x1f88,
            0x1f8a,
            0x2048,
            0x204b,
            0x21c8,
            0x21cb,
            0x2208,
            0x220a,
            0x2248,
            0x224a,
            0x2288,
            0x228a,
        )
    private val SUICA_SERVICES =
        setOf(
            0x1808,
            0x180a,
            0x1848,
            0x184b,
            0x18c8,
            0x18ca,
            0x1908,
            0x190a,
            0x1948,
            0x194b,
            0x1988,
            0x198b,
            0x2308,
            0x230a,
            0x2348,
            0x234b,
            0x2388,
            0x238b,
            0x23c8,
            0x23cb,
        )
    private val TOICA_SERVICES =
        setOf(
            0x1848,
            0x184b,
            0x1e08,
            0x1e0a,
            0x1e48,
            0x1e4a,
            0x1e88,
            0x1e8a,
            0x1e8b,
            0x1ecc,
            0x1ecf,
        )

    private enum class ICCardType(
        val localName: org.jetbrains.compose.resources.StringResource,
        val uniqueServices: Set<Int>,
    ) {
        Hayakaken(
            Res.string.card_name_hayakaken,
            (
                HAYAKAKEN_SERVICES
                    subtract ICOCA_SERVICES
                    subtract KITACA_SERVICES
                    subtract MANACA_SERVICES
                    subtract NIMOCA_SERVICES
                    subtract PASMO_SERVICES
                    subtract PITAPA_SERVICES
                    subtract SUGOCA_SERVICES
                    subtract SUICA_SERVICES
                    subtract TOICA_SERVICES
            ),
        ),
        ICOCA(
            Res.string.card_name_icoca,
            (
                ICOCA_SERVICES
                    subtract HAYAKAKEN_SERVICES
                    subtract KITACA_SERVICES
                    subtract MANACA_SERVICES
                    subtract NIMOCA_SERVICES
                    subtract PASMO_SERVICES
                    subtract PITAPA_SERVICES
                    subtract SUGOCA_SERVICES
                    subtract SUICA_SERVICES
                    subtract TOICA_SERVICES
            ),
        ),
        Kitaca(
            Res.string.card_name_kitaca,
            (
                KITACA_SERVICES
                    subtract HAYAKAKEN_SERVICES
                    subtract ICOCA_SERVICES
                    subtract MANACA_SERVICES
                    subtract NIMOCA_SERVICES
                    subtract PASMO_SERVICES
                    subtract PITAPA_SERVICES
                    subtract SUGOCA_SERVICES
                    subtract SUICA_SERVICES
                    subtract TOICA_SERVICES
            ),
        ),
        Manaca(
            Res.string.card_name_manaca,
            (
                MANACA_SERVICES
                    subtract HAYAKAKEN_SERVICES
                    subtract ICOCA_SERVICES
                    subtract KITACA_SERVICES
                    subtract NIMOCA_SERVICES
                    subtract PASMO_SERVICES
                    subtract PITAPA_SERVICES
                    subtract SUGOCA_SERVICES
                    subtract SUICA_SERVICES
                    subtract TOICA_SERVICES
            ),
        ),
        Nimoca(
            Res.string.card_name_nimoca,
            (
                NIMOCA_SERVICES
                    subtract HAYAKAKEN_SERVICES
                    subtract ICOCA_SERVICES
                    subtract KITACA_SERVICES
                    subtract MANACA_SERVICES
                    subtract PASMO_SERVICES
                    subtract PITAPA_SERVICES
                    subtract SUGOCA_SERVICES
                    subtract SUICA_SERVICES
                    subtract TOICA_SERVICES
            ),
        ),
        PASMO(
            Res.string.card_name_pasmo,
            (
                PASMO_SERVICES
                    subtract HAYAKAKEN_SERVICES
                    subtract KITACA_SERVICES
                    subtract ICOCA_SERVICES
                    subtract MANACA_SERVICES
                    subtract NIMOCA_SERVICES
                    subtract PITAPA_SERVICES
                    subtract SUGOCA_SERVICES
                    subtract SUICA_SERVICES
                    subtract TOICA_SERVICES
            ),
        ),
        PiTaPa(
            Res.string.card_name_pitapa,
            (
                PITAPA_SERVICES
                    subtract HAYAKAKEN_SERVICES
                    subtract ICOCA_SERVICES
                    subtract KITACA_SERVICES
                    subtract MANACA_SERVICES
                    subtract NIMOCA_SERVICES
                    subtract PASMO_SERVICES
                    subtract SUGOCA_SERVICES
                    subtract SUICA_SERVICES
                    subtract TOICA_SERVICES
            ),
        ),
        SUGOCA(
            Res.string.card_name_sugoca,
            (
                SUGOCA_SERVICES
                    subtract HAYAKAKEN_SERVICES
                    subtract ICOCA_SERVICES
                    subtract KITACA_SERVICES
                    subtract MANACA_SERVICES
                    subtract NIMOCA_SERVICES
                    subtract PASMO_SERVICES
                    subtract PITAPA_SERVICES
                    subtract SUICA_SERVICES
                    subtract TOICA_SERVICES
            ),
        ),
        Suica(
            Res.string.card_name_suica,
            (
                SUICA_SERVICES
                    subtract HAYAKAKEN_SERVICES
                    subtract ICOCA_SERVICES
                    subtract KITACA_SERVICES
                    subtract MANACA_SERVICES
                    subtract NIMOCA_SERVICES
                    subtract PASMO_SERVICES
                    subtract PITAPA_SERVICES
                    subtract SUGOCA_SERVICES
                    subtract TOICA_SERVICES
            ),
        ),
        TOICA(
            Res.string.card_name_toica,
            (
                TOICA_SERVICES
                    subtract HAYAKAKEN_SERVICES
                    subtract ICOCA_SERVICES
                    subtract KITACA_SERVICES
                    subtract MANACA_SERVICES
                    subtract NIMOCA_SERVICES
                    subtract PASMO_SERVICES
                    subtract PITAPA_SERVICES
                    subtract SUGOCA_SERVICES
                    subtract SUICA_SERVICES
            ),
        ),
        ;

        init {
            require(uniqueServices.isNotEmpty()) {
                "Japan IC cards need at least one unique service code"
            }
        }
    }

    /**
     * Gets the StringResource for the card name, or null if unknown or ambiguous.
     */
    private fun getCardNameResource(services: Set<Int>): org.jetbrains.compose.resources.StringResource? =
        ICCardType.entries
            .map {
                Pair(it.localName, (it.uniqueServices intersect services).size)
            }.singleOrNull {
                it.second > 0
            }?.first

    /**
     * Detect the card type from the set of service codes present on the card.
     * Returns the card name (e.g., "Suica", "PASMO", "ICOCA") or "Japan IC" if unknown.
     */
    fun getCardName(
        serviceCodes: Set<Int>,
    ): FormattedString {
        val nameRes = getCardNameResource(serviceCodes)
        return if (nameRes != null) {
            FormattedString(nameRes)
        } else {
            FormattedString(Res.string.card_name_japan_ic)
        }
    }

    fun extractDate(
        isProductSale: Boolean,
        data: ByteArray,
    ): Instant? {
        val date = data.byteArrayToInt(4, 2)
        if (date == 0) {
            return null
        }
        val yy = date shr 9
        val mm = (date shr 5) and 0xf
        val dd = date and 0x1f

        // Product sales have time, too.
        val hh: Int
        val min: Int
        if (isProductSale) {
            val time = data.byteArrayToInt(6, 2)
            hh = time shr 11
            min = (time shr 5) and 0x3f
        } else {
            hh = 0
            min = 0
        }
        return LocalDateTime(2000 + yy, mm, dd, hh, min).toInstant(TZ)
    }

    /**
     * 機器種別を取得します
     * <pre>http:// sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
     *
     * @param cType コンソールタイプをセット
     * @return String 機器タイプが文字列で戻ります
     */
    fun getConsoleTypeName(
        cType: Int,
    ): FormattedString =
        when (cType and 0xff) {
            0x03 -> FormattedString(Res.string.felica_terminal_fare_adjustment)
            0x04 -> FormattedString(Res.string.felica_terminal_portable)
            0x05 -> FormattedString(Res.string.felica_terminal_vehicle) // bus
            0x07 -> FormattedString(Res.string.felica_terminal_ticket)
            0x08 -> FormattedString(Res.string.felica_terminal_ticket)
            0x09 -> FormattedString(Res.string.felica_terminal_deposit_quick_charge)
            0x12 -> FormattedString(Res.string.felica_terminal_tvm_tokyo_monorail)
            0x13 -> FormattedString(Res.string.felica_terminal_tvm_etc)
            0x14 -> FormattedString(Res.string.felica_terminal_tvm_etc)
            0x15 -> FormattedString(Res.string.felica_terminal_tvm_etc)
            0x16 -> FormattedString(Res.string.felica_terminal_turnstile)
            0x17 -> FormattedString(Res.string.felica_terminal_ticket_validator)
            0x18 -> FormattedString(Res.string.felica_terminal_ticket_booth)
            0x19 -> FormattedString(Res.string.felica_terminal_ticket_office_green)
            0x1a -> FormattedString(Res.string.felica_terminal_ticket_gate_terminal)
            0x1b -> FormattedString(Res.string.felica_terminal_mobile_phone)
            0x1c -> FormattedString(Res.string.felica_terminal_connection_adjustment)
            0x1d -> FormattedString(Res.string.felica_terminal_transfer_adjustment)
            0x1f -> FormattedString(Res.string.felica_terminal_simple_deposit)
            0x46 -> FormattedString(Res.string.felica_terminal_view_altte)
            0x48 -> FormattedString(Res.string.felica_terminal_view_altte)
            0xc7 -> FormattedString(Res.string.felica_terminal_pos) // sales
            0xc8 -> FormattedString(Res.string.felica_terminal_vending) // sales
            else -> FormattedString(Res.string.suica_unknown_console, cType.toString(16))
        }

    /**
     * 処理種別を取得します
     * <pre>http:// sourceforge.jp/projects/felicalib/wiki/suicaを参考にしています</pre>
     *
     * @param proc 処理タイプをセット
     * @return String 処理タイプが文字列で戻ります
     */
    fun getProcessTypeName(
        proc: Int,
    ): FormattedString =
        when (proc and 0xff) {
            0x01 -> FormattedString(Res.string.felica_process_fare_exit_gate)
            0x02 -> FormattedString(Res.string.felica_process_charge)
            0x03 -> FormattedString(Res.string.felica_process_purchase_magnetic)
            0x04 -> FormattedString(Res.string.felica_process_fare_adjustment)
            0x05 -> FormattedString(Res.string.felica_process_admission_payment)
            0x06 -> FormattedString(Res.string.felica_process_booth_exit)
            0x07 -> FormattedString(Res.string.felica_process_issue_new)
            0x08 -> FormattedString(Res.string.felica_process_booth_deduction)
            0x0d -> FormattedString(Res.string.felica_process_bus_pitapa) // Bus
            0x0f -> FormattedString(Res.string.felica_process_bus_iruca) // Bus
            0x11 -> FormattedString(Res.string.felica_process_reissue)
            0x13 -> FormattedString(Res.string.felica_process_payment_shinkansen)
            0x14 -> FormattedString(Res.string.felica_process_entry_a_autocharge)
            0x15 -> FormattedString(Res.string.felica_process_exit_a_autocharge)
            0x1f -> FormattedString(Res.string.felica_process_deposit_bus) // Bus
            0x23 -> FormattedString(Res.string.felica_process_purchase_special_ticket) // Bus
            0x46 -> FormattedString(Res.string.felica_process_merchandise_purchase) // Sales
            0x48 -> FormattedString(Res.string.felica_process_bonus_charge)
            0x49 -> FormattedString(Res.string.felica_process_register_deposit) // Sales
            0x4a -> FormattedString(Res.string.felica_process_merchandise_cancel) // Sales
            0x4b -> FormattedString(Res.string.felica_process_merchandise_admission) // Sales
            0xc6 -> FormattedString(Res.string.felica_process_merchandise_purchase_cash) // Sales
            0xcb -> FormattedString(Res.string.felica_process_merchandise_admission_cash) // Sales
            0x84 -> FormattedString(Res.string.felica_process_payment_thirdparty)
            0x85 -> FormattedString(Res.string.felica_process_admission_thirdparty)
            else -> FormattedString(Res.string.suica_unknown_process, proc.toString(16))
        }

    private const val SUICA_BUS_STR = "suica_bus"
    private const val SUICA_RAIL_STR = "suica_rail"

    fun getBusStop(
        regionCode: Int,
        lineCode: Int,
        stationCode: Int,
    ): Station? {
        val lineCodeLow = lineCode and 0xFF
        val stationCodeLow = stationCode and 0xFF
        val stationId = (lineCodeLow shl 8) or stationCodeLow
        if (stationId == 0) return null

        val result = MdstStationLookup.getStation(SUICA_BUS_STR, stationId)
        if (result != null) {
            return Station
                .builder()
                .companyName(result.companyName)
                .stationName(result.stationName)
                .latitude(if (result.hasLocation) result.latitude.toString() else null)
                .longitude(if (result.hasLocation) result.longitude.toString() else null)
                .build()
        }

        // Return unknown station with formatted ID
        return Station.unknown(
            "${NumberUtils.intToHex(
                regionCode,
            )}/${NumberUtils.intToHex(lineCodeLow)}/${NumberUtils.intToHex(stationCodeLow)}",
        )
    }

    fun getRailStation(
        regionCode: Int,
        lineCode: Int,
        stationCode: Int,
    ): Station? {
        val lineCodeLow = lineCode and 0xFF
        val stationCodeLow = stationCode and 0xFF
        val areaCode = regionCode shr 6 and 0xFF
        val stationId = (areaCode shl 16) or (lineCodeLow shl 8) or stationCodeLow
        if (stationId == 0) return null

        val result = MdstStationLookup.getStation(SUICA_RAIL_STR, stationId)
        if (result != null) {
            return Station
                .builder()
                .companyName(result.companyName)
                .lineNames(result.lineNames)
                .stationName(result.stationName)
                .latitude(if (result.hasLocation) result.latitude.toString() else null)
                .longitude(if (result.hasLocation) result.longitude.toString() else null)
                .build()
        }

        // Return unknown station with formatted ID
        return Station.unknown(
            "${NumberUtils.intToHex(
                regionCode,
            )}/${NumberUtils.intToHex(lineCodeLow)}/${NumberUtils.intToHex(stationCodeLow)}",
        )
    }
}
