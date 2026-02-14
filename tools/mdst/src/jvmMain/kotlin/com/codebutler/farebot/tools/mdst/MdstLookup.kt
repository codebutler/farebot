/*
 * MdstLookup.kt
 * Look up a station by ID in an MDST file.
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid lookup.py
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

package com.codebutler.farebot.tools.mdst

import com.codebutler.farebot.base.mdst.MdstStationTableReader
import com.codebutler.farebot.base.mdst.TransportType
import java.io.File
import kotlin.system.exitProcess

object MdstLookup {
    fun run(args: List<String>) {
        if (args.size < 2) {
            System.err.println("Usage: mdst lookup <file.mdst> <station_id>")
            System.err.println("  station_id can be decimal or hex (0x...)")
            exitProcess(1)
        }

        val file = File(args[0])
        val stationId = parseIntArg(args[1])

        println("Looking for station $stationId (0x${stationId.toString(16)})...")

        val data = file.readBytes()
        val reader = MdstStationTableReader.fromByteArray(data)

        println("File version = ${reader.version}, local languages = ${reader.localLanguages}")

        val station = reader.getStationById(stationId)
        if (station == null) {
            System.err.println("Station not found.")
            exitProcess(1)
        }

        println()
        println("Station $stationId (0x${stationId.toString(16)}):")
        println("  Name (English):       ${station.name.english.ifEmpty { "(none)" }}")
        println("  Name (Local):         ${station.name.local.ifEmpty { "(none)" }}")
        println("  Name Short (English): ${station.name.englishShort.ifEmpty { "(none)" }}")
        println("  Name Short (Local):   ${station.name.localShort.ifEmpty { "(none)" }}")
        if (station.latitude != 0f || station.longitude != 0f) {
            println("  Location:             ${station.latitude}, ${station.longitude}")
        }

        if (station.operatorId != 0) {
            val op = reader.getOperator(station.operatorId)
            println("  Operator ID:          ${station.operatorId}")
            if (op != null) {
                println("  Operator (English):   ${op.name.english}")
                println("  Operator (Local):     ${op.name.local.ifEmpty { "(none)" }}")
                val transport = TransportType.entries.getOrNull(op.defaultTransport)
                if (transport != null && transport != TransportType.UNKNOWN) {
                    println("  Operator Transport:   $transport")
                }
            }
        }

        if (station.lineId.isNotEmpty()) {
            for (lineId in station.lineId) {
                val line = reader.getLine(lineId)
                println("  Line ID:              $lineId")
                if (line != null) {
                    println("  Line (English):       ${line.name.english}")
                    println("  Line (Local):         ${line.name.local.ifEmpty { "(none)" }}")
                    val transport = TransportType.entries.getOrNull(line.transport)
                    if (transport != null && transport != TransportType.UNKNOWN) {
                        println("  Line Transport:       $transport")
                    }
                }
            }
        }
    }
}
