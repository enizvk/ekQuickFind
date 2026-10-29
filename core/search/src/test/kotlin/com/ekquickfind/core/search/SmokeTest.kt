package com.ekquickfind.core.search

import app.cash.turbine.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SmokeTest {
    @Test
    fun `test toolchain runs coroutines and turbine`() =
        runTest {
            flowOf(1).test {
                assertEquals(1, awaitItem())
                awaitComplete()
            }
        }
}
