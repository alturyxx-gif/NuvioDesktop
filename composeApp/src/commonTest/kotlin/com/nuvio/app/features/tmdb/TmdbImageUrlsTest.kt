package com.nuvio.app.features.tmdb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TmdbImageUrlsTest {

    @Test
    fun `w1280 tmdb image uses original size`() {
        assertEquals(
            "https://image.tmdb.org/t/p/original/backdrop.jpg?language=en",
            originalTmdbImageUrl("https://image.tmdb.org/t/p/w1280/backdrop.jpg?language=en"),
        )
    }

    @Test
    fun `other tmdb sizes remain unchanged`() {
        val posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg"

        assertEquals(posterUrl, originalTmdbImageUrl(posterUrl))
    }

    @Test
    fun `non tmdb urls remain unchanged`() {
        val imageUrl = "https://example.com/t/p/w1280/backdrop.jpg"

        assertEquals(imageUrl, originalTmdbImageUrl(imageUrl))
    }

    @Test
    fun `null image url remains null`() {
        assertNull(originalTmdbImageUrl(null))
    }

    @Test
    fun `backdrop keeps w1280 on surfaces up to 1920px wide`() {
        val imageUrl = "https://image.tmdb.org/t/p/w1280/backdrop.jpg"

        assertEquals(imageUrl, tmdbBackdropUrlForDisplayWidth(imageUrl, 1366f))
        assertEquals(imageUrl, tmdbBackdropUrlForDisplayWidth(imageUrl, 1920f))
    }

    @Test
    fun `backdrop uses original on surfaces wider than 1920px`() {
        assertEquals(
            "https://image.tmdb.org/t/p/original/backdrop.jpg",
            tmdbBackdropUrlForDisplayWidth("https://image.tmdb.org/t/p/w1280/backdrop.jpg", 2560f),
        )
    }
}
