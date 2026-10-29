package com.ekquickfind.app.search

import android.app.SearchManager
import android.content.Intent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.ekquickfind.app.SearchLauncher
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class IncomingIntentTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @After
    fun tearDown() = SearchLauncher.finishAll()

    @Test
    fun webSearchIntentPrefillsTheQueryAndStartsSearching() {
        SearchLauncher.launch {
            action = Intent.ACTION_WEB_SEARCH
            putExtra(SearchManager.QUERY, "cats")
        }
        compose.waitUntilAtLeastOneExists(hasTestTag("query").and(hasText("cats")), 3_000)
        compose.waitUntilAtLeastOneExists(hasTestTag("top-result"), 3_000)
    }

    @Test
    fun searchIntentWhileOpenReplacesTheQuery() {
        SearchLauncher.launch()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.startActivity(
            Intent(Intent.ACTION_SEARCH)
                .setClass(context, SearchActivity::class.java)
                .putExtra(SearchManager.QUERY, "weather")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        compose.waitUntilAtLeastOneExists(hasTestTag("query").and(hasText("weather")), 3_000)
    }

    @Test
    fun manifestDeclaresSearchWebSearchAndAssistHandlers() {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        listOf(Intent.ACTION_SEARCH, Intent.ACTION_WEB_SEARCH, Intent.ACTION_ASSIST).forEach { action ->
            val handlers = pm.queryIntentActivities(Intent(action).setPackage("com.ekquickfind.app"), 0)
            assertTrue("$action not handled", handlers.any { it.activityInfo.name == SearchActivity::class.java.name })
        }
    }
}
