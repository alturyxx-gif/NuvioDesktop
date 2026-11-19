package com.nuvio.app.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Rows that show skeleton cards until the posters they have created have loaded, then
 * fade the real cards in together, instead of filling in poster by poster.
 */
private const val PosterRowRevealTimeoutMillis = 3_000L
private const val PosterRowRevealFadeMillis = 260

/** Counts the posters a row has created and how many have finished loading (or failed). */
@Stable
internal class PosterRowLoadTracker {
    private var registered by mutableIntStateOf(0)
    private var pending by mutableIntStateOf(0)

    /** True once at least one poster exists and none are still loading. */
    val allSettled: Boolean
        get() = registered > 0 && pending == 0

    fun newHandle(): PosterLoadHandle = PosterLoadHandle(this)

    internal fun onRegistered(alreadySettled: Boolean) {
        registered++
        if (!alreadySettled) pending++
    }

    internal fun onSettledAfterRegister() {
        pending--
    }
}

/**
 * One poster's slot in a [PosterRowLoadTracker]. Registration happens after composition
 * and the image can finish before that (memory cache hits), so both orders are handled.
 * Main thread only.
 */
internal class PosterLoadHandle(private val tracker: PosterRowLoadTracker) {
    private var registered = false
    private var settled = false

    fun register() {
        if (registered) return
        registered = true
        tracker.onRegistered(alreadySettled = settled)
    }

    fun settle() {
        if (settled) return
        settled = true
        if (registered) tracker.onSettledAfterRegister()
    }
}

/** What a poster card inside a revealing row needs to know. */
@Stable
internal class PosterRowLoadState(
    val tracker: PosterRowLoadTracker,
    /** Skeletons are composed while true; false once the fade-in has finished. */
    val skeletonVisible: Boolean,
    /** 0 = skeleton fully shown, 1 = real card fully shown. Read in draw only. */
    val revealProgress: () -> Float,
)

internal val LocalPosterRowLoadState = staticCompositionLocalOf<PosterRowLoadState?> { null }

/** Rows already revealed this session, so scrolling back doesn't show skeletons again. */
private val revealedPosterRows = HashSet<String>()

/**
 * Wraps a row of poster cards. Cards inside keep loading normally but are covered by
 * skeletons until every created card has settled, or [PosterRowRevealTimeoutMillis] passes.
 */
@Composable
internal fun PosterRowReveal(
    rowKey: String,
    content: @Composable () -> Unit,
) {
    val alreadyRevealed = remember(rowKey) { rowKey in revealedPosterRows }
    val tracker = remember(rowKey) { PosterRowLoadTracker() }
    val progress = remember(rowKey) { Animatable(if (alreadyRevealed) 1f else 0f) }
    var skeletonVisible by remember(rowKey) { mutableStateOf(!alreadyRevealed) }

    if (skeletonVisible) {
        LaunchedEffect(rowKey) {
            withTimeoutOrNull(PosterRowRevealTimeoutMillis) {
                snapshotFlow { tracker.allSettled }.first { it }
            }
            revealedPosterRows += rowKey
            progress.animateTo(1f, tween(PosterRowRevealFadeMillis))
            skeletonVisible = false
        }
    }

    val state = remember(tracker, skeletonVisible, progress) {
        PosterRowLoadState(
            tracker = tracker,
            skeletonVisible = skeletonVisible,
            revealProgress = { progress.value },
        )
    }
    CompositionLocalProvider(LocalPosterRowLoadState provides state, content = content)
}
