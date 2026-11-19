package com.nuvio.app.features.player

import co.touchlab.kermit.Logger
import com.nuvio.app.features.player.desktop.DesktopHostOs
import com.nuvio.app.features.player.desktop.NativePlayerBridge

internal actual object HardwareDecodeCapabilities {
    private val log = Logger.withTag("HardwareDecodeCapabilities")

    // Only the Windows bridge implements the query (D3D11 video decoder profiles).
    actual val isSupported: Boolean
        get() = DesktopHostOs.current == DesktopHostOs.WINDOWS

    actual fun query(): List<HardwareDecodeAdapter> {
        if (!isSupported) return emptyList()
        return runCatching {
            NativePlayerBridge.ensureLoaded()
            parseHardwareDecodeReport(NativePlayerBridge.hardwareDecoderSupport())
        }.onFailure { error ->
            log.w(error) { "hardware decoder query failed" }
        }.getOrDefault(emptyList())
    }
}
