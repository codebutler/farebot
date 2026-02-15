package com.codebutler.farebot.card.felica

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mock FeliCa tag adapter for testing the reader algorithm.
 *
 * @param systemCodes System codes to report from [getSystemCodes].
 * @param selectResponses Map of system code → PMm bytes returned by [selectSystem].
 * @param serviceCodesBySystem Map of system code → list of service codes from [getServiceCodes].
 * @param blockData Map of (serviceCode, blockAddr) → block data returned by [readBlock].
 */
class MockFeliCaTagAdapter(
    private val idm: ByteArray = ByteArray(8) { 0x01 },
    private val systemCodes: List<Int> = emptyList(),
    private val selectResponses: Map<Int, ByteArray?> = emptyMap(),
    private val serviceCodesBySystem: Map<Int, List<Int>> = emptyMap(),
    private val blockData: Map<Pair<Int, Byte>, ByteArray?> = emptyMap(),
) : FeliCaTagAdapter {
    val selectSystemCalls = mutableListOf<Int>()
    val readBlockCalls = mutableListOf<Pair<Int, Byte>>()

    override fun getIDm(): ByteArray = idm

    override fun getSystemCodes(): List<Int> = systemCodes

    override fun selectSystem(systemCode: Int): ByteArray? {
        selectSystemCalls.add(systemCode)
        return selectResponses[systemCode]
    }

    override fun getServiceCodes(): List<Int> {
        val lastSelected = selectSystemCalls.lastOrNull() ?: return emptyList()
        return serviceCodesBySystem[lastSelected] ?: emptyList()
    }

    override fun readBlock(
        serviceCode: Int,
        blockAddr: Byte,
    ): ByteArray? {
        readBlockCalls.add(Pair(serviceCode, blockAddr))
        return blockData[Pair(serviceCode, blockAddr)]
    }
}

class FeliCaReaderTest {
    private val dummyPmm = ByteArray(8) { 0x02 }
    private val dummyTagId = ByteArray(8) { 0xAA.toByte() }
    private val dummyBlockData = ByteArray(16) { 0xFF.toByte() }

    @Test
    fun testNormalCardWithSystemCodes() {
        // When system codes are reported, they are used directly (no magic fallback)
        val adapter =
            MockFeliCaTagAdapter(
                systemCodes = listOf(FeliCaConstants.SYSTEMCODE_SUICA),
                selectResponses =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_SUICA to dummyPmm,
                    ),
                serviceCodesBySystem =
                    mapOf(
                        // Use odd service codes (bit 0 == 1) so they pass the auth filter
                        FeliCaConstants.SYSTEMCODE_SUICA to listOf(0x090f),
                    ),
                blockData =
                    mapOf(
                        Pair(0x090f, 0.toByte()) to dummyBlockData,
                    ),
            )

        val result = FeliCaReader.readTag(dummyTagId, adapter)

        assertEquals(1, result.systems.size)
        assertEquals(FeliCaConstants.SYSTEMCODE_SUICA, result.systems[0].code)
        // Service should have the block we provided
        val service = result.systems[0].services.first { !it.skipped }
        assertEquals(0x090f, service.serviceCode)
        assertEquals(1, service.blocks.size)
    }

    @Test
    fun testFelicaLiteDetection() {
        // When no system codes are reported, reader should try FeliCa Lite first
        val adapter =
            MockFeliCaTagAdapter(
                systemCodes = emptyList(),
                selectResponses =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_FELICA_LITE to dummyPmm,
                    ),
                serviceCodesBySystem = emptyMap(),
                blockData =
                    buildMap {
                        // Provide blocks 0x00..0x1F for the Lite readonly service
                        for (addr in 0x00..0x1F) {
                            put(Pair(FeliCaConstants.SERVICE_FELICA_LITE_READONLY, addr.toByte()), dummyBlockData)
                        }
                        // Plus extra block addresses: 0x80..0x88, 0x90, 0x92, 0xa0
                        for (addr in (0x80..0x88) + listOf(0x90, 0x92, 0xa0)) {
                            put(Pair(FeliCaConstants.SERVICE_FELICA_LITE_READONLY, addr.toByte()), dummyBlockData)
                        }
                    },
            )

        val result = FeliCaReader.readTag(dummyTagId, adapter)

        assertEquals(1, result.systems.size)
        assertEquals(FeliCaConstants.SYSTEMCODE_FELICA_LITE, result.systems[0].code)
        assertFalse(result.isPartialRead)

        // Check that regular blocks + extra blocks were all read
        val service = result.systems[0].services.first { !it.skipped }
        // 0x20 regular blocks + 12 extra blocks (0x80..0x88 = 9, 0x90, 0x92, 0xa0 = 3)
        assertEquals(0x20 + 12, service.blocks.size)
    }

    @Test
    fun testFelicaLitePreventsOctopusSztProbing() {
        // When FeliCa Lite is detected, Octopus/SZT should NOT be probed
        val adapter =
            MockFeliCaTagAdapter(
                systemCodes = emptyList(),
                selectResponses =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_FELICA_LITE to dummyPmm,
                        // These would succeed if called, but shouldn't be
                        FeliCaConstants.SYSTEMCODE_OCTOPUS to dummyPmm,
                        FeliCaConstants.SYSTEMCODE_SZT to dummyPmm,
                    ),
                blockData =
                    buildMap {
                        for (addr in 0x00..0x1F) {
                            put(Pair(FeliCaConstants.SERVICE_FELICA_LITE_READONLY, addr.toByte()), dummyBlockData)
                        }
                        for (addr in (0x80..0x88) + listOf(0x90, 0x92, 0xa0)) {
                            put(Pair(FeliCaConstants.SERVICE_FELICA_LITE_READONLY, addr.toByte()), dummyBlockData)
                        }
                    },
            )

        val result = FeliCaReader.readTag(dummyTagId, adapter)

        // Only FeliCa Lite should be in results
        assertEquals(1, result.systems.size)
        assertEquals(FeliCaConstants.SYSTEMCODE_FELICA_LITE, result.systems[0].code)

        // Octopus and SZT system codes should never have been selected
        assertFalse(adapter.selectSystemCalls.contains(FeliCaConstants.SYSTEMCODE_OCTOPUS))
        assertFalse(adapter.selectSystemCalls.contains(FeliCaConstants.SYSTEMCODE_SZT))
    }

    @Test
    fun testOctopusMagicFallback() {
        // When FeliCa Lite is NOT detected, fall back to Octopus/SZT
        val adapter =
            MockFeliCaTagAdapter(
                systemCodes = emptyList(),
                selectResponses =
                    mapOf(
                        // Lite NOT present
                        FeliCaConstants.SYSTEMCODE_OCTOPUS to dummyPmm,
                    ),
                blockData =
                    mapOf(
                        Pair(FeliCaConstants.SERVICE_OCTOPUS, 0.toByte()) to dummyBlockData,
                    ),
            )

        val result = FeliCaReader.readTag(dummyTagId, adapter)

        assertEquals(1, result.systems.size)
        assertEquals(FeliCaConstants.SYSTEMCODE_OCTOPUS, result.systems[0].code)
        // Octopus magic service should have the block we provided
        val service = result.systems[0].services.first { !it.skipped }
        assertEquals(FeliCaConstants.SERVICE_OCTOPUS, service.serviceCode)
        assertEquals(1, service.blocks.size)
    }

    @Test
    fun testSystemCode0Skipped() {
        // System code 0 should be skipped
        val adapter =
            MockFeliCaTagAdapter(
                systemCodes = listOf(0, FeliCaConstants.SYSTEMCODE_SUICA),
                selectResponses =
                    mapOf(
                        0 to dummyPmm,
                        FeliCaConstants.SYSTEMCODE_SUICA to dummyPmm,
                    ),
                serviceCodesBySystem =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_SUICA to listOf(0x090f),
                    ),
                blockData =
                    mapOf(
                        Pair(0x090f, 0.toByte()) to dummyBlockData,
                    ),
            )

        val result = FeliCaReader.readTag(dummyTagId, adapter)

        // Only Suica system should be present, not system code 0
        assertEquals(1, result.systems.size)
        assertEquals(FeliCaConstants.SYSTEMCODE_SUICA, result.systems[0].code)
    }

    @Test
    fun testAuthenticatedServicesExcluded() {
        // Services with bit 0 == 0 require authentication and should be marked as skipped
        val authServiceCode = 0x0008 // bit 0 = 0 → requires auth
        val openServiceCode = 0x000b // bit 0 = 1 → no auth required
        val adapter =
            MockFeliCaTagAdapter(
                systemCodes = listOf(FeliCaConstants.SYSTEMCODE_SUICA),
                selectResponses =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_SUICA to dummyPmm,
                    ),
                serviceCodesBySystem =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_SUICA to listOf(authServiceCode, openServiceCode),
                    ),
                blockData =
                    mapOf(
                        Pair(openServiceCode, 0.toByte()) to dummyBlockData,
                    ),
            )

        val result = FeliCaReader.readTag(dummyTagId, adapter)

        val system = result.systems[0]
        // Both service codes should be in allServiceCodes
        assertTrue(system.allServiceCodes.contains(authServiceCode))
        assertTrue(system.allServiceCodes.contains(openServiceCode))

        // The authenticated service should be marked as skipped
        val skippedService = system.services.first { it.serviceCode == authServiceCode }
        assertTrue(skippedService.skipped)
        assertTrue(skippedService.blocks.isEmpty())

        // The open service should have blocks
        val openService = system.services.first { it.serviceCode == openServiceCode }
        assertFalse(openService.skipped)
        assertEquals(1, openService.blocks.size)
    }

    @Test
    fun testPartialReadOnBlockFailure() {
        // When a block read fails mid-read, isPartialRead should be set
        val adapter =
            MockFeliCaTagAdapter(
                systemCodes = listOf(FeliCaConstants.SYSTEMCODE_SUICA),
                selectResponses =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_SUICA to dummyPmm,
                    ),
                serviceCodesBySystem =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_SUICA to listOf(0x090f),
                    ),
                blockData =
                    mapOf(
                        // First block succeeds, second fails (null)
                        Pair(0x090f, 0.toByte()) to dummyBlockData,
                        // Block 1 is absent → readBlock returns null
                    ),
            )

        val result = FeliCaReader.readTag(dummyTagId, adapter)

        assertTrue(result.isPartialRead)
        assertEquals(1, result.systems.size)
        val service = result.systems[0].services.first { !it.skipped }
        assertEquals(1, service.blocks.size)
    }

    @Test
    fun testFelicaLiteBlockLimit() {
        // FeliCa Lite should stop reading regular blocks at 0x20
        val adapter =
            MockFeliCaTagAdapter(
                systemCodes = emptyList(),
                selectResponses =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_FELICA_LITE to dummyPmm,
                    ),
                blockData =
                    buildMap {
                        // Provide blocks 0x00..0x3F (more than the 0x20 limit)
                        for (addr in 0x00..0x3F) {
                            put(Pair(FeliCaConstants.SERVICE_FELICA_LITE_READONLY, addr.toByte()), dummyBlockData)
                        }
                        // Plus extra block addresses
                        for (addr in (0x80..0x88) + listOf(0x90, 0x92, 0xa0)) {
                            put(Pair(FeliCaConstants.SERVICE_FELICA_LITE_READONLY, addr.toByte()), dummyBlockData)
                        }
                    },
            )

        val result = FeliCaReader.readTag(dummyTagId, adapter)

        val service = result.systems[0].services.first { !it.skipped }
        // Should have 0x20 regular + 12 extra = 44 blocks total, NOT 0x40 + 12
        assertEquals(0x20 + 12, service.blocks.size)

        // Verify no block beyond 0x1F was read as a "regular" block
        val regularAddrs =
            adapter.readBlockCalls
                .filter {
                    it.first == FeliCaConstants.SERVICE_FELICA_LITE_READONLY &&
                        (it.second.toInt() and 0xFF) < 0x80
                }.map { it.second.toInt() and 0xFF }
        assertEquals(0x20, regularAddrs.size)
        assertEquals((0x00..0x1F).toList(), regularAddrs)
    }

    @Test
    fun testPartialReadOnExtraBlockFailure() {
        // When a FeliCa Lite extra block read fails, isPartialRead should be set
        val adapter =
            MockFeliCaTagAdapter(
                systemCodes = emptyList(),
                selectResponses =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_FELICA_LITE to dummyPmm,
                    ),
                blockData =
                    buildMap {
                        for (addr in 0x00..0x1F) {
                            put(Pair(FeliCaConstants.SERVICE_FELICA_LITE_READONLY, addr.toByte()), dummyBlockData)
                        }
                        // Only provide first few extra blocks, then let it fail
                        put(Pair(FeliCaConstants.SERVICE_FELICA_LITE_READONLY, 0x80.toByte()), dummyBlockData)
                        put(Pair(FeliCaConstants.SERVICE_FELICA_LITE_READONLY, 0x81.toByte()), dummyBlockData)
                        // 0x82 not present → null → partial read
                    },
            )

        val result = FeliCaReader.readTag(dummyTagId, adapter)

        assertTrue(result.isPartialRead)
        // Should still have the regular + 2 extra blocks that succeeded
        val service = result.systems[0].services.first { !it.skipped }
        assertEquals(0x20 + 2, service.blocks.size)
    }

    @Test
    fun testIsPartialReadPropagatedToFelicaCard() {
        // Verify isPartialRead propagates from RawFelicaCard.parse() to FelicaCard
        val adapter =
            MockFeliCaTagAdapter(
                systemCodes = listOf(FeliCaConstants.SYSTEMCODE_SUICA),
                selectResponses =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_SUICA to dummyPmm,
                    ),
                serviceCodesBySystem =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_SUICA to listOf(0x090f),
                    ),
                blockData =
                    mapOf(
                        // One block succeeds, next returns null → isPartialRead = true
                        Pair(0x090f, 0.toByte()) to dummyBlockData,
                    ),
            )

        val result = FeliCaReader.readTag(dummyTagId, adapter)

        assertTrue(result.isPartialRead)
        // Parse into FelicaCard and verify propagation
        val card = result.parse()
        assertTrue(card.isPartialRead)
    }

    @Test
    fun testNoPartialReadWhenNoBlocksRead() {
        // When readBlock returns null immediately (no blocks), isPartialRead stays false
        val adapter =
            MockFeliCaTagAdapter(
                systemCodes = listOf(FeliCaConstants.SYSTEMCODE_SUICA),
                selectResponses =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_SUICA to dummyPmm,
                    ),
                serviceCodesBySystem =
                    mapOf(
                        FeliCaConstants.SYSTEMCODE_SUICA to listOf(0x090f),
                    ),
                blockData = emptyMap(), // No blocks available → null immediately
            )

        val result = FeliCaReader.readTag(dummyTagId, adapter)

        assertFalse(result.isPartialRead)
        assertEquals(1, result.systems.size)
        // No services should have blocks (empty service not added)
        assertTrue(result.systems[0].services.all { it.skipped || it.blocks.isEmpty() })
    }
}
