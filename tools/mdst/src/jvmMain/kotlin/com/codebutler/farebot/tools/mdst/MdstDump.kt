/*
 * MdstDump.kt
 * Dump all stations from an MDST file to CSV.
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid dump2csv.py
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
import java.io.PrintWriter
import kotlin.system.exitProcess

object MdstDump {
    private val FIELD_NAMES =
        listOf(
            "id",
            "name_en",
            "name",
            "name_short_en",
            "lat",
            "lon",
            "oper_id",
            "oper_en",
            "oper",
            "line_id",
            "line_en",
            "line",
        )

    fun run(args: List<String>) {
        if (args.isEmpty()) {
            System.err.println("Usage: mdst dump <file.mdst> [-o output.csv] [-p operators.csv] [-r lines.csv]")
            exitProcess(1)
        }

        val file = File(args[0])
        val outputFile = parseOptionalArg(args, "-o")
        val operatorsFile = parseOptionalArg(args, "-p")
        val linesFile = parseOptionalArg(args, "-r")

        val data = file.readBytes()
        val reader = MdstStationTableReader.fromByteArray(data)

        println(
            "File version = ${reader.version}, local languages = ${reader.localLanguages}, tts_hint_language = ${reader.ttsHintLanguage}",
        )

        val notice = reader.notice
        if (notice != null) {
            println("== START OF LICENSE NOTICE (${notice.length} bytes) ==")
            println(notice)
            println("== END OF LICENSE NOTICE ==")
        }

        val writer =
            if (outputFile != null) {
                PrintWriter(File(outputFile).bufferedWriter())
            } else {
                PrintWriter(System.out, true)
            }

        writer.println(FIELD_NAMES.joinToString(","))

        val stations = reader.getAllStations()
        for ((id, station) in stations) {
            val fields = mutableMapOf<String, String>()
            fields["id"] = id.toString()
            if (station.name.english.isNotEmpty()) {
                fields["name_en"] = station.name.english
            }
            if (station.name.local.isNotEmpty()) {
                fields["name"] = station.name.local
            }
            if (station.name.englishShort.isNotEmpty()) {
                fields["name_short_en"] = station.name.englishShort
            }
            if (station.latitude != 0f && station.longitude != 0f) {
                fields["lat"] = "%.6f".format(station.latitude)
                fields["lon"] = "%.6f".format(station.longitude)
            }
            if (station.operatorId != 0) {
                fields["oper_id"] = station.operatorId.toString()
                val oper = reader.getOperator(station.operatorId)
                if (oper != null) {
                    fields["oper_en"] = oper.name.english
                    fields["oper"] = oper.name.local
                }
            }
            if (station.lineId.isNotEmpty()) {
                fields["line_id"] = station.lineId.joinToString(",")
                fields["line_en"] =
                    station.lineId.joinToString(",") { lineId ->
                        reader.getLine(lineId)?.name?.english ?: ""
                    }
                fields["line"] =
                    station.lineId.joinToString(",") { lineId ->
                        reader.getLine(lineId)?.name?.local ?: ""
                    }
            }

            writer.println(
                FIELD_NAMES.joinToString(",") { field ->
                    csvEscape(fields[field] ?: "")
                },
            )
        }

        if (outputFile != null) {
            writer.close()
            println("Wrote ${stations.size} stations to $outputFile")
        }

        if (operatorsFile != null) {
            dumpOperators(reader, operatorsFile)
        }
        if (linesFile != null) {
            dumpLines(reader, linesFile)
        }
    }

    private fun dumpOperators(
        reader: MdstStationTableReader,
        outputFile: String,
    ) {
        val pw = PrintWriter(File(outputFile).bufferedWriter())
        pw.println("id,name,short_name,local_name,local_short_name,mode")
        for ((id, op) in reader.operators.toSortedMap()) {
            val mode = TransportType.entries.getOrNull(op.defaultTransport)
            val modeName = if (mode != null && mode != TransportType.UNKNOWN) mode.name else ""
            pw.println(
                listOf(
                    id.toString(),
                    csvEscape(op.name.english),
                    csvEscape(op.name.englishShort),
                    csvEscape(op.name.local),
                    csvEscape(op.name.localShort),
                    modeName,
                ).joinToString(","),
            )
        }
        pw.close()
        println("Wrote ${reader.operators.size} operators to $outputFile")
    }

    private fun dumpLines(
        reader: MdstStationTableReader,
        outputFile: String,
    ) {
        val pw = PrintWriter(File(outputFile).bufferedWriter())
        pw.println("id,name,short_name,local_name,mode")
        for ((id, line) in reader.lines.toSortedMap()) {
            val mode = TransportType.entries.getOrNull(line.transport)
            val modeName = if (mode != null && mode != TransportType.UNKNOWN) mode.name else ""
            pw.println(
                listOf(
                    id.toString(),
                    csvEscape(line.name.english),
                    csvEscape(line.name.englishShort),
                    csvEscape(line.name.local),
                    modeName,
                ).joinToString(","),
            )
        }
        pw.close()
        println("Wrote ${reader.lines.size} lines to $outputFile")
    }

    private fun parseOptionalArg(
        args: List<String>,
        flag: String,
    ): String? {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1] else null
    }
}
