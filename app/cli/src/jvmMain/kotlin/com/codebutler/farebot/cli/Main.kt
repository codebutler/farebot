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
import com.codebutler.farebot.persist.db.FareBotDb
import com.codebutler.farebot.shared.serialize.FareBotSerializersModule
import com.codebutler.farebot.shared.transit.TransitFactoryRegistry
import com.codebutler.farebot.shared.transit.createTransitFactoryRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
