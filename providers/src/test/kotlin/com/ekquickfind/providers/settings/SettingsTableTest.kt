package com.ekquickfind.providers.settings

import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTableTest {
    private val entries = SettingsTable.entries

    @Test
    fun hasAroundEightyEntries() = assertTrue("only ${entries.size}", entries.size >= 70)

    @Test
    fun everyEntryHasAtLeastTwoKeywordsAndAFallback() {
        entries.forEach { e ->
            assertTrue("${e.key} needs 2+ keywords", e.keywords.size >= 2)
            assertTrue("${e.key} has no fallback", e.fallbackAction.isNotBlank())
        }
    }

    @Test
    fun fallbacksPointAtAParentNotThemselves() {
        entries.filter { it.action != Settings.ACTION_SETTINGS }.forEach { assertNotEquals(it.key, it.action, it.fallbackAction) }
    }

    @Test
    fun keysAreUnique() = assertEquals(entries.size, entries.map { it.key }.toSet().size)

    @Test
    fun includesSynonymsUsersActuallyType() {
        fun has(word: String) = entries.any { e -> e.keywords.any { it.equals(word, ignoreCase = true) } }
        listOf("dark mode", "hotspot", "wifi", "bluetooth", "do not disturb", "screen timeout", "battery saver").forEach {
            assertTrue("missing keyword '$it'", has(it))
        }
    }
}
