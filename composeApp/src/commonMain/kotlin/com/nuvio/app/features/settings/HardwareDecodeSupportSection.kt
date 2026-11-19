package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.player.HardwareDecodeAdapter
import com.nuvio.app.features.player.HardwareDecodeCapabilities
import com.nuvio.app.features.player.HardwareDecodeCodecSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

// The driver's answer doesn't change while the app runs, and asking creates a D3D
// device per GPU, so the first result is kept for the session.
private var cachedHardwareDecodeReport: List<HardwareDecodeAdapter>? = null

/** The GPU decoder report, queried once per session off the main thread; null while loading. */
@Composable
internal fun rememberHardwareDecodeReport(): List<HardwareDecodeAdapter>? {
    val report by produceState(initialValue = cachedHardwareDecodeReport) {
        if (value == null) {
            value = withContext(Dispatchers.Default) { HardwareDecodeCapabilities.query() }
                .also { cachedHardwareDecodeReport = it }
        }
    }
    return report
}

/**
 * False only when the report positively lists the GPUs and none is NVIDIA. Unknown
 * (still loading, or the query failed) counts as possibly present.
 */
internal fun List<HardwareDecodeAdapter>?.mayHaveNvidiaGpu(): Boolean =
    this == null || isEmpty() || any { it.name.contains("NVIDIA", ignoreCase = true) }

@Composable
internal fun HardwareDecodeSupportSection(isTablet: Boolean) {
    val report = rememberHardwareDecodeReport()
    val tokens = MaterialTheme.nuvio
    val horizontalPadding = if (isTablet) 20.dp else 16.dp

    SettingsSection(
        title = stringResource(Res.string.settings_playback_hardware_decode_section),
        isTablet = isTablet,
    ) {
        val adapters = report
        when {
            adapters == null -> HardwareDecodeMessage(
                text = stringResource(Res.string.settings_playback_hardware_decode_checking),
                isTablet = isTablet,
            )
            adapters.isEmpty() -> HardwareDecodeMessage(
                text = stringResource(Res.string.settings_playback_hardware_decode_unavailable),
                isTablet = isTablet,
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(if (isTablet) 14.dp else 10.dp)) {
                adapters.forEach { adapter ->
                    SettingsGroup(isTablet = isTablet) {
                        Text(
                            text = if (adapter.isDefault) {
                                stringResource(Res.string.settings_playback_hardware_decode_default_gpu, adapter.name)
                            } else {
                                adapter.name
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = tokens.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                start = horizontalPadding,
                                end = horizontalPadding,
                                top = 14.dp,
                                bottom = 6.dp,
                            ),
                        )
                        adapter.codecs.forEach { support ->
                            HardwareDecodeCodecRow(support = support, horizontalPadding = horizontalPadding)
                        }
                    }
                }
                Text(
                    text = stringResource(Res.string.settings_playback_hardware_decode_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                )
            }
        }
    }
}

@Composable
private fun HardwareDecodeCodecRow(
    support: HardwareDecodeCodecSupport,
    horizontalPadding: androidx.compose.ui.unit.Dp,
) {
    val tokens = MaterialTheme.nuvio
    val (status, color) = when {
        support.supports4k -> stringResource(Res.string.settings_playback_hardware_decode_supported_4k) to tokens.colors.success
        support.supported -> stringResource(Res.string.settings_playback_hardware_decode_supported_1080p) to tokens.colors.warning
        else -> stringResource(Res.string.settings_playback_hardware_decode_unsupported) to tokens.colors.danger
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = support.codec.label,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textPrimary,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        HardwareDecodeStatus(text = status, color = color)
    }
}

@Composable
private fun HardwareDecodeStatus(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun HardwareDecodeMessage(text: String, isTablet: Boolean) {
    SettingsGroup(isTablet = isTablet) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.nuvio.colors.textMuted,
            modifier = Modifier.padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = 14.dp),
        )
    }
}
