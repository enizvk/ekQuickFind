package com.ekquickfind.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppLabelDaoTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), EkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val dao = db.appLabels()

    @After
    fun tearDown() = db.close()

    @Test
    fun replaceAllSwapsTheWholeCache() =
        runTest {
            dao.replaceAll(listOf(label("Maps"), label("Gmail")))
            dao.replaceAll(listOf(label("Chrome")))
            assertEquals(listOf("Chrome"), dao.all().map { it.label })
        }

    @Test
    fun keepsProfileSpecificRowsApart() =
        runTest {
            dao.replaceAll(listOf(label("Slack", serial = 0), label("Slack", serial = 10, badge = "Work")))
            assertEquals(2, dao.all().size)
        }

    private fun label(
        name: String,
        serial: Long = 0,
        badge: String? = null,
    ) = AppLabelEntity("com.example.${name.lowercase()}", "Main", serial, name, badge, isSystemApp = false)
}
