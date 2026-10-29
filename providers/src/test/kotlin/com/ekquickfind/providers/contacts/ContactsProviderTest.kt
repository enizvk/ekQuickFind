package com.ekquickfind.providers.contacts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.test.core.app.ApplicationProvider
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.providers.FakeAndroidHost
import com.ekquickfind.providers.PermissionChecker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContactsProviderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun contact(
        id: Long,
        name: String,
        starred: Boolean = false,
        phones: List<ContactPhone> = emptyList(),
        org: String? = null,
        email: String? = null,
    ) = ContactRow(id, "lk$id", name, null, starred, phones, listOfNotNull(email), null, org)

    private val alexA = contact(1, "Alex Kim", phones = listOf(ContactPhone("087 1234 567", Phone.TYPE_MOBILE)))
    private val alexB =
        contact(
            2,
            "Alex Kim",
            starred = true,
            phones = listOf(ContactPhone("01 222 3333", Phone.TYPE_HOME), ContactPhone("086 111 2222", Phone.TYPE_WORK)),
        )
    private val jamie = contact(3, "Jamie Lee", org = "Acme", email = "jamie@example.com")

    private fun kotlinx.coroutines.test.TestScope.provider(
        granted: Boolean = true,
        rows: List<ContactRow> = listOf(alexA, alexB, jamie),
    ) = ContactsProvider(
        source = { rows },
        permissions = PermissionChecker { granted },
        io = StandardTestDispatcher(testScheduler),
    )

    @Test
    fun ungrantedPermissionEmitsASingleEnableRow() =
        runTest {
            val rows = provider(granted = false).search(Query("alex")).first()
            val row = rows.single()
            assertEquals("Enable contacts search", row.title)
            assertFalse(row.isRecordable)

            val host = FakeAndroidHost(context, grant = true)
            assertEquals(ActionOutcome.Refresh, row.primaryAction.perform(host))
            assertEquals(listOf(listOf(Manifest.permission.READ_CONTACTS)), host.requested)
        }

    @Test
    fun partialPhoneNumberMatchesAndShowsTheMatchingNumber() =
        runTest {
            val row = provider().search(Query("0871")).first().single()
            assertEquals("Alex Kim", row.title)
            assertEquals("087 1234 567", row.subtitle)
        }

    @Test
    fun organisationMatches() =
        runTest {
            val row = provider().search(Query("acme")).first().single()
            assertEquals("Jamie Lee", row.title)
            assertEquals("Acme", row.subtitle)
        }

    @Test
    fun starredWinsAtEqualTextMatch() =
        runTest {
            val ids = provider().search(Query("alex")).first().map { it.id.value }
            assertEquals(listOf("contacts:2", "contacts:1"), ids)
        }

    @Test
    fun contactsWithANumberGetInlineCallAndMessage() =
        runTest {
            val alex = provider().search(Query("alex")).first().first()
            assertEquals(listOf("Call", "Message"), alex.inlineActions.map { it.label })
            assertTrue(
                provider()
                    .search(Query("jamie"))
                    .first()
                    .single()
                    .inlineActions
                    .isEmpty(),
            )
        }

    @Test
    fun callWithOneNumberOpensTheDialler() =
        runTest {
            val single = provider(rows = listOf(alexA)).search(Query("alex")).first().single()
            val host = FakeAndroidHost(context)
            single.inlineActions.first { it.label == "Call" }.perform(host)
            assertEquals(Intent.ACTION_DIAL, host.started.single().action)
            assertEquals("tel:087%201234%20567", host.started.single().dataString)
            assertTrue(host.choices.isEmpty())
        }

    @Test
    fun callWithSeveralNumbersAsksWhichOneWithLabels() =
        runTest {
            val row = provider(rows = listOf(alexB)).search(Query("alex")).first().single()
            val host = FakeAndroidHost(context, choice = 1)
            row.inlineActions.first { it.label == "Call" }.perform(host)
            assertEquals(listOf("Home · 01 222 3333", "Work · 086 111 2222"), host.choices.single().second)
            assertEquals("tel:086%20111%202222", host.started.single().dataString)
        }

    @Test
    fun cancellingTheChooserStartsNothing() =
        runTest {
            val row = provider(rows = listOf(alexB)).search(Query("alex")).first().single()
            val host = FakeAndroidHost(context, choice = null)
            assertEquals(ActionOutcome.Stay, row.inlineActions.first().perform(host))
            assertTrue(host.started.isEmpty())
        }

    @Test
    fun tapOpensTheContactCard() =
        runTest {
            val host = FakeAndroidHost(context)
            provider()
                .search(Query("jamie"))
                .first()
                .single()
                .primaryAction
                .perform(host)
            assertEquals(Intent.ACTION_VIEW, host.started.single().action)
            assertTrue(
                host.started
                    .single()
                    .dataString!!
                    .startsWith("content://com.android.contacts/contacts/lookup/lk3"),
            )
        }

    @Test
    fun avatarUsesInitialsWithoutAPhoto() =
        runTest {
            assertEquals(
                ResultIcon.Avatar(null, "JL"),
                provider()
                    .search(Query("jamie"))
                    .first()
                    .single()
                    .icon,
            )
        }
}
