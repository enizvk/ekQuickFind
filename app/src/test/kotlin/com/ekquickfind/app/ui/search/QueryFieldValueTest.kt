package com.ekquickfind.app.ui.search

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class QueryFieldValueTest {
    @Test
    fun aQueryFromOutsidePutsTheCursorAtTheEnd() {
        assertEquals(TextRange(5), fieldValueFor(TextFieldValue(""), "clock").selection)
    }

    @Test
    fun aReplacedQueryAlsoEndsWithTheCursorAtTheEnd() {
        assertEquals(TextRange(7), fieldValueFor(TextFieldValue("clock", TextRange(2)), "weather").selection)
    }

    @Test
    fun theSameTextKeepsTheCursorWhereThePersonPutIt() {
        val typing = TextFieldValue("helo", TextRange(2))
        assertEquals(typing, fieldValueFor(typing, "helo"))
    }
}
