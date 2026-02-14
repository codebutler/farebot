/*
 * CsvParsing.kt
 * Simple CSV parser and utilities for MDST tools.
 *
 * Copyright 2025 Eric Butler <eric@codebutler.com>
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

import java.io.File

fun parseCsv(file: File): List<Map<String, String>> {
    val lines = file.readLines(Charsets.UTF_8)
    if (lines.isEmpty()) return emptyList()

    // Strip BOM if present
    val headerLine = lines[0].removePrefix("\uFEFF")
    val headers = parseCsvLine(headerLine)

    return lines.drop(1).filter { it.isNotBlank() }.map { line ->
        val values = parseCsvLine(line)
        headers.zip(values).toMap()
    }
}

fun parseCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        if (inQuotes) {
            if (c == '"') {
                if (i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i += 2
                    continue
                } else {
                    inQuotes = false
                    i++
                    continue
                }
            } else {
                current.append(c)
            }
        } else {
            when (c) {
                ',' -> {
                    fields.add(current.toString())
                    current.clear()
                }
                '"' -> inQuotes = true
                else -> current.append(c)
            }
        }
        i++
    }
    fields.add(current.toString())
    return fields
}

fun csvEscape(value: String): String =
    if (value.contains(',') || value.contains('"') || value.contains('\n')) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }

fun parseIntArg(s: String): Int =
    if (s.startsWith("0x") || s.startsWith("0X")) {
        s.substring(2).toInt(16)
    } else {
        s.toInt()
    }
