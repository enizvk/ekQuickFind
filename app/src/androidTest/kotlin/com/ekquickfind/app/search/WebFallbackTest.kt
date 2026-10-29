package com.ekquickfind.app.search

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import com.ekquickfind.app.SearchLauncher
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class WebFallbackTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @javax.inject.Inject lateinit var prefs: com.ekquickfind.data.prefs.PreferencesRepository
    private lateinit var saved: com.ekquickfind.data.prefs.UserPreferences

    @Before
    fun setUp() {
        hilt.inject()
        // Test baseline (DuckDuckGo); the owner's choices are restored afterwards.
        saved = kotlinx.coroutines.runBlocking { prefs.preferences.first() }
        kotlinx.coroutines.runBlocking {
            prefs.update {
                com.ekquickfind.app.testBaseline(it)
            }
        }
        Intents.init()
        // Stub every outgoing intent so no browser actually opens on the device.
        intending(not(isInternal())).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
        SearchLauncher.launch()
    }

    @After
    fun tearDown() {
        Intents.release()
        SearchLauncher.finishAll()
        kotlinx.coroutines.runBlocking { prefs.update { saved } }
    }

    @Test
    fun fallbackIsTheTopResultWhenNothingMatchesAndOpensTheEngine() {
        compose.onNodeWithTag("query").performTextInput("zzqqxx")
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result").and(hasText("Search the web for “zzqqxx”")), 5_000)

        compose.onNodeWithTag("top-result").performClick()
        compose.waitForIdle()

        intended(allOf(hasAction(Intent.ACTION_VIEW), hasData("https://duckduckgo.com/?q=zzqqxx")))
    }
}
