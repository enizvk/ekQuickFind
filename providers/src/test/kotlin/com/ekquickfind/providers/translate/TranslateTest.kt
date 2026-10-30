package com.ekquickfind.providers.translate

import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.CopyAction
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import com.ekquickfind.data.prefs.Translator
import com.ekquickfind.data.prefs.UserPreferences
import com.ekquickfind.providers.sites.OpenSiteAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** The switch-to-offline action needs nothing from the host. */
private object NoHost : ActionHost {
    override suspend fun copyToClipboard(
        label: String,
        text: String,
    ) = Unit

    override suspend fun pin(result: SearchResult) = Unit

    override suspend fun unpin(result: SearchResult) = Unit

    override suspend fun hide(result: ResultId) = Unit

    override suspend fun choose(
        title: String,
        options: List<String>,
    ): Int? = null
}

class TranslateTest {
    private fun fixture(name: String) = javaClass.getResource("/translate/$name")!!.readText()

    @Test
    fun googlesAnswerIsJoinedFromItsPiecesWithTheLanguageItFound() {
        assertEquals(
            TranslateOutcome.Done("Къщата ми е много хубава. Разполага с градина.", from = "en"),
            GoogleTranslateJson.parse(fixture("google-en-bg.json")),
        )
        assertNull(GoogleTranslateJson.parse("not json"))
        assertNull(GoogleTranslateJson.parse("[]"))
    }

    /** Pretends to translate by tagging the text; Bulgarian-looking text counts as Bulgarian. */
    private class FakeTranslator(
        var outcome: ((String, String) -> TranslateOutcome)? = null,
    ) : TextTranslator {
        override val name = "Google Translate"
        val calls = mutableListOf<Pair<String, String>>()

        override suspend fun translate(
            text: String,
            to: String,
        ): TranslateOutcome {
            calls += text to to
            outcome?.let { return it(text, to) }
            val from = if (text.any { it in 'а'..'я' }) "bg" else "en"
            return TranslateOutcome.Done("[$to] $text", from)
        }
    }

    private val google = FakeTranslator()
    private val offline = FakeTranslator()
    private var switchedToOffline = false

    private fun TestScope.provider(prefs: UserPreferences = UserPreferences(translateTo = "bg")) =
        TranslateProvider(
            prefs = { prefs },
            translators = { if (it == Translator.Google) google else offline },
            useOffline = { switchedToOffline = true },
            isInstalled = { false },
            io = StandardTestDispatcher(testScheduler),
            locale = { Locale.UK },
        )

    @Test
    fun theKeywordShowsTheTranslationAsTheTopQuickAnswer() =
        runTest {
            val card = provider().search(Query("tr My house is very nice one")).first().single()
            assertEquals("[bg] My house is very nice one", card.title)
            assertEquals("English → Bulgarian · Google Translate", card.subtitle)
            assertEquals(Section.Translation, card.section)
            assertEquals(1f, card.score)
            assertEquals(CopyAction("[bg] My house is very nice one", "Copy translation"), card.primaryAction)
            assertTrue(card.secondaryActions.single() is OpenSiteAction)
            assertTrue(card.titleMaxLines > 1)
        }

    @Test
    fun theTriggerWordWorksToo() =
        runTest {
            assertEquals(
                "[bg] hello there",
                provider()
                    .search(Query("translate hello there"))
                    .first()
                    .single()
                    .title,
            )
        }

    @Test
    fun noKeywordShortTextOrTurnedOffGivesNothing() =
        runTest {
            assertTrue(provider().search(Query("My house is very nice one")).first().isEmpty())
            assertTrue(provider().search(Query("tr a")).first().isEmpty())
            assertTrue(provider(UserPreferences(translateTo = "bg", translateInline = false)).search(Query("tr hello")).first().isEmpty())
            assertTrue(google.calls.isEmpty())
        }

    @Test
    fun withNoLanguageChosenItTranslatesIntoThePhonesLanguage() =
        runTest {
            provider(UserPreferences()).search(Query("tr hola amigo")).first()
            assertEquals("en", google.calls.single().second)
        }

    @Test
    fun textAlreadyInTheTargetLanguageGoesIntoEnglish() =
        runTest {
            val card = provider().search(Query("tr моята къща")).first().single()
            assertEquals("[en] моята къща", card.title)
            assertEquals("Bulgarian → English · Google Translate", card.subtitle)
        }

    @Test
    fun englishIntoEnglishGivesNothing() =
        runTest {
            assertTrue(provider(UserPreferences(translateTo = "en")).search(Query("tr hello there")).first().isEmpty())
        }

    @Test
    fun theOfflineTranslatorIsUsedWhenChosen() =
        runTest {
            provider(UserPreferences(translateTo = "bg", translator = Translator.OnDevice)).search(Query("tr hello")).first()
            assertEquals(1, offline.calls.size)
            assertTrue(google.calls.isEmpty())
        }

    @Test
    fun aMissingOfflineLanguageSaysItsGettingReady() =
        runTest {
            offline.outcome = { _, to -> TranslateOutcome.Preparing(to) }
            val card =
                provider(
                    UserPreferences(translateTo = "bg", translator = Translator.OnDevice),
                ).search(Query("tr hello")).first().single()
            assertEquals("Getting Bulgarian ready for offline use…", card.title)
        }

    @Test
    fun whenGoogleFailsOneTapSwitchesToOffline() =
        runTest {
            google.outcome = { _, _ -> TranslateOutcome.Failed }
            val card = provider().search(Query("tr hello")).first().single()
            assertEquals("Translation unavailable", card.title)
            card.primaryAction.perform(NoHost)
            assertTrue(switchedToOffline)
        }

    @Test
    fun theSameTextIsntTranslatedTwice() =
        runTest {
            val p = provider()
            p.search(Query("tr hello")).first()
            p.search(Query("tr hello")).first()
            assertEquals(1, google.calls.size)
        }
}
