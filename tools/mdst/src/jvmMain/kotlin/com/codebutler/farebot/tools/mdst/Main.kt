/*
 * Main.kt
 * CLI entry point for MDST tools.
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

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        exitProcess(1)
    }

    when (args[0]) {
        "lookup" -> MdstLookup.run(args.drop(1))
        "dump" -> MdstDump.run(args.drop(1))
        "compile" -> MdstCompile.run(args.drop(1))
        else -> {
            System.err.println("Unknown command: ${args[0]}")
            printUsage()
            exitProcess(1)
        }
    }
}

private fun printUsage() {
    System.err.println(
        """
        |Usage: mdst <command> [args]
        |
        |Commands:
        |  lookup  <file.mdst> <station_id>      Look up a station by ID
        |  dump    <file.mdst> [-o output.csv]    Dump all stations to CSV
        |  compile <input.csv> -o <output.mdst>   Compile CSV data into MDST binary
        |          [-p operators.csv] [-r lines.csv] [-V version]
        |          [-l tts-hint-language] [-L local-languages] [-n notice.txt]
        """.trimMargin(),
    )
}
