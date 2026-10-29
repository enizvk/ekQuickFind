package com.ekquickfind.app.ui.search

import org.junit.Assert.assertEquals
import org.junit.Test

class MoreBelowLabelTest {
    private val order = listOf("Apps", "Settings", "Web")

    @Test
    fun namesTheSectionsThatStartBelowTheScreen() {
        assertEquals("More below: Settings · Web", moreBelowLabel(order, current = "Apps"))
    }

    @Test
    fun inTheLastSectionItSaysMoreOfThatSection() {
        assertEquals("More Web below", moreBelowLabel(order, current = "Web"))
    }

    @Test
    fun withoutAKnownSectionItStaysGeneric() {
        assertEquals("More results below", moreBelowLabel(order, current = null))
    }
}
