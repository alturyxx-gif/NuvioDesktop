package com.nuvio.app.features.player

internal actual object HardwareDecodeCapabilities {
    actual val isSupported: Boolean = false

    actual fun query(): List<HardwareDecodeAdapter> = emptyList()
}
