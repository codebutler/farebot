/*
 * MdstData.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
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

package com.codebutler.farebot.base.mdst

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoPacked

/**
 * @Serializable data classes matching the MdST stations.proto schema.
 * Uses kotlinx-serialization-protobuf for binary protobuf parsing.
 */

enum class TransportType {
    UNKNOWN,
    BUS,
    TRAIN,
    TRAM,
    METRO,
    FERRY,
    TICKET_MACHINE,
    VENDING_MACHINE,
    POS,
    OTHER,
    BANNED,
    TROLLEYBUS,
    TOLL_ROAD,
    MONORAIL,
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Names(
    @ProtoNumber(1) val english: String = "",
    @ProtoNumber(2) val local: String = "",
    @ProtoNumber(3) val englishShort: String = "",
    @ProtoNumber(4) val localShort: String = "",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Operator(
    @ProtoNumber(3) val name: Names = Names(),
    @ProtoNumber(4) val defaultTransport: Int = 0,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Line(
    @ProtoNumber(3) val name: Names = Names(),
    @ProtoNumber(4) val transport: Int = 0,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MdstStation(
    @ProtoNumber(1) val id: Int = 0,
    @ProtoNumber(8) val name: Names = Names(),
    @ProtoNumber(4) val latitude: Float = 0f,
    @ProtoNumber(5) val longitude: Float = 0f,
    @ProtoNumber(6) val operatorId: Int = 0,
    @ProtoNumber(7) @ProtoPacked val lineId: List<Int> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StationDb(
    @ProtoNumber(1) val version: Long = 0,
    @ProtoNumber(2) val localLanguages: List<String> = emptyList(),
    @ProtoNumber(3) val operators: Map<Int, Operator> = emptyMap(),
    @ProtoNumber(4) val lines: Map<Int, Line> = emptyMap(),
    @ProtoNumber(5) val ttsHintLanguage: String = "",
    @ProtoNumber(6) val licenseNotice: String = "",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class StationIndex(
    @ProtoNumber(1) val stationMap: Map<Int, Int> = emptyMap(),
)
