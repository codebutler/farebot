/*
 * Main.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2026 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.cli

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.codebutler.farebot.app.keymanager.KeyManagerPluginImpl
import com.codebutler.farebot.base.util.hex
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.classic.ClassicCardReader
import com.codebutler.farebot.card.classic.raw.RawClassicCard
import com.codebutler.farebot.card.classic.raw.RawClassicSector
import com.codebutler.farebot.card.nfc.pn533.PN533
import com.codebutler.farebot.card.nfc.pn533.PN533CardInfo
import com.codebutler.farebot.card.nfc.pn533.PN533ClassicTechnology
import com.codebutler.farebot.card.nfc.pn533.PN533Device
import com.codebutler.farebot.card.nfc.pn533.PN533Exception
import com.codebutler.farebot.keymanager.NestedAttackKeyRecovery
import com.codebutler.farebot.keymanager.crypto1.HardnestedAttack
import com.codebutler.farebot.persist.db.FareBotDb
import com.codebutler.farebot.shared.serialize.FareBotSerializersModule
import com.codebutler.farebot.shared.transit.TransitFactoryRegistry
import com.codebutler.farebot.shared.transit.createTransitFactoryRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Properties

/**
 * Preload the bundled libusb before usb4java initializes.
 *
 * usb4java's libusb4java.dylib links against /opt/local/lib/libusb-1.0.0.dylib.
 * We bundle a copy with its install name patched to match that path. By loading it
 * into the process first, dyld finds the already-loaded image (matched by install
 * name) when processing libusb4java's dependency — no external libusb required.
 */
private fun preloadBundledLibusb() {
    try {
        val stream =
            Thread
                .currentThread()
                .contextClassLoader
                .getResourceAsStream("native/libusb-1.0.0.dylib") ?: return
        val tmpFile = File.createTempFile("libusb-1.0", ".dylib")
        tmpFile.deleteOnExit()
        stream.use { input ->
            java.io.FileOutputStream(tmpFile).use { output ->
                input.copyTo(output)
            }
        }
        System.load(tmpFile.absolutePath)
    } catch (e: Throwable) {
        System.err.println("[cli] Could not preload bundled libusb: ${e.message}")
    }
}

fun main(args: Array<String>) {
    val recover = args.contains("--recover")
    val offline = args.contains("--offline")
    val verify = args.contains("--verify")

    if (offline) {
        runBlocking { offlineRecover() }
        return
    }

    if (verify) {
        preloadBundledLibusb()
        runBlocking { verifyOfflineCandidates() }
        return
    }

    preloadBundledLibusb()

    // Set up DB
    val farebotDir = File(System.getProperty("user.home"), ".farebot").apply { mkdirs() }
    val dbFile = File(farebotDir, "farebot.db")
    val url = "jdbc:sqlite:${dbFile.absolutePath}"
    val driver = JdbcSqliteDriver(url, properties = Properties(), schema = FareBotDb.Schema)
    val db = FareBotDb(driver)

    // Set up JSON + key manager
    val json =
        Json {
            serializersModule = FareBotSerializersModule
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    val keyManager =
        KeyManagerPluginImpl(
            cardKeysPersister =
                com.codebutler.farebot.persist.db
                    .DbCardKeysPersister(db),
            json = json,
        )

    // Set up transit registry
    val transitRegistry = createTransitFactoryRegistry()

    runBlocking {
        readCard(keyManager, transitRegistry, recover)
    }
}

private suspend fun readCard(
    keyManager: KeyManagerPluginImpl,
    transitRegistry: TransitFactoryRegistry,
    recover: Boolean,
) {
    // Open PN533 USB device
    val transport =
        PN533Device.open()
            ?: error("PN533 device not found. Is a USB NFC reader connected?")

    transport.flush()
    val pn533 = PN533(transport)

    try {
        // Initialize device (same as PN533ReaderBackend)
        val fw = pn533.getFirmwareVersion()
        println("[cli] Firmware: $fw")
        pn533.samConfiguration()
        pn533.setMaxRetries(passiveActivation = 0x02)
        pn533.setTimings(retryTimeout = 0x0C)

        // Poll until a card is detected
        println("[cli] Polling for MIFARE Classic card...")
        var target: PN533.TargetInfo.TypeA? = null
        while (target == null) {
            val result = pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_106_ISO14443A)
            if (result is PN533.TargetInfo.TypeA) {
                target = result
            } else {
                if (result != null) {
                    try {
                        pn533.inRelease(result.tg)
                    } catch (_: PN533Exception) {
                    }
                }
                delay(250)
            }
        }

        val tagId = target.uid
        val info = PN533CardInfo.fromTypeA(target)
        println("[cli] Card detected: type=${info.cardType}, SAK=0x%02X, UID=${tagId.hex()}".format(target.sak))

        if (info.cardType != CardType.MifareClassic) {
            println("[cli] Not a MIFARE Classic card (got ${info.cardType}). Exiting.")
            return
        }

        // Read the card
        val tech = PN533ClassicTechnology(pn533, target.tg, tagId, info)
        val tagIdHex = tagId.hex()
        val cardKeys = keyManager.getCardKeysForTag(tagIdHex)
        val globalKeys = keyManager.getGlobalKeys()
        val keyRecovery = if (recover) keyManager.classicKeyRecovery else null

        // Wire up nonce persistence for hardnested offline restart
        if (keyRecovery is NestedAttackKeyRecovery) {
            val noncesDir = File(System.getProperty("user.home"), ".farebot/nonces").apply { mkdirs() }
            val uidHex = tagId.hex()
            keyRecovery.onNoncesCollected = { sectorIndex, data ->
                val file = File(noncesDir, "$uidHex-sector%02d.bin".format(sectorIndex))
                file.writeBytes(data)
                println("[cli] Saved ${data.size} bytes of nonce data to ${file.name}")
            }
        }

        println("[cli] Reading card${if (recover) " (with key recovery)" else ""}...")
        if (cardKeys != null) {
            println("[cli] Loaded saved keys for $tagIdHex")
        }
        if (globalKeys.isNotEmpty()) {
            println("[cli] Loaded ${globalKeys.size} global dictionary keys")
        }

        val rawCard =
            ClassicCardReader.readCard(
                tagId,
                tech,
                cardKeys,
                globalKeys,
                keyRecovery,
                onProgress = { current, total -> print("\r[cli] Reading sector $current/$total...") },
            )
        println() // newline after progress

        // Save discovered keys
        rawCard.extractKeys()?.let { keys ->
            keyManager.saveCardKeys(tagIdHex, keys)
        }

        // Print sector summary
        printSectorSummary(rawCard)

        // Parse and print transit info
        printTransitInfo(rawCard, transitRegistry)

        // Release target
        try {
            pn533.inRelease(target.tg)
        } catch (_: PN533Exception) {
        }
    } finally {
        pn533.close()
    }
}

/**
 * Offline hardnested key recovery from saved nonce files in ~/.farebot/nonces/.
 *
 * Loads each .bin file, runs brute force with full nonce verification,
 * and persists all candidate keys to ~/.farebot/nonces/candidates.json.
 */
private suspend fun offlineRecover() {
    val noncesDir = File(System.getProperty("user.home"), ".farebot/nonces")
    if (!noncesDir.exists() || !noncesDir.isDirectory) {
        println("[offline] No nonces directory found at ${noncesDir.absolutePath}")
        return
    }

    val nonceFiles =
        noncesDir
            .listFiles { f -> f.extension == "bin" }
            ?.sortedBy { it.name }
            ?: emptyList()

    if (nonceFiles.isEmpty()) {
        println("[offline] No .bin nonce files found in ${noncesDir.absolutePath}")
        return
    }

    println("[offline] Found ${nonceFiles.size} nonce file(s):")
    for (f in nonceFiles) {
        val numNonces = (f.length() - 6) / 5
        println("  ${f.name} ($numNonces nonces)")
    }
    println()

    // Collect results for all sectors
    val allResults = mutableMapOf<String, List<String>>() // "sector22_A" -> ["04128FEB91FB", ...]

    for (nonceFile in nonceFiles) {
        println("=== ${nonceFile.name} ===")
        val data = nonceFile.readBytes()

        if (data.size < 6) {
            println("[offline] File too small, skipping")
            println()
            continue
        }

        // Parse UID from file header
        val uid =
            ((data[0].toInt() and 0xFF).toUInt() shl 24) or
                ((data[1].toInt() and 0xFF).toUInt() shl 16) or
                ((data[2].toInt() and 0xFF).toUInt() shl 8) or
                (data[3].toInt() and 0xFF).toUInt()
        val targetBlock = data[4].toInt() and 0xFF
        val targetKeyType = data[5]
        val keyTypeStr = if (targetKeyType == 0x60.toByte()) "A" else "B"
        val sectorIndex = targetBlock / 4
        println(
            "[offline] UID=${"%08X".format(uid.toInt())}, block=$targetBlock (sector $sectorIndex), key=$keyTypeStr",
        )

        val attack = HardnestedAttack.offline(uid)
        val candidates =
            attack.offlineRecover(
                nonceData = data,
                onProgress = { msg -> println("[offline] $msg") },
            )

        val keyStrings = candidates.map { "%012X".format(it) }
        val resultKey = "sector${sectorIndex}_$keyTypeStr"
        allResults[resultKey] = keyStrings

        if (candidates.isNotEmpty()) {
            println("[offline] ${candidates.size} candidate(s) for sector $sectorIndex key $keyTypeStr:")
            for ((i, key) in candidates.withIndex()) {
                println("[offline]   [$i] ${"%012X".format(key)}")
            }
        } else {
            println("[offline] FAILED: Could not recover key")
        }
        println()
    }

    // Print summary table
    println("=== Candidate Key Summary ===")
    println("%-20s  %s".format("Sector/Key", "Candidates"))
    println("-".repeat(60))
    for ((key, candidates) in allResults.toSortedMap()) {
        println("%-20s  %s".format(key, candidates.joinToString(", ")))
    }
    println()

    // Persist to JSON
    val candidatesFile = File(noncesDir, "candidates.json")
    val json = Json { prettyPrint = true }
    candidatesFile.writeText(json.encodeToString(allResults))
    println("[offline] Saved candidates to ${candidatesFile.absolutePath}")
    println("[offline] Run with --verify to test candidates against the card")
}

/**
 * Verify offline candidate keys against a physical card.
 *
 * Reads candidates.json from ~/.farebot/nonces/, connects to the card,
 * and authenticates with each candidate to find the correct key.
 */
private suspend fun verifyOfflineCandidates() {
    val noncesDir = File(System.getProperty("user.home"), ".farebot/nonces")
    val candidatesFile = File(noncesDir, "candidates.json")
    if (!candidatesFile.exists()) {
        println("[verify] No candidates.json found. Run --offline first.")
        return
    }

    val json = Json { ignoreUnknownKeys = true }
    val allCandidates: Map<String, List<String>> =
        json.decodeFromString(candidatesFile.readText())

    if (allCandidates.isEmpty()) {
        println("[verify] No candidates to verify.")
        return
    }

    println("[verify] Loaded candidates for ${allCandidates.size} sector(s)")
    for ((key, candidates) in allCandidates.toSortedMap()) {
        println("  $key: ${candidates.size} candidate(s)")
    }
    println()

    // Open PN533
    val transport =
        PN533Device.open()
            ?: error("PN533 device not found. Is a USB NFC reader connected?")
    transport.flush()
    val pn533 = PN533(transport)

    try {
        val fw = pn533.getFirmwareVersion()
        println("[verify] Firmware: $fw")
        pn533.samConfiguration()
        pn533.setMaxRetries(passiveActivation = 0x02)
        pn533.setTimings(retryTimeout = 0x0C)

        println("[verify] Polling for card...")
        var target: PN533.TargetInfo.TypeA? = null
        while (target == null) {
            val result = pn533.inListPassiveTarget(baudRate = PN533.BAUD_RATE_106_ISO14443A)
            if (result is PN533.TargetInfo.TypeA) {
                target = result
            } else {
                if (result != null) {
                    try {
                        pn533.inRelease(result.tg)
                    } catch (_: PN533Exception) {
                    }
                }
                delay(250)
            }
        }

        println("[verify] Card detected: UID=${target.uid.hex()}")
        val tech =
            PN533ClassicTechnology(
                pn533,
                target.tg,
                target.uid,
                PN533CardInfo.fromTypeA(target),
            )

        println()
        val verified = mutableMapOf<String, String>()
        for ((key, candidates) in allCandidates.toSortedMap()) {
            // Parse "sector22_A" -> sector=22, keyType=A
            val parts = key.removePrefix("sector").split("_")
            val sectorIndex = parts[0].toInt()
            val keyTypeStr = parts[1]

            println("=== $key (sector $sectorIndex, key $keyTypeStr) ===")
            println("  Testing ${candidates.size} candidate(s)...")

            var found: String? = null
            for (candidateHex in candidates) {
                val keyBytes =
                    ByteArray(6) { i ->
                        candidateHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                    }
                val success =
                    if (keyTypeStr == "A") {
                        tech.authenticateSectorWithKeyA(sectorIndex, keyBytes)
                    } else {
                        tech.authenticateSectorWithKeyB(sectorIndex, keyBytes)
                    }
                if (success) {
                    println("  VERIFIED: $candidateHex")
                    found = candidateHex
                    break
                }
            }

            if (found != null) {
                verified[key] = found
            } else {
                println("  FAILED: None of ${candidates.size} candidates verified")
            }
            println()
        }

        // Summary
        println("=== Verified Keys ===")
        for ((key, verifiedKey) in verified.toSortedMap()) {
            println("  $key = $verifiedKey")
        }
        if (verified.size < allCandidates.size) {
            val failed = allCandidates.keys - verified.keys
            println()
            println("  Failed: ${failed.sorted().joinToString(", ")}")
        }

        try {
            pn533.inRelease(target.tg)
        } catch (_: PN533Exception) {
        }
    } finally {
        pn533.close()
    }
}

private fun printSectorSummary(rawCard: RawClassicCard) {
    println()
    println("=== Sector Summary ===")
    for (sector in rawCard.sectors()) {
        val status =
            when (sector.type) {
                RawClassicSector.TYPE_DATA -> "DATA"
                RawClassicSector.TYPE_UNAUTHORIZED -> "UNAUTHORIZED"
                RawClassicSector.TYPE_INVALID -> "INVALID"
                else -> sector.type.uppercase()
            }
        val keyA = sector.keyA?.hex() ?: "------"
        val keyB = sector.keyB?.hex() ?: "------"
        println("  Sector %2d: %-14s  KeyA=%s  KeyB=%s".format(sector.index, status, keyA, keyB))
    }
    val dataCount = rawCard.sectors().count { it.type == RawClassicSector.TYPE_DATA }
    val unauthCount = rawCard.sectors().count { it.type == RawClassicSector.TYPE_UNAUTHORIZED }
    val invalidCount = rawCard.sectors().count { it.type == RawClassicSector.TYPE_INVALID }
    println()
    println(
        "Total: ${rawCard.sectors().size} sectors ($dataCount data, $unauthCount unauthorized, $invalidCount invalid)",
    )
}

private suspend fun printTransitInfo(
    rawCard: RawCard<*>,
    transitRegistry: TransitFactoryRegistry,
) {
    println()
    println("=== Transit Info ===")
    val card = rawCard.parse()

    val identity = transitRegistry.parseTransitIdentity(card)
    if (identity != null) {
        println("  Card: ${identity.name.resolveAsync()}")
        identity.serialNumber?.let { println("  Serial: $it") }
    } else {
        println("  (no transit system identified)")
        return
    }

    val transitInfo = transitRegistry.parseTransitInfo(card)
    if (transitInfo != null) {
        transitInfo.serialNumber?.let { println("  Serial: $it") }
        val balanceStr = transitInfo.formatBalanceString()
        if (balanceStr.isNotEmpty()) {
            println("  Balance: $balanceStr")
        }
        transitInfo.trips?.let { trips ->
            if (trips.isNotEmpty()) {
                println("  Trips: ${trips.size}")
            }
        }
        transitInfo.subscriptions?.let { subs ->
            if (subs.isNotEmpty()) {
                println("  Subscriptions: ${subs.size}")
            }
        }
    }
}
