/*
 * MdstCompile.kt
 * Compile CSV data into MDST binary format.
 *
 * Copyright 2015-2019 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018 Google
 * Copyright 2025 Eric Butler <eric@codebutler.com>
 *
 * Ported from Metrodroid csv2pb.py + mdst.py
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

import com.codebutler.farebot.base.mdst.Line
import com.codebutler.farebot.base.mdst.MdstStation
import com.codebutler.farebot.base.mdst.MdstWriter
import com.codebutler.farebot.base.mdst.Names
import com.codebutler.farebot.base.mdst.Operator
import com.codebutler.farebot.base.mdst.TransportType
import java.io.File
import kotlin.system.exitProcess

object MdstCompile {
    fun run(args: List<String>) {
        if (args.isEmpty()) {
            System.err.println(
                """
                |Usage: mdst compile <input.csv> -o <output.mdst>
                |  [-p operators.csv] [-r lines.csv] [-V version]
                |  [-l tts-hint-language] [-L local-languages] [-n notice.txt]
                """.trimMargin(),
            )
            exitProcess(1)
        }

        val inputFile = File(args[0])
        val outputFile = requireArg(args, "-o", "output file")
        val operatorsFile = optionalArg(args, "-p")
        val linesFile = optionalArg(args, "-r")
        val version = optionalArg(args, "-V")?.toLong() ?: 1L
        val ttsHintLanguage = optionalArg(args, "-l") ?: ""
        val localLanguages = optionalArg(args, "-L")?.split(",") ?: emptyList()
        val noticeFile = optionalArg(args, "-n")

        val operators =
            if (operatorsFile != null) {
                readOperatorsFromCsv(File(operatorsFile))
            } else {
                emptyMap()
            }

        val lines =
            if (linesFile != null) {
                readLinesFromCsv(File(linesFile))
            } else {
                emptyMap()
            }

        val licenseNotice =
            if (noticeFile != null) {
                File(noticeFile).readText().trim()
            } else {
                ""
            }

        val writer =
            MdstWriter(
                version = version,
                operators = operators,
                lines = lines,
                localLanguages = localLanguages,
                ttsHintLanguage = ttsHintLanguage,
                licenseNotice = licenseNotice,
            )

        readStopsFromCsv(inputFile, writer)

        val bytes = writer.toByteArray()
        File(outputFile).writeBytes(bytes)

        println("Finished writing database (${bytes.size} bytes).")
    }

    private fun readStopsFromCsv(
        file: File,
        writer: MdstWriter,
    ) {
        val rows = parseCsv(file)
        for (row in rows) {
            // Support both csv2pb.py format (reader_id, stop_name, ...) and
            // dump2csv.py format (id, name_en, ...)
            val readerId = row["reader_id"] ?: row["id"] ?: continue
            val id = parseIntArg(readerId)
            val stopName = row["stop_name"] ?: row["name_en"] ?: ""
            val localName = row["local_name"] ?: row["name"] ?: ""
            val shortName = row["short_name"] ?: row["name_short_en"] ?: ""
            val operatorId =
                (row["operator_id"] ?: row["oper_id"])
                    ?.takeIf { it.isNotEmpty() }
                    ?.toInt() ?: 0
            val lineIdStr = row["line_id"] ?: ""
            val lineIds =
                if (lineIdStr.isNotEmpty()) {
                    lineIdStr.split(",").map { it.trim().toInt() }
                } else {
                    emptyList()
                }
            val lat =
                (row["stop_lat"] ?: row["lat"])
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.toFloat() ?: 0f
            val lon =
                (row["stop_lon"] ?: row["lon"])
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.toFloat() ?: 0f

            val station =
                MdstStation(
                    id = id,
                    name =
                        Names(
                            english = stopName,
                            local = localName,
                            englishShort = shortName,
                        ),
                    latitude = lat,
                    longitude = lon,
                    operatorId = operatorId,
                    lineId = lineIds,
                )

            writer.pushStation(station)
        }
    }

    private fun readOperatorsFromCsv(file: File): Map<Int, Operator> {
        val operators = mutableMapOf<Int, Operator>()
        val rows = parseCsv(file)
        for (row in rows) {
            val id = parseIntArg(row["id"] ?: continue)
            val name = row["name"] ?: ""
            val shortName = row["short_name"] ?: ""
            val localName = row["local_name"] ?: ""
            val localShortName = row["local_short_name"] ?: ""
            val mode = row["mode"]?.takeIf { it.isNotEmpty() }
            val transport =
                if (mode != null) {
                    TransportType.entries.firstOrNull { it.name == mode }?.ordinal ?: 0
                } else {
                    0
                }

            operators[id] =
                Operator(
                    name =
                        Names(
                            english = name,
                            englishShort = shortName,
                            local = localName,
                            localShort = localShortName,
                        ),
                    defaultTransport = transport,
                )
        }
        return operators
    }

    private fun readLinesFromCsv(file: File): Map<Int, Line> {
        val lines = mutableMapOf<Int, Line>()
        val rows = parseCsv(file)
        for (row in rows) {
            val id = parseIntArg(row["id"] ?: continue)
            val name = row["name"] ?: ""
            val shortName = row["short_name"] ?: ""
            val localName = row["local_name"] ?: ""
            val mode = row["mode"]?.takeIf { it.isNotEmpty() }
            val transport =
                if (mode != null) {
                    TransportType.entries.firstOrNull { it.name == mode }?.ordinal ?: 0
                } else {
                    0
                }

            lines[id] =
                Line(
                    name =
                        Names(
                            english = name,
                            englishShort = shortName,
                            local = localName,
                        ),
                    transport = transport,
                )
        }
        return lines
    }

    private fun requireArg(
        args: List<String>,
        flag: String,
        name: String,
    ): String {
        val idx = args.indexOf(flag)
        if (idx < 0 || idx + 1 >= args.size) {
            System.err.println("Missing required argument: $flag <$name>")
            exitProcess(1)
        }
        return args[idx + 1]
    }

    private fun optionalArg(
        args: List<String>,
        flag: String,
    ): String? {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1] else null
    }
}
