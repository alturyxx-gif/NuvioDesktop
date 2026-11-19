package com.nuvio.app.features.tmdb

private const val TMDB_W1280_IMAGE_PREFIX = "https://image.tmdb.org/t/p/w1280/"
private const val TMDB_ORIGINAL_IMAGE_PREFIX = "https://image.tmdb.org/t/p/original/"

internal fun originalTmdbImageUrl(imageUrl: String?): String? {
    if (imageUrl == null || !imageUrl.startsWith(TMDB_W1280_IMAGE_PREFIX)) return imageUrl

    return TMDB_ORIGINAL_IMAGE_PREFIX + imageUrl.removePrefix(TMDB_W1280_IMAGE_PREFIX)
}

// w1280 still looks sharp when stretched up to 1.5x, so `original` (often 3840px+ and
// several MB per image) is only worth downloading and decoding on wider surfaces.
private const val TMDB_ORIGINAL_MIN_DISPLAY_WIDTH_PX = 1280f * 1.5f

/**
 * Picks the TMDB backdrop size for a surface [displayWidthPx] pixels wide: `original`
 * only when w1280 would be visibly upscaled, otherwise the w1280 URL unchanged.
 */
internal fun tmdbBackdropUrlForDisplayWidth(imageUrl: String?, displayWidthPx: Float): String? =
    if (displayWidthPx > TMDB_ORIGINAL_MIN_DISPLAY_WIDTH_PX) originalTmdbImageUrl(imageUrl) else imageUrl
