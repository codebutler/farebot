/*
 * SuicaTransitFactory.kt
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid (https://github.com/metrodroid/metrodroid)
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
 * https://github.com/codebutler/farebot/wiki/suica
 */

package com.codebutler.farebot.transit.suica

import com.codebutler.farebot.base.util.NumberUtils
import com.codebutler.farebot.base.util.StringResource
import com.codebutler.farebot.base.util.byteArrayToInt
import com.codebutler.farebot.base.util.byteArrayToIntReversed
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.felica.FeliCaConstants
import com.codebutler.farebot.card.felica.FelicaCard
import com.codebutler.farebot.transit.CardInfo
import com.codebutler.farebot.transit.TransitFactory
import com.codebutler.farebot.transit.TransitIdentity
import com.codebutler.farebot.transit.TransitRegion
import com.codebutler.farebot.transit.Trip
import farebot.farebot_transit_suica.generated.resources.*

class SuicaTransitFactory(
    private val stringResource: StringResource,
) : TransitFactory<FelicaCard, SuicaTransitInfo> {
    override val allCards: List<CardInfo>
        get() = ALL_CARDS

    override fun check(card: FelicaCard): Boolean = card.getSystem(FeliCaConstants.SYSTEMCODE_SUICA) != null

    override fun parseIdentity(card: FelicaCard): TransitIdentity {
        val cardName = detectCardName(card)
        return TransitIdentity.create(cardName, null)
    }

    override fun parseInfo(card: FelicaCard): SuicaTransitInfo {
        val system = card.getSystem(FeliCaConstants.SYSTEMCODE_SUICA)!!
        val service = system.getService(FeliCaConstants.SERVICE_SUICA_HISTORY)!!
        val tapService = system.getService(FeliCaConstants.SERVICE_SUICA_INOUT)

        val matchedTaps = mutableSetOf<Int>()
        val trips = mutableListOf<SuicaTrip>()

        // Read blocks oldest-to-newest to calculate fare.
        val blocks = service.blocks
        var tapBlocks = tapService?.blocks

        for (i in blocks.indices.reversed()) {
            val block = blocks[i]

            val previousBalance =
                if (i + 1 < blocks.size) {
                    blocks[i + 1].data.byteArrayToIntReversed(10, 2)
                } else {
                    -1
                }

            val trip = SuicaTrip.parse(block, previousBalance, stringResource)

            if (trip.startTimestamp == null) {
                continue
            }

            // Tap matching: match tap-off and tap-on from the INOUT service
            if (tapBlocks != null && trip.consoleTypeInt == 0x16) {
                // Pass 1: Match tap-offs (exit gates)
                for ((tapIdx, tapBlock) in tapBlocks.withIndex()) {
                    if (matchedTaps.contains(tapIdx)) continue
                    val tapData = tapBlock.data

                    // Skip tap-ons
                    if (tapData[0].toInt() and 0x80 != 0) {
                        continue
                    }

                    val station = tapData.byteArrayToInt(2, 2)
                    if (station != trip.endStationId) {
                        continue
                    }

                    val dateNum = tapData.byteArrayToInt(6, 2)
                    if (dateNum != trip.dateRaw) {
                        continue
                    }

                    val fare = tapData.byteArrayToIntReversed(10, 2)
                    if (fare != trip.fareRaw) {
                        continue
                    }

                    trip.setEndTime(
                        NumberUtils.convertBCDtoInteger(tapData[8]),
                        NumberUtils.convertBCDtoInteger(tapData[9]),
                    )
                    matchedTaps.add(tapIdx)
                    break
                }

                // Pass 2: Match tap-ons (entry gates)
                for ((tapIdx, tapBlock) in tapBlocks.withIndex()) {
                    if (matchedTaps.contains(tapIdx)) continue
                    val tapData = tapBlock.data

                    // Skip tap-offs
                    if (tapData[0].toInt() and 0x80 == 0) {
                        continue
                    }

                    val station = tapData.byteArrayToInt(2, 2)
                    if (station != trip.startStationId) {
                        continue
                    }

                    val dateNum = tapData.byteArrayToInt(6, 2)
                    if (dateNum != trip.dateRaw) {
                        continue
                    }

                    trip.setStartTime(
                        NumberUtils.convertBCDtoInteger(tapData[8]),
                        NumberUtils.convertBCDtoInteger(tapData[9]),
                    )
                    matchedTaps.add(tapIdx)
                    break
                }

                // Check if we have matched every tap we can, if so, destroy the tap list so we
                // don't peek again.
                if (matchedTaps.size == tapBlocks.size) {
                    tapBlocks = null
                }
            }

            trips.add(trip)
        }

        // Trips are already in descending order (newest first) since we iterated reversed.

        val cardName = detectCardName(card)

        return SuicaTransitInfo(
            serialNumber = null, // FIXME: Find where this is on the card.
            trips = trips.toList<Trip>(),
            subscriptions = null,
            cardName = cardName,
        )
    }

    private fun detectCardName(card: FelicaCard): String {
        val system =
            card.getSystem(FeliCaConstants.SYSTEMCODE_SUICA)
                ?: return stringResource.getString(Res.string.card_name_suica)
        // Use allServiceCodes (includes services without readable data) for card type detection.
        // Fall back to services list for backward compatibility with old serialized cards.
        val serviceCodes =
            system.allServiceCodes.ifEmpty {
                system.services.map { it.serviceCode }.toSet()
            }
        return SuicaUtil.getCardName(stringResource, serviceCodes)
    }

    companion object {
        private val ALL_CARDS =
            listOf(
                CardInfo(
                    nameRes = Res.string.card_name_suica,
                    cardType = CardType.FeliCa,
                    region = TransitRegion.JAPAN,
                    locationRes = Res.string.location_tokyo_japan,
                    imageRes = Res.drawable.suica_card,
                    latitude = 35.6762f,
                    longitude = 139.6503f,
                    brandColor = 0x6CBB5A,
                    credits = listOf("nfc-felica project", "IC SFCard Fan project"),
                    sampleDumpFile = "Suica.nfc",
                ),
                CardInfo(
                    nameRes = Res.string.card_name_pasmo,
                    cardType = CardType.FeliCa,
                    region = TransitRegion.JAPAN,
                    locationRes = Res.string.location_tokyo_japan,
                    imageRes = Res.drawable.pasmo_card,
                    latitude = 35.6762f,
                    longitude = 139.6503f,
                    brandColor = 0xFC848C,
                    credits = listOf("nfc-felica project", "IC SFCard Fan project"),
                    sampleDumpFile = "PASMO.nfc",
                ),
                CardInfo(
                    nameRes = Res.string.card_name_icoca,
                    cardType = CardType.FeliCa,
                    region = TransitRegion.JAPAN,
                    locationRes = Res.string.location_osaka_japan,
                    imageRes = Res.drawable.icoca_card,
                    latitude = 34.6937f,
                    longitude = 135.5023f,
                    brandColor = 0x74C6D3,
                    credits = listOf("nfc-felica project", "IC SFCard Fan project"),
                    sampleDumpFile = "ICOCA.nfc",
                ),
                CardInfo(
                    nameRes = Res.string.card_name_toica,
                    cardType = CardType.FeliCa,
                    region = TransitRegion.JAPAN,
                    locationRes = Res.string.location_nagoya_japan,
                    imageRes = Res.drawable.toica,
                    latitude = 35.1815f,
                    longitude = 136.9066f,
                    brandColor = 0x01C4FE,
                    credits = listOf("nfc-felica project", "IC SFCard Fan project"),
                ),
                CardInfo(
                    nameRes = Res.string.card_name_manaca,
                    cardType = CardType.FeliCa,
                    region = TransitRegion.JAPAN,
                    locationRes = Res.string.location_nagoya_japan,
                    imageRes = Res.drawable.manaca,
                    latitude = 35.1815f,
                    longitude = 136.9066f,
                    brandColor = 0x95B5C6,
                    credits = listOf("nfc-felica project", "IC SFCard Fan project"),
                ),
                CardInfo(
                    nameRes = Res.string.card_name_pitapa,
                    cardType = CardType.FeliCa,
                    region = TransitRegion.JAPAN,
                    locationRes = Res.string.location_osaka_japan,
                    imageRes = Res.drawable.pitapa,
                    latitude = 34.6937f,
                    longitude = 135.5023f,
                    brandColor = 0x898CB0,
                    credits = listOf("nfc-felica project", "IC SFCard Fan project"),
                ),
                CardInfo(
                    nameRes = Res.string.card_name_kitaca,
                    cardType = CardType.FeliCa,
                    region = TransitRegion.JAPAN,
                    locationRes = Res.string.location_sapporo_japan,
                    imageRes = Res.drawable.kitaca,
                    latitude = 43.0618f,
                    longitude = 141.3545f,
                    brandColor = 0xE5F5BA,
                    credits = listOf("nfc-felica project", "IC SFCard Fan project"),
                ),
                CardInfo(
                    nameRes = Res.string.card_name_sugoca,
                    cardType = CardType.FeliCa,
                    region = TransitRegion.JAPAN,
                    locationRes = Res.string.location_fukuoka_japan,
                    imageRes = Res.drawable.sugoca,
                    latitude = 33.5904f,
                    longitude = 130.4017f,
                    brandColor = 0xE86696,
                    credits = listOf("nfc-felica project", "IC SFCard Fan project"),
                ),
                CardInfo(
                    nameRes = Res.string.card_name_nimoca,
                    cardType = CardType.FeliCa,
                    region = TransitRegion.JAPAN,
                    locationRes = Res.string.location_fukuoka_japan,
                    imageRes = Res.drawable.nimoca,
                    latitude = 33.5904f,
                    longitude = 130.4017f,
                    brandColor = 0xE9D0A1,
                    credits = listOf("nfc-felica project", "IC SFCard Fan project"),
                ),
                CardInfo(
                    nameRes = Res.string.card_name_hayakaken,
                    cardType = CardType.FeliCa,
                    region = TransitRegion.JAPAN,
                    locationRes = Res.string.location_fukuoka_japan,
                    imageRes = Res.drawable.hayakaken,
                    latitude = 33.5904f,
                    longitude = 130.4017f,
                    brandColor = 0xA0D8EE,
                    credits = listOf("nfc-felica project", "IC SFCard Fan project"),
                ),
            )
    }
}
