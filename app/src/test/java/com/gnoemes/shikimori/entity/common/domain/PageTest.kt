package com.gnoemes.shikimori.entity.common.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageTest {

    @Test
    fun `withExtraRow drops the extra row and is not the last page`() {
        val page = Page.withExtraRow(listOf(1, 2, 3, 4), limit = 3)

        assertEquals(listOf(1, 2, 3), page.items)
        assertFalse(page.isLast)
    }

    @Test
    fun `withExtraRow without the extra row is the last page`() {
        val page = Page.withExtraRow(listOf(1, 2, 3), limit = 3)

        assertEquals(listOf(1, 2, 3), page.items)
        assertTrue(page.isLast)
    }

    @Test
    fun `withExtraRow with no rows is the last page`() {
        val page = Page.withExtraRow(emptyList<Int>(), limit = 3)

        assertTrue(page.items.isEmpty())
        assertTrue(page.isLast)
    }

    @Test
    fun `exact keeps every row and ends on a short page`() {
        assertFalse(Page.exact(listOf(1, 2, 3), limit = 3).isLast)

        val short = Page.exact(listOf(1, 2), limit = 3)
        assertEquals(listOf(1, 2), short.items)
        assertTrue(short.isLast)
    }

    @Test
    fun `endsWhenEmpty ends only on an empty page`() {
        val full = Page.endsWhenEmpty(listOf(1, 2, 3, 4), limit = 3)
        assertEquals(listOf(1, 2, 3), full.items)
        assertFalse(full.isLast)

        //topics the reader may not see are dropped after the page is taken, so a short page proves nothing
        assertFalse(Page.endsWhenEmpty(listOf(1, 2), limit = 3).isLast)
        assertTrue(Page.endsWhenEmpty(emptyList<Int>(), limit = 3).isLast)
    }

    @Test
    fun `map keeps whether the page is the last`() {
        val page = Page.withExtraRow(listOf(1, 2), limit = 3).map { rows -> rows.map { it.toString() } }

        assertEquals(listOf("1", "2"), page.items)
        assertTrue(page.isLast)
    }
}
