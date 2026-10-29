package com.ekquickfind.app.search

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.ekquickfind.app.SearchLauncher
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Uses a test image this test inserts and deletes, so no personal media is shown or touched. */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class FileResultsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_IMAGES)

    @get:Rule(order = 2)
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "ekquickfindtest${System.currentTimeMillis()}"
    private lateinit var image: Uri

    @Before
    fun setUp() {
        image = insertTestImage()
        SearchLauncher.launch()
    }

    @After
    fun tearDown() {
        SearchLauncher.finishAll()
        context.contentResolver.delete(image, null, null)
    }

    @Test
    fun imageResultsShowAThumbnail() {
        compose.onNodeWithTag("query").performTextInput(name)
        compose.waitUntilAtLeastOneExists(hasText("$name.png"), 5_000)
        compose.waitUntil(3_000) { compose.onAllNodesWithTag("thumbnail", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun longPressOffersShareAndShowInFolder() {
        compose.onNodeWithTag("query").performTextInput(name)
        compose.waitUntilAtLeastOneExists(hasText("$name.png"), 5_000)
        compose.onNodeWithText("$name.png").performTouchInput { longClick() }
        compose.waitUntilAtLeastOneExists(hasText("Share"), 2_000)
        compose.waitUntilAtLeastOneExists(hasText("Show in folder"), 2_000)
    }

    private fun insertTestImage(): Uri {
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EkQuickFindTest")
            }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)!!
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).also { Canvas(it).drawColor(Color.MAGENTA) }
        context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return uri
    }
}
