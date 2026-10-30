package com.gnoemes.shikimori.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {

    @Test
    fun `a tag and a debug build of the same version match`() {
        assertEquals(0, compareVersions("v0.10.6", "0.10.6-SNAPSHOT"))
    }

    @Test
    fun `a missing part counts as zero`() {
        assertEquals(0, compareVersions("0.8.8", "0.8.8.0"))
    }

    @Test
    fun `parts compare as numbers, not as text`() {
        assertTrue(compareVersions("0.10.10", "0.10.9") > 0)
        assertTrue(compareVersions("0.9.0", "0.10.0") < 0)
    }

    @Test
    fun `a newer tag is newer than the installed version`() {
        assertTrue(compareVersions("v0.12.0", "0.11.9") > 0)
        assertTrue(compareVersions("v0.11.9", "0.12.0") < 0)
    }

    @Test
    fun `a missing version is older than any`() {
        assertTrue(compareVersions(null, "0.1") < 0)
    }
}
