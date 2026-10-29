package com.ekquickfind.app.ui.search

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipKindTest {
    @Test
    fun webAddressesAreLinks() {
        assertEquals(ClipKind.Link, ClipKind.of("https://example.com/a?b=1"))
        assertEquals(ClipKind.Link, ClipKind.of("  www.example.com  "))
    }

    @Test
    fun phoneNumbersAreRecognisedWithCommonFormatting() {
        assertEquals(ClipKind.Phone, ClipKind.of("+359 88 123 4567"))
        assertEquals(ClipKind.Phone, ClipKind.of("(087) 1999-000"))
    }

    @Test
    fun everythingElseIsText() {
        assertEquals(ClipKind.Text, ClipKind.of("buy milk and eggs"))
        assertEquals(ClipKind.Text, ClipKind.of("12"))
        assertEquals(ClipKind.Text, ClipKind.of("see https://example.com later"))
    }
}
