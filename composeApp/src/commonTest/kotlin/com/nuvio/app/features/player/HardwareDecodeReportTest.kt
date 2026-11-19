package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HardwareDecodeReportTest {

    @Test
    fun `parses each adapter with every codec in display order`() {
        val adapters = parseHardwareDecodeReport(
            """
            adapter=Intel(R) UHD Graphics 620
            h264=1,1
            hevc=1,1
            hevc10=1,0
            av1=0,0
            adapter=NVIDIA GeForce MX150
            h264=1,1
            """.trimIndent(),
        )

        assertEquals(listOf("Intel(R) UHD Graphics 620", "NVIDIA GeForce MX150"), adapters.map { it.name })
        assertTrue(adapters[0].isDefault)
        assertFalse(adapters[1].isDefault)
        assertEquals(HardwareDecodeCodec.entries, adapters[0].codecs.map { it.codec })

        val intel = adapters[0].codecs.associateBy { it.codec }
        assertTrue(intel.getValue(HardwareDecodeCodec.Hevc).supports4k)
        assertTrue(intel.getValue(HardwareDecodeCodec.Hevc10).supported)
        assertFalse(intel.getValue(HardwareDecodeCodec.Hevc10).supports4k)
        assertFalse(intel.getValue(HardwareDecodeCodec.Av1).supported)
        // Codecs the report omits count as unsupported.
        assertFalse(intel.getValue(HardwareDecodeCodec.Vp9).supported)
    }

    @Test
    fun `4k flag is ignored when the codec is unsupported`() {
        val codec = parseHardwareDecodeReport("adapter=GPU\nhevc=0,1").single().codecs
            .first { it.codec == HardwareDecodeCodec.Hevc }

        assertFalse(codec.supported)
        assertFalse(codec.supports4k)
    }

    @Test
    fun `empty report yields no adapters`() {
        assertEquals(emptyList(), parseHardwareDecodeReport(""))
    }
}
