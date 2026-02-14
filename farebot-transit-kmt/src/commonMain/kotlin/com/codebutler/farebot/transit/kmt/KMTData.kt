/*
 * KMTData.kt
 *
 * Authors:
 * Bondan Sumbodo <sybond@gmail.com>
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
 */

package com.codebutler.farebot.transit.kmt

import com.codebutler.farebot.base.mdst.MdstStationLookup
import com.codebutler.farebot.transit.Station

internal object KMTData {
    private const val KMT_STR = "kmt"

    /**
     * Kereta Commuter Indonesia Station list (fallback).
     */
    private val KCI_STATIONS: Map<Int, Station> =
        mapOf(
            0x1 to Station.create("Tanah Abang", "Tanah Abang", "-6.18574476", "106.8108382"),
            0x101 to Station.create("Bogor", "Bogor", "-6.59561005", "106.7904379"),
            0x102 to Station.create("Cilebut", "Cilebut", "-6.53050343", "106.8005885"),
            0x103 to Station.create("Bojonggede", "Bojonggede", "-6.49326562", "106.7949173"),
            0x104 to Station.create("Citayam", "Citayam", "-6.44879141", "106.8024588"),
            0x105 to Station.create("Depok", "Depok", "-6.40493394", "106.8172447"),
            0x106 to Station.create("Depok Baru", "Depok Baru", "-6.39113047", "106.821707"),
            0x107 to Station.create("Pondok Cina", "Pondok Cina", "-6.36905168", "106.8322114"),
            0x108 to Station.create("Univ. Indonesia", "Univ. Indonesia", "-6.36075528", "106.8317544"),
            0x109 to Station.create("Univ. Pancasila", "Univ. Pancasila", "-6.33894476", "106.8344241"),
            0x110 to Station.create("Lenteng Agung", "Lenteng Agung", "-6.33065157", "106.8349938"),
            0x111 to Station.create("Tanjung Barat", "Tanjung Barat", "-6.30780817", "106.8388513"),
            0x112 to Station.create("Pasar Minggu", "Pasar Minggu", "-6.28440597", "106.8445384"),
            0x113 to Station.create("Pasar Minggu Baru", "Pasar Minggu Baru", "-6.26278132", "106.8518598"),
            0x114 to Station.create("Duren Kalibata", "Duren Kalibata", "-6.25534623", "106.8550195"),
            0x115 to Station.create("Cawang", "Cawang", "-6.24266069", "106.8588196"),
            0x116 to Station.create("Tebet", "Tebet", "-6.22606896", "106.8583004"),
            0x117 to Station.create("Manggarai", "Manggarai", "-6.20992352", "106.8502129"),
            0x118 to Station.create("Cikini", "Cikini", "-6.19856352", "106.8412599"),
            0x119 to Station.create("Gondangdia", "Gondangdia", "-6.18594019", "106.8325942"),
            0x120 to Station.create("Juanda", "Juanda", "-6.16672229", "106.8304674"),
            0x121 to Station.create("Sawah Besar", "Sawah Besar", "-6.16063965", "106.8276397"),
            0x122 to Station.create("Mangga Besar", "Mangga Besar", "-6.14979667", "106.8269796"),
            0x123 to Station.create("Jayakarta", "Jayakarta", "-6.14134112", "106.8230834"),
            0x124 to Station.create("Jakarta Kota", "Jakarta Kota", "-6.13761335", "106.8146308"),
            0x125 to Station.create("Bekasi", "Bekasi", "-6.23614485", "106.9994173"),
            0x126 to Station.create("Kranji", "Kranji", "-6.22433352", "106.9793992"),
            0x127 to Station.create("Cakung", "Cakung", "-6.21929974", "106.9521357"),
            0x128 to Station.create("Klender Baru", "Klender Baru", "-6.21743543", "106.9396893"),
            0x129 to Station.create("Buaran", "Buaran", "-6.21615092", "106.9283069"),
            0x130 to Station.create("Klender", "Klender", "-6.21335877", "106.8998889"),
            0x131 to Station.create("Jatinegara", "Jatinegara", "-6.21513342", "106.8703259"),
            0x139 to Station.create("Tangerang", "Tangerang", "-6.17679787", "106.63272688"),
            0x147 to Station.create("Karet", "Karet", "-6.2008165", "106.8159002"),
            0x148 to Station.create("Sudirman", "Sudirman", "-6.202438", "106.8234505"),
            0x149 to Station.create("Tanah Abang", "Tanah Abang", "-6.18574476", "106.8108382"),
            0x150 to Station.create("Palmerah", "Palmerah", "-6.20740425", "106.7974463"),
            0x151 to Station.create("Kebayoran", "Kebayoran", "-6.23718958", "106.782542"),
            0x152 to Station.create("Pondok Ranji", "Pondok Ranji", "-6.27633762", "106.7449376"),
            0x153 to Station.create("Jurang Mangu", "Jurang Mangu", "-6.28876225", "106.7291141"),
            0x154 to Station.create("Sudimara", "Sudimara", "-6.29694285", "106.7127952"),
            0x155 to Station.create("Rawabuntu", "Rawabuntu", "-6.31500105", "106.6761968"),
            0x156 to Station.create("Serpong", "Serpong", "-6.32004857", "106.6655717"),
            0x157 to Station.create("Cisauk", "Cisauk", "-6.3249995", "106.6407467"),
            0x158 to Station.create("Cicayur", "Cicayur", "-6.32951436", "106.6189624"),
            0x159 to Station.create("Parung Panjang", "Parung Panjang", "-6.34420808", "106.5698061"),
            0x160 to Station.create("Cilejit", "Cilejit", "-6.35434367", "106.5097328"),
            0x161 to Station.create("Daru", "Daru", "-6.33800742", "106.4923913"),
            0x162 to Station.create("Tenjo", "Tenjo", "-6.32725713", "106.4613542"),
            0x163 to Station.create("Tigaraksa", "Tigaraksa", "-6.32846118", "106.4347451"),
            0x164 to Station.create("Maja", "Maja", "-6.33230387", "106.3965692"),
            0x165 to Station.create("Citeras", "Citeras", "-6.33492764", "106.3327125"),
            0x166 to Station.create("Rangkasbitung", "Rangkasbitung", "-6.3526711", "106.251502"),
            0x176 to Station.create("Bekasi Timur", "Bekasitimur", "-6.246845", "107.0181248"),
            0x178 to Station.create("Cikarang", "Cikarang", "-6.2553926", "107.1451293"),
        )

    fun getStation(code: Int): Station? {
        // Try MDST database first
        val result = MdstStationLookup.getStation(KMT_STR, code)
        if (result != null) {
            return Station
                .Builder()
                .stationName(result.stationName)
                .shortStationName(result.shortStationName)
                .companyName(result.companyName)
                .lineNames(result.lineNames)
                .latitude(if (result.hasLocation) result.latitude.toString() else null)
                .longitude(if (result.hasLocation) result.longitude.toString() else null)
                .build()
        }
        // Fallback to hardcoded map
        return KCI_STATIONS[code]
    }
}
