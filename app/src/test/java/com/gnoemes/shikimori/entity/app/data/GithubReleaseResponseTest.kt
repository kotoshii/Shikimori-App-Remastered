package com.gnoemes.shikimori.entity.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GithubReleaseResponseTest {

    private fun release(body: String?) = GithubReleaseResponse("v1.0.0", null, body, null)

    @Test
    fun `the notes end at the marker`() {
        val body = "Исправлен плеер.\n\n<!-- app:cut -->\n\n**Full Changelog**: v0.9.0...v1.0.0"

        assertEquals("Исправлен плеер.", release(body).localizedBody)
    }

    @Test
    fun `the marker may differ in spaces and case`() {
        assertEquals("Исправлен плеер.", release("Исправлен плеер.<!--APP:CUT-->English").localizedBody)
    }

    @Test
    fun `notes without the marker are kept whole`() {
        assertEquals("Исправлен плеер.", release("  Исправлен плеер.\n").localizedBody)
    }

    @Test
    fun `no notes stay no notes`() {
        assertNull(release(null).localizedBody)
    }
}
