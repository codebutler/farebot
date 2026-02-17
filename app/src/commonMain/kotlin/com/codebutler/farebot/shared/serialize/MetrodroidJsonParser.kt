/*
 * MetrodroidJsonParser.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.shared.serialize

import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.card.RawCard
import com.codebutler.farebot.card.classic.raw.RawClassicBlock
import com.codebutler.farebot.card.classic.raw.RawClassicCard
import com.codebutler.farebot.card.classic.raw.RawClassicSector
import com.codebutler.farebot.card.desfire.raw.RawDesfireApplication
import com.codebutler.farebot.card.desfire.raw.RawDesfireCard
import com.codebutler.farebot.card.desfire.raw.RawDesfireFile
import com.codebutler.farebot.card.desfire.raw.RawDesfireFileSettings
import com.codebutler.farebot.card.desfire.raw.RawDesfireManufacturingData
import com.codebutler.farebot.card.felica.FeliCaIdm
import com.codebutler.farebot.card.felica.FeliCaPmm
import com.codebutler.farebot.card.felica.FelicaBlock
import com.codebutler.farebot.card.felica.FelicaService
import com.codebutler.farebot.card.felica.FelicaSystem
import com.codebutler.farebot.card.felica.raw.RawFelicaCard
import com.codebutler.farebot.card.iso7816.ISO7816Application
import com.codebutler.farebot.card.iso7816.ISO7816File
import com.codebutler.farebot.card.iso7816.raw.RawISO7816Card
import com.codebutler.farebot.card.ultralight.UltralightPage
import com.codebutler.farebot.card.ultralight.raw.RawUltralightCard
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.Instant

/**
 * Parses Metrodroid JSON card dumps into FareBot RawCard objects.
 *
 * Metrodroid uses a different JSON schema than FareBot. This parser handles the
 * structural differences by directly constructing RawCard objects from the
 * Metrodroid JSON tree, similar to how FlipperNfcParser handles Flipper NFC dumps.
 */
object MetrodroidJsonParser {
    fun parse(obj: JsonObject): RawCard<*>? {
        val tagId = parseTagId(obj)
        val scannedAt = parseScannedAt(obj)

        return when {
            obj.containsKey("mifareDesfire") ->
                parseDesfire(obj["mifareDesfire"]!!.jsonObject, tagId, scannedAt)
            obj.containsKey("mifareUltralight") ->
                parseUltralight(obj["mifareUltralight"]!!.jsonObject, tagId, scannedAt)
            obj.containsKey("mifareClassic") ->
                parseClassic(obj["mifareClassic"]!!.jsonObject, tagId, scannedAt)
            obj.containsKey("iso7816") ->
                parseISO7816(obj["iso7816"]!!.jsonObject, tagId, scannedAt)
            obj.containsKey("felica") ->
                parseFelica(obj["felica"]!!.jsonObject, tagId, scannedAt)
            else -> null
        }
    }

    // --- Common parsing ---

    private fun parseTagId(obj: JsonObject): ByteArray {
        val hex = obj["tagId"]?.jsonPrimitive?.content ?: "00000000"
        return hexToBytes(hex)
    }

    private fun parseScannedAt(obj: JsonObject): Instant {
        val scannedAtObj = obj["scannedAt"]?.jsonObject ?: return Instant.fromEpochMilliseconds(0)
        val timeInMillis =
            scannedAtObj["timeInMillis"]?.jsonPrimitive?.longOrNull
                ?: return Instant.fromEpochMilliseconds(0)
        return Instant.fromEpochMilliseconds(timeInMillis)
    }

    // --- DESFire ---

    private fun parseDesfire(
        desfire: JsonObject,
        tagId: ByteArray,
        scannedAt: Instant,
    ): RawDesfireCard {
        val manufDataHex = desfire["manufacturingData"]?.jsonPrimitive?.content ?: ""
        val manufData =
            RawDesfireManufacturingData.create(
                if (manufDataHex.isNotEmpty()) hexToBytes(manufDataHex) else ByteArray(28),
            )

        val appsObj = desfire["applications"]?.jsonObject ?: JsonObject(emptyMap())
        val applications =
            appsObj.entries.map { (appIdStr, appElement) ->
                val appId = appIdStr.toIntOrNull() ?: appIdStr.toIntOrNull(16) ?: 0
                parseDesfireApplication(appId, appElement.jsonObject)
            }

        return RawDesfireCard.create(tagId, scannedAt, applications, manufData)
    }

    private fun parseDesfireApplication(
        appId: Int,
        appObj: JsonObject,
    ): RawDesfireApplication {
        val filesObj = appObj["files"]?.jsonObject ?: JsonObject(emptyMap())
        val files =
            filesObj.entries.map { (fileIdStr, fileElement) ->
                val fileId = fileIdStr.toIntOrNull() ?: fileIdStr.toIntOrNull(16) ?: 0
                parseDesfireFile(fileId, fileElement.jsonObject)
            }
        return RawDesfireApplication.create(appId, files)
    }

    private fun parseDesfireFile(
        fileId: Int,
        fileObj: JsonObject,
    ): RawDesfireFile {
        val settingsHex = fileObj["settings"]?.jsonPrimitive?.content ?: ""
        val dataHex = fileObj["data"]?.jsonPrimitive?.content

        val settings =
            RawDesfireFileSettings.create(
                if (settingsHex.isNotEmpty()) hexToBytes(settingsHex) else byteArrayOf(0, 0, 0, 0, 0, 0, 0),
            )

        return if (dataHex != null && dataHex.isNotEmpty()) {
            RawDesfireFile.create(fileId, settings, hexToBytes(dataHex))
        } else {
            // File with settings but no data (e.g., unauthorized or empty)
            RawDesfireFile.create(fileId, settings, ByteArray(0))
        }
    }

    // --- Ultralight ---

    private fun parseUltralight(
        ul: JsonObject,
        tagId: ByteArray,
        scannedAt: Instant,
    ): RawUltralightCard {
        val pagesArray = ul["pages"]?.jsonArray ?: JsonArray(emptyList())
        val pages =
            pagesArray.mapIndexed { index, pageElement ->
                val pageObj = pageElement.jsonObject
                val dataHex = pageObj["data"]?.jsonPrimitive?.content ?: ""
                val data = if (dataHex.isNotEmpty()) hexToBytes(dataHex) else ByteArray(4)
                UltralightPage.create(index, data)
            }

        val cardModel = ul["cardModel"]?.jsonPrimitive?.content
        val ultralightType = mapUltralightType(cardModel)

        return RawUltralightCard.create(tagId, scannedAt, pages, ultralightType)
    }

    private fun mapUltralightType(model: String?): Int =
        when (model) {
            "EV1_MF0UL11" -> 0
            "EV1_MF0UL21" -> 0
            "NTAG213" -> 2
            "NTAG215" -> 4
            "NTAG216" -> 6
            else -> 0
        }

    // --- Classic ---

    private fun parseClassic(
        classic: JsonObject,
        tagId: ByteArray,
        scannedAt: Instant,
    ): RawClassicCard {
        val sectorsArray = classic["sectors"]?.jsonArray ?: JsonArray(emptyList())
        val sectors =
            sectorsArray.mapIndexed { index, sectorElement ->
                val sectorObj = sectorElement.jsonObject
                val type = sectorObj["type"]?.jsonPrimitive?.content

                if (type == "unauthorized" || type == "keyA" || type == "unknown") {
                    RawClassicSector.createUnauthorized(index)
                } else {
                    val blocksArray = sectorObj["blocks"]?.jsonArray ?: JsonArray(emptyList())
                    val blocks =
                        blocksArray.mapIndexed { blockIndex, blockElement ->
                            val blockObj = blockElement.jsonObject
                            val dataHex = blockObj["data"]?.jsonPrimitive?.content ?: ""
                            val data = if (dataHex.isNotEmpty()) hexToBytes(dataHex) else ByteArray(16)
                            RawClassicBlock.create(blockIndex, data)
                        }
                    RawClassicSector.createData(index, blocks)
                }
            }
        return RawClassicCard.create(tagId, scannedAt, sectors)
    }

    // --- ISO 7816 ---

    private fun parseISO7816(
        iso: JsonObject,
        tagId: ByteArray,
        scannedAt: Instant,
    ): RawISO7816Card {
        val appsArray = iso["applications"]?.jsonArray ?: JsonArray(emptyList())
        val applications =
            appsArray.mapNotNull { appElement ->
                parseISO7816Application(appElement.jsonArray)
            }
        return RawISO7816Card.create(tagId, scannedAt, applications)
    }

    /**
     * Parses an ISO7816 application from Metrodroid format.
     * Metrodroid uses [type, data] array pairs for applications.
     */
    private fun parseISO7816Application(appArray: JsonArray): ISO7816Application? {
        if (appArray.size < 2) return null
        val type = appArray[0].jsonPrimitive.content
        val appData = appArray[1].jsonObject

        val generic = appData["generic"]?.jsonObject ?: return null

        // Parse app name and FCI
        val appNameHex = generic["appName"]?.jsonPrimitive?.content
        val appName = if (!appNameHex.isNullOrEmpty()) hexToBytes(appNameHex) else null

        val appFciHex = generic["appFci"]?.jsonPrimitive?.content
        val appFci = if (!appFciHex.isNullOrEmpty()) hexToBytes(appFciHex) else null

        // Parse files
        val filesObj = generic["files"]?.jsonObject ?: JsonObject(emptyMap())
        val files = mutableMapOf<String, ISO7816File>()
        val sfiFiles = mutableMapOf<Int, ISO7816File>()

        for ((fileKey, fileElement) in filesObj.entries) {
            val fileObj = fileElement.jsonObject
            val file = parseISO7816File(fileObj)

            // Store with original key in files map
            files[fileKey] = file

            // Try to determine SFI from the file key or FCI
            val sfi = extractSfiFromKey(fileKey) ?: extractSfiFromFci(fileObj)
            if (sfi != null) {
                sfiFiles[sfi] = file
            }
        }

        // Handle balance field — TMoney stores balance as hex under "balance" key
        val balanceHex = appData["balance"]?.jsonPrimitive?.content
        if (!balanceHex.isNullOrEmpty()) {
            val balanceFile = ISO7816File.create(binaryData = hexToBytes(balanceHex))
            files["balance/0"] = balanceFile
        }

        return ISO7816Application.create(
            appName = appName,
            appFci = appFci,
            files = files,
            sfiFiles = sfiFiles,
            type = type,
        )
    }

    /**
     * Extracts SFI from a Metrodroid file key like "#appname:N" where N is the SFI.
     * Only applies to "#"-prefixed keys (e.g., "#d4100000030001:4").
     * File-selector keys like ":2000:2001" should not use this — they get SFI from FCI.
     */
    private fun extractSfiFromKey(key: String): Int? {
        if (!key.startsWith("#")) return null
        if (!key.contains(":")) return null
        val afterColon = key.substringAfterLast(":")
        return afterColon.toIntOrNull()
    }

    /**
     * Extracts SFI from Calypso FCI data.
     * In Calypso cards, the FCI proprietary template (tag 85) contains the SFI at byte 2.
     */
    private fun extractSfiFromFci(fileObj: JsonObject): Int? {
        val fciHex = fileObj["fci"]?.jsonPrimitive?.content
        if (fciHex.isNullOrEmpty() || fciHex.length < 6) return null
        val fciBytes = hexToBytes(fciHex)
        // Calypso FCI format: tag(85) + length + SFI + ...
        if (fciBytes.size >= 3 && fciBytes[0] == 0x85.toByte()) {
            return fciBytes[2].toInt() and 0xFF
        }
        return null
    }

    private fun parseISO7816File(fileObj: JsonObject): ISO7816File {
        // Binary data
        val binaryHex = fileObj["binaryData"]?.jsonPrimitive?.content
        val binaryData = if (!binaryHex.isNullOrEmpty()) hexToBytes(binaryHex) else null

        // FCI
        val fciHex = fileObj["fci"]?.jsonPrimitive?.content
        val fci = if (!fciHex.isNullOrEmpty()) hexToBytes(fciHex) else null

        // Records
        val recordsObj = fileObj["records"]?.jsonObject
        val records = mutableMapOf<Int, ByteArray>()
        if (recordsObj != null) {
            for ((recordIdStr, recordElement) in recordsObj.entries) {
                val recordId = recordIdStr.toIntOrNull() ?: continue
                val recordHex = recordElement.jsonPrimitive.content
                if (recordHex.isNotEmpty()) {
                    records[recordId] = hexToBytes(recordHex)
                }
            }
        }

        return ISO7816File.create(
            binaryData = binaryData,
            records = records,
            fci = fci,
        )
    }

    // --- FeliCa ---

    private fun parseFelica(
        felica: JsonObject,
        tagId: ByteArray,
        scannedAt: Instant,
    ): RawFelicaCard {
        val idmHex = felica["iDm"]?.jsonPrimitive?.content ?: ""
        val pmmHex = felica["pMm"]?.jsonPrimitive?.content ?: ""

        val idmBytes = if (idmHex.isNotEmpty()) hexToBytes(idmHex) else ByteArray(8)
        val pmmBytes = if (pmmHex.isNotEmpty()) hexToBytes(pmmHex) else ByteArray(8)

        val idm = FeliCaIdm(if (idmBytes.size == 8) idmBytes else ByteArray(8))
        val pmm = FeliCaPmm(if (pmmBytes.size == 8) pmmBytes else ByteArray(8))

        val systemsObj = felica["systems"]?.jsonObject ?: JsonObject(emptyMap())
        val systems =
            systemsObj.entries.map { (codeStr, systemElement) ->
                val code = codeStr.toIntOrNull() ?: codeStr.toIntOrNull(16) ?: 0
                parseFelicaSystem(code, systemElement.jsonObject)
            }

        return RawFelicaCard.create(tagId, scannedAt, idm, pmm, systems)
    }

    private fun parseFelicaSystem(
        code: Int,
        systemObj: JsonObject,
    ): FelicaSystem {
        val servicesObj = systemObj["services"]?.jsonObject ?: JsonObject(emptyMap())
        val services =
            servicesObj.entries.map { (codeStr, serviceElement) ->
                val serviceCode = codeStr.toIntOrNull() ?: codeStr.toIntOrNull(16) ?: 0
                parseFelicaService(serviceCode, serviceElement.jsonObject)
            }
        return FelicaSystem.create(code, services)
    }

    private fun parseFelicaService(
        serviceCode: Int,
        serviceObj: JsonObject,
    ): FelicaService {
        val blocksArray = serviceObj["blocks"]?.jsonArray ?: JsonArray(emptyList())
        val blocks =
            blocksArray.mapIndexed { index, blockElement ->
                val blockObj = blockElement.jsonObject
                val dataHex = blockObj["data"]?.jsonPrimitive?.content ?: ""
                val data = if (dataHex.isNotEmpty()) hexToBytes(dataHex) else ByteArray(16)
                val address = blockObj["address"]?.jsonPrimitive?.intOrNull ?: index
                FelicaBlock.create(address.toByte(), data)
            }
        return FelicaService.create(serviceCode, blocks)
    }

    // --- Helpers ---

    private fun hexToBytes(hex: String): ByteArray {
        if (hex.isEmpty()) return ByteArray(0)
        return try {
            ByteUtils.hexStringToByteArray(hex)
        } catch (e: Exception) {
            println("[MetrodroidJsonParser] Failed to parse hex string: $e")
            ByteArray(0)
        }
    }
}
