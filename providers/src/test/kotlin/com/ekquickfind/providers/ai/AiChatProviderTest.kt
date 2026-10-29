package com.ekquickfind.providers.ai

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.prefs.AiChat
import com.ekquickfind.providers.FakeAndroidHost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AiChatProviderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun provider(
        enabled: Set<AiChat> = AiChat.entries.toSet(),
        installed: Set<String> = emptySet(),
    ) = AiChatProvider(enabled = { enabled }, isInstalled = { it in installed })

    @Test
    fun enabledAssistantsAreOfferedInTheirOwnSectionInAFixedOrder() =
        runTest {
            val rows = provider(enabled = setOf(AiChat.Gemini, AiChat.ChatGpt)).search(Query("best pizza in rome")).first()
            assertEquals(listOf("Ask ChatGPT", "Ask Gemini"), rows.map { it.title })
            assertTrue(rows.all { it.section == Section.AskAi && !it.isPromotable })
        }

    @Test
    fun nothingIsOfferedForAnEmptyQuery() =
        runTest {
            assertTrue(provider().search(Query("  ")).first().isEmpty())
        }

    @Test
    fun anInstalledAppShowsItsOwnIcon() =
        runTest {
            val rows = provider(installed = setOf(AiChat.Claude.packageName)).search(Query("hi")).first()
            assertEquals(AiChat.Claude.packageName, rows.single { it.title == "Ask Claude" }.handler)
            assertNull(rows.single { it.title == "Ask ChatGPT" }.handler)
        }

    @Test
    fun askingChatGptOpensItWithTheQuestionFilledIn() =
        runTest {
            val row = provider().search(Query("best pizza in rome")).first().single { it.title == "Ask ChatGPT" }
            val host = FakeAndroidHost(context)
            assertEquals(ActionOutcome.Close, row.primaryAction.perform(host))
            val intent = host.started.single()
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals("https://chatgpt.com/?q=best%20pizza%20in%20rome", intent.dataString)
            assertNull(host.copied)
        }

    @Test
    fun askingClaudeAlsoCopiesTheQuestionInCaseItDoesntArrive() =
        runTest {
            val row = provider().search(Query("explain monads")).first().single { it.title == "Ask Claude" }
            val host = FakeAndroidHost(context)
            row.primaryAction.perform(host)
            assertEquals("https://claude.ai/new?q=explain%20monads", host.started.single().dataString)
            assertEquals("explain monads", host.copied)
        }

    @Test
    fun geminiHasNoWayToPrefillSoTheQuestionIsCopiedAndTheSiteOpens() =
        runTest {
            val row = provider().search(Query("weather oslo")).first().single { it.title == "Ask Gemini" }
            val host = FakeAndroidHost(context)
            row.primaryAction.perform(host)
            assertEquals("weather oslo", host.copied)
            assertEquals("https://gemini.google.com/app", host.started.single().dataString)
            assertFalse(row.subtitle.isNullOrBlank())
        }
}
