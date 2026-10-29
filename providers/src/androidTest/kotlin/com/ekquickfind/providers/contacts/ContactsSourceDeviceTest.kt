package com.ekquickfind.providers.contacts

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real SQL against the device's contacts provider. Asserts on counts only; never logs contact data. */
@RunWith(AndroidJUnit4::class)
class ContactsSourceDeviceTest {
    @get:Rule
    val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS)

    private val source = ContentResolverContactsSource(ApplicationProvider.getApplicationContext<Context>().contentResolver)

    @Test
    fun nameAndDigitQueriesAreAcceptedByTheRealProvider() {
        val byName = source.query("a")
        assumeTrue("device has no contacts", byName.isNotEmpty())
        assertTrue(byName.size <= 50)
        assertTrue(byName.all { it.displayName.isNotBlank() && it.lookupKey.isNotBlank() })

        val someDigits =
            byName
                .flatMap { it.phones }
                .firstOrNull()
                ?.number
                ?.filter(Char::isDigit)
                ?.takeLast(4)
        assumeTrue("no phone numbers", someDigits != null)
        assertTrue("digit search found nothing", source.query(someDigits!!).isNotEmpty())
    }
}
