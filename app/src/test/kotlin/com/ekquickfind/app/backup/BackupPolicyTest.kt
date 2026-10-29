package com.ekquickfind.app.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ekquickfind.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import java.io.File

@RunWith(RobolectricTestRunner::class)
// Plain Application: these tests don't need the Hilt graph or its startup work.
@Config(application = android.app.Application::class)
class BackupPolicyTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun prefsFile() = File(context.filesDir, "datastore/preferences.json").apply { parentFile!!.mkdirs() }

    @Test
    fun historyIsExcludedWhenThereAreNoPreferencesYet() {
        prefsFile().delete()
        assertFalse(BackupPolicy.includeHistory(context.filesDir))
    }

    @Test
    fun historyIsExcludedByDefault() {
        prefsFile().writeText("""{"backupHistory":false}""")
        assertFalse(BackupPolicy.includeHistory(context.filesDir))
    }

    @Test
    fun historyIsIncludedWhenTheUserOptsIn() {
        prefsFile().writeText("""{"backupHistory":true,"theme":"Dark"}""")
        assertTrue(BackupPolicy.includeHistory(context.filesDir))
    }

    @Test
    fun corruptPreferencesKeepHistoryOut() {
        prefsFile().writeText("{nope")
        assertFalse(BackupPolicy.includeHistory(context.filesDir))
    }

    @Test
    fun extractionRulesExcludeTheDatabaseFromCloudBackupAndDeviceTransfer() {
        val parser = context.resources.getXml(R.xml.data_extraction_rules)
        val excludes = mutableMapOf<String, MutableList<String>>()
        var section = ""
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "cloud-backup", "device-transfer" -> section = parser.name
                "exclude" ->
                    if (parser.getAttributeValue(null, "domain") ==
                        "database"
                    ) {
                        excludes.getOrPut(section) { mutableListOf() } += parser.getAttributeValue(null, "path")
                    }
            }
        }
        val expected = listOf("ekquickfind.db", "ekquickfind.db-wal", "ekquickfind.db-shm")
        assertEquals(expected, excludes["cloud-backup"])
        assertEquals(expected, excludes["device-transfer"])
    }
}
