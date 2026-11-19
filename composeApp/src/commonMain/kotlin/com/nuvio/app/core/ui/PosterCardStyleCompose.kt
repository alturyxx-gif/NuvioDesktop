package com.nuvio.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App-wide subscriptions to the card style repositories. Every poster reads these styles,
 * several times each (card, hover preview, depth effect); without a shared State each read
 * started its own collectAsState coroutine, so a row of posters scrolling into view launched
 * dozens of flow collectors.
 */
private val LocalPosterCardStyleState = staticCompositionLocalOf<State<PosterCardStyleUiState>?> { null }
private val LocalCardDepthStyleState = staticCompositionLocalOf<State<CardDepthStyleUiState>?> { null }

@Composable
internal fun ProvideSharedCardStyleStates(content: @Composable () -> Unit) {
    val posterCardStyle = PosterCardStyleRepository.uiState.collectAsState()
    val cardDepthStyle = CardDepthStyleRepository.uiState.collectAsState()
    CompositionLocalProvider(
        LocalPosterCardStyleState provides posterCardStyle,
        LocalCardDepthStyleState provides cardDepthStyle,
        content = content,
    )
}

/** The desktop playback mode for animated posters and episode thumbnails, from the user's setting. */
@Composable
internal fun rememberAnimatedArtworkAnimation(): NuvioDesktopAnimation =
    rememberPosterCardStyleUiState().animatedArtworkMode.toDesktopAnimation()

@Composable
internal fun rememberPosterCardStyleUiState(): PosterCardStyleUiState {
    PosterCardStyleRepository.ensureLoaded()
    LocalPosterCardStyleState.current?.let { shared -> return shared.value }
    val uiState by PosterCardStyleRepository.uiState.collectAsState()
    return uiState
}

@Composable
internal fun sharedCardDepthStyleUiState(): CardDepthStyleUiState? =
    LocalCardDepthStyleState.current?.value
