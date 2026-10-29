package com.ekquickfind.providers.contacts

import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContentResolverContactsSourceTest {
    private lateinit var fake: FakeContactsContentProvider
    private val source = ContentResolverContactsSource(ApplicationProvider.getApplicationContext<Context>().contentResolver)

    @Before
    fun setUp() {
        fake = Robolectric.buildContentProvider(FakeContactsContentProvider::class.java).create(ContactsContract.AUTHORITY).get()
        fake.addContact(
            "Alex Morgan",
            starred = true,
            phones = listOf("+353 87 123 4567" to Phone.TYPE_MOBILE),
            emails = listOf("alex@example.com"),
        )
        fake.addContact(
            "Jamie Lee",
            phones = listOf("(087) 1999-000" to Phone.TYPE_WORK, "01 555 0000" to Phone.TYPE_HOME),
            organization = "Acme",
        )
        fake.addContact("Robert Smith", nickname = "Bobby")
    }

    private fun names(q: String) = source.query(q).map { it.displayName }

    @Test
    fun matchesByName() = assertEquals(listOf("Alex Morgan"), names("alex"))

    @Test
    fun matchesPhoneDigitsIgnoringFormatting() {
        assertEquals(listOf("Jamie Lee"), names("0871"))
        assertEquals(listOf("Alex Morgan"), names("871234"))
    }

    @Test
    fun matchesOrganisation() = assertEquals(listOf("Jamie Lee"), names("acme"))

    @Test
    fun matchesNickname() = assertEquals(listOf("Robert Smith"), names("bobby"))

    @Test
    fun matchesEmail() = assertEquals(listOf("Alex Morgan"), names("example.com"))

    @Test
    fun loadsEveryPhoneWithItsLabelAndTheStarredFlag() {
        val jamie = source.query("jamie").single()
        assertEquals(listOf("(087) 1999-000", "01 555 0000"), jamie.phones.map { it.number })
        assertEquals(listOf(Phone.TYPE_WORK, Phone.TYPE_HOME), jamie.phones.map { it.type })
        assertEquals("Acme", jamie.organization)
        assertTrue(source.query("alex").single().starred)
    }

    @Test
    fun capsAtFiftyContacts() {
        repeat(80) { fake.addContact("Bulk $it") }
        assertEquals(50, source.query("bulk").size)
    }

    @Test
    fun likeWildcardsInTheQueryAreTreatedLiterally() = assertTrue(source.query("%").isEmpty())
}
