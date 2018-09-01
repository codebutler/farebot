/*
 * KMTData.java
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

package com.codebutler.farebot.transit.kmt;

import android.support.annotation.Nullable;
import com.codebutler.farebot.transit.Station;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

final class KMTData {
    /**
     * Kereta Commuter Indonesia Station list.
     */
    private static final Map<Integer, Station> KCI_STATIONS = new ImmutableMap.Builder<Integer, Station>()
            .put(0x1, Station.create("Tanah Abang", "Tanah Abang", "-6.18574476", "106.8108382"))
            .put(0x101, Station.create("Bogor", "Bogor", "-6.59561005", "106.7904379"))
            .put(0x102, Station.create("Cilebut", "Cilebut", "-6.53050343", "106.8005885"))
            .put(0x103, Station.create("Bojonggede", "Bojonggede", "-6.49326562", "106.7949173"))
            .put(0x104, Station.create("Citayam", "Citayam", "-6.44879141", "106.8024588"))
            .put(0x105, Station.create("Depok", "Depok", "-6.40493394", "106.8172447"))
            .put(0x106, Station.create("Depok Baru", "Depok Baru", "-6.39113047", "106.821707"))
            .put(0x107, Station.create("Pondok Cina", "Pondok Cina", "-6.36905168", "106.8322114"))
            .put(0x108, Station.create("Univ. Indonesia", "Univ. Indonesia", "-6.36075528", "106.8317544"))
            .put(0x109, Station.create("Univ. Pancasila", "Univ. Pancasila", "-6.33894476", "106.8344241"))
            .put(0x110, Station.create("Lenteng Agung", "Lenteng Agung", "-6.33065157", "106.8349938"))
            .put(0x111, Station.create("Tanjung Barat", "Tanjung Barat", "-6.30780817", "106.8388513"))
            .put(0x112, Station.create("Pasar Minggu", "Pasar Minggu", "-6.28440597", "106.8445384"))
            .put(0x113, Station.create("Pasar Minggu Baru", "Pasar Minggu Baru", "-6.26278132", "106.8518598"))
            .put(0x114, Station.create("Duren Kalibata", "Duren Kalibata", "-6.25534623", "106.8550195"))
            .put(0x115, Station.create("Cawang", "Cawang", "-6.24266069", "106.8588196"))
            .put(0x116, Station.create("Tebet", "Tebet", "-6.22606896", "106.8583004"))
            .put(0x117, Station.create("Manggarai", "Manggarai", "-6.20992352", "106.8502129"))
            .put(0x118, Station.create("Cikini", "Cikini", "-6.19856352", "106.8412599"))
            .put(0x119, Station.create("Gondangdia", "Gondangdia", "-6.18594019", "106.8325942"))
            .put(0x120, Station.create("Juanda", "Juanda", "-6.16672229", "106.8304674"))
            .put(0x121, Station.create("Sawah Besar", "Sawah Besar", "-6.16063965", "106.8276397"))
            .put(0x122, Station.create("Mangga Besar", "Mangga Besar", "-6.14979667", "106.8269796"))
            .put(0x123, Station.create("Jayakarta", "Jayakarta", "-6.14134112", "106.8230834"))
            .put(0x124, Station.create("Jakarta Kota", "Jakarta Kota", "-6.13761335", "106.8146308"))
            .put(0x125, Station.create("Bekasi", "Bekasi", "-6.23614485", "106.9994173"))
            .put(0x126, Station.create("Kranji", "Kranji", "-6.22433352", "106.9793992"))
            .put(0x127, Station.create("Cakung", "Cakung", "-6.21929974", "106.9521357"))
            .put(0x128, Station.create("Klender Baru", "Klender Baru", "-6.21743543", "106.9396893"))
            .put(0x129, Station.create("Buaran", "Buaran", "-6.21615092", "106.9283069"))
            .put(0x130, Station.create("Klender", "Klender", "-6.21335877", "106.8998889"))
            .put(0x131, Station.create("Jatinegara", "Jatinegara", "-6.21513342", "106.8703259"))
            .put(0x139, Station.create("Tangerang", "Tangerang", "-6.17679787", "106.63272688"))
            .put(0x147, Station.create("Karet", "Karet", "-6.2008165", "106.8159002"))
            .put(0x148, Station.create("Sudirman", "Sudirman", "-6.202438", "106.8234505"))
            .put(0x149, Station.create("Tanah Abang", "Tanah Abang", "-6.18574476", "106.8108382"))
            .put(0x150, Station.create("Palmerah", "Palmerah", "-6.20740425", "106.7974463"))
            .put(0x151, Station.create("Kebayoran", "Kebayoran", "-6.23718958", "106.782542"))
            .put(0x152, Station.create("Pondok Ranji", "Pondok Ranji", "-6.27633762", "106.7449376"))
            .put(0x153, Station.create("Jurang Mangu", "Jurang Mangu", "-6.28876225", "106.7291141"))
            .put(0x154, Station.create("Sudimara", "Sudimara", "-6.29694285", "106.7127952"))
            .put(0x155, Station.create("Rawabuntu", "Rawabuntu", "-6.31500105", "106.6761968"))
            .put(0x156, Station.create("Serpong", "Serpong", "-6.32004857", "106.6655717"))
            .put(0x157, Station.create("Cisauk", "Cisauk", "-6.3249995", "106.6407467"))
            .put(0x158, Station.create("Cicayur", "Cicayur", "-6.32951436", "106.6189624"))
            .put(0x159, Station.create("Parung Panjang", "Parung Panjang", "-6.34420808", "106.5698061"))
            .put(0x160, Station.create("Cilejit", "Cilejit", "-6.35434367", "106.5097328"))
            .put(0x161, Station.create("Daru", "Daru", "-6.33800742", "106.4923913"))
            .put(0x162, Station.create("Tenjo", "Tenjo", "-6.32725713", "106.4613542"))
            .put(0x163, Station.create("Tigaraksa", "Tigaraksa", "-6.32846118", "106.4347451"))
            .put(0x164, Station.create("Maja", "Maja", "-6.33230387", "106.3965692"))
            .put(0x165, Station.create("Citeras", "Citeras", "-6.33492764", "106.3327125"))
            .put(0x166, Station.create("Rangkasbitung", "Rangkasbitung", "-6.3526711", "106.251502"))
            .put(0x176, Station.create("Bekasi Timur", "Bekasitimur", "-6.246845", "107.0181248"))
            .put(0x178, Station.create("Cikarang", "Cikarang", "-6.2553926", "107.1451293"))
            .build();

    @Nullable
    static Station getStation(int code) {
        return KCI_STATIONS.get(code);
    }
}
