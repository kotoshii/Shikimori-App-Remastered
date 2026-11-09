package com.gnoemes.shikimori.presentation.presenter.common.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

//`[color]` passes `#` with any hex digits, or any word, into the HTML (BB_CODES.md §6)
class CssColorTest {

    @Test
    fun `short and long hex, with and without alpha`() {
        assertEquals(0xFFFF0000.toInt(), CssColor.parse("#f00"))
        assertEquals(0x88FF0000.toInt(), CssColor.parse("#f008"))
        assertEquals(0xFFBF7237.toInt(), CssColor.parse("#BF7237"))
        assertEquals(0x80FF4136.toInt(), CssColor.parse("#ff413680"))
    }

    @Test
    fun `colour names, in any case`() {
        assertEquals(0xFFFF0000.toInt(), CssColor.parse("red"))
        assertEquals(0xFF8B0000.toInt(), CssColor.parse(" DarkRed "))
        assertEquals(0xFF663399.toInt(), CssColor.parse("rebeccapurple"))
    }

    @Test
    fun `what a browser ignores is ignored`() {
        assertNull(CssColor.parse("#ff"))
        assertNull(CssColor.parse("#ggg"))
        assertNull(CssColor.parse("notacolour"))
        assertNull(CssColor.parse("rgb(0,128,0)"))
    }
}
