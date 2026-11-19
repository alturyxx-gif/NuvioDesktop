package com.nuvio.app.features.player

/** Codecs the hardware-decoding report covers, in the order Settings lists them. */
enum class HardwareDecodeCodec(val reportKey: String, val label: String) {
    H264("h264", "H.264 (AVC)"),
    Hevc("hevc", "HEVC (H.265) 8-bit"),
    Hevc10("hevc10", "HEVC (H.265) 10-bit / HDR"),
    Vp9("vp9", "VP9 8-bit"),
    Vp910("vp910", "VP9 10-bit / HDR"),
    Av1("av1", "AV1 8-bit"),
    Av110("av110", "AV1 10-bit / HDR"),
    Mpeg2("mpeg2", "MPEG-2"),
    Vc1("vc1", "VC-1"),
}

data class HardwareDecodeCodecSupport(
    val codec: HardwareDecodeCodec,
    /** The GPU's video decoder accepts this codec at 1080p. */
    val supported: Boolean,
    /** It also accepts 3840x2160. */
    val supports4k: Boolean,
)

data class HardwareDecodeAdapter(
    val name: String,
    /** First adapter the OS reports; the one the player decodes on by default. */
    val isDefault: Boolean,
    val codecs: List<HardwareDecodeCodecSupport>,
)

/** Which codecs the GPU can decode, for the Settings report. Desktop (Windows) only. */
internal expect object HardwareDecodeCapabilities {
    val isSupported: Boolean

    /** Empty when the platform can't be queried. Blocking; call off the main thread. */
    fun query(): List<HardwareDecodeAdapter>
}

/**
 * Parses the native report: `adapter=<name>` starts a GPU section, then one
 * `<codec key>=<supported>,<4k>` line per codec with 1/0 flags.
 */
internal fun parseHardwareDecodeReport(report: String): List<HardwareDecodeAdapter> {
    val adapters = mutableListOf<HardwareDecodeAdapter>()
    var currentName: String? = null
    var currentCodecs = mutableMapOf<HardwareDecodeCodec, HardwareDecodeCodecSupport>()

    fun flush() {
        val name = currentName ?: return
        adapters += HardwareDecodeAdapter(
            name = name,
            isDefault = adapters.isEmpty(),
            codecs = HardwareDecodeCodec.entries.map { codec ->
                currentCodecs[codec] ?: HardwareDecodeCodecSupport(codec, supported = false, supports4k = false)
            },
        )
    }

    report.lineSequence().map(String::trim).filter(String::isNotEmpty).forEach { line ->
        val key = line.substringBefore('=', missingDelimiterValue = "")
        val value = line.substringAfter('=', missingDelimiterValue = "")
        if (key.isEmpty()) return@forEach
        if (key == "adapter") {
            flush()
            currentName = value.trim().ifEmpty { "GPU" }
            currentCodecs = mutableMapOf()
            return@forEach
        }
        val codec = HardwareDecodeCodec.entries.firstOrNull { it.reportKey == key } ?: return@forEach
        val flags = value.split(',').map { it.trim() == "1" }
        val supported = flags.getOrElse(0) { false }
        currentCodecs[codec] = HardwareDecodeCodecSupport(
            codec = codec,
            supported = supported,
            supports4k = supported && flags.getOrElse(1) { false },
        )
    }
    flush()
    return adapters
}
