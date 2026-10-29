package com.ekquickfind.providers.contacts

import android.Manifest
import android.content.Intent
import android.provider.ContactsContract
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.core.search.MatchTarget
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.QueryMatcher
import com.ekquickfind.core.search.Requirement
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.ResultResolver
import com.ekquickfind.core.search.SearchProvider
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import com.ekquickfind.core.search.TextNormalizer
import com.ekquickfind.providers.HandlerResolver
import com.ekquickfind.providers.PermissionChecker
import com.ekquickfind.providers.RequestPermissionAction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class ContactsProvider(
    private val source: ContactsSource,
    private val permissions: PermissionChecker,
    private val io: CoroutineDispatcher,
    private val handlers: HandlerResolver = HandlerResolver.None,
) : SearchProvider,
    ResultResolver {
    override val id = ID
    override val section = Section.Contacts
    override val requirements = setOf(Requirement.Permission(PERMISSION))

    private val contactsApp by lazy { handlers.packageFor(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)) }

    override fun search(query: Query): Flow<List<SearchResult>> =
        flow {
            if (!permissions.isGranted(PERMISSION)) {
                emit(listOf(enableRow()))
                return@flow
            }
            // Short pause so fast typing doesn't hit the contacts database on every character.
            delay(DEBOUNCE_MS)
            val q = query.normalized
            val digits = query.raw.filter(Char::isDigit)
            val rows = withContext(io) { source.query(query.raw.trim()) }
            emit(rows.mapNotNull { toResult(it, q, digits) }.sortedByDescending { it.score })
        }

    override suspend fun resolve(id: ResultId): SearchResult? {
        if (!permissions.isGranted(PERMISSION)) return null
        val contactId = id.value.removePrefix("contacts:").toLongOrNull() ?: return null
        val row = withContext(io) { source.byIds(listOf(contactId)) }.firstOrNull() ?: return null
        return toResult(row, TextNormalizer.normalize(row.displayName), "")
    }

    private fun toResult(
        row: ContactRow,
        q: String,
        digits: String,
    ): SearchResult? {
        val phoneHit = row.phones.firstOrNull { digits.length >= 3 && digits in it.number.filter(Char::isDigit) }
        val textFields = listOfNotNull(row.displayName, row.nickname, row.organization) + row.emails
        val textScore = QueryMatcher.bestScore(q, textFields.map(::MatchTarget))
        val base = listOfNotNull(textScore, PHONE_SCORE.takeIf { phoneHit != null }).maxOrNull() ?: return null
        // Starred breaks ties without jumping a better text match.
        val score = base + if (row.starred) STARRED_BONUS else 0f
        val orgHit = row.organization?.let { QueryMatcher.score(q, MatchTarget(it)) } != null
        val subtitle =
            phoneHit?.number
                ?: row.organization.takeIf { orgHit }
                ?: row.phones.firstOrNull()?.number
                ?: row.emails.firstOrNull()
                ?: row.organization
        val numbers = row.phones
        return SearchResult(
            id = ResultId("contacts:${row.contactId}"),
            providerId = ID,
            section = section,
            title = row.displayName,
            subtitle = subtitle,
            icon = ResultIcon.Avatar(row.photoUri, initials(row.displayName)),
            score = score.coerceAtMost(1f),
            primaryAction = OpenContactAction(row.contactId, row.lookupKey),
            handler = contactsApp,
            inlineActions =
                if (numbers.isEmpty()) {
                    emptyList()
                } else {
                    listOf(NumberAction(numbers, NumberAction.Kind.Call), NumberAction(numbers, NumberAction.Kind.Message))
                },
        )
    }

    private fun enableRow() =
        SearchResult(
            id = ResultId("contacts:enable"),
            providerId = ID,
            section = section,
            title = "Enable contacts search",
            subtitle = "Find people, call or text them from here",
            icon = ResultIcon.Vector(Glyph.Lock),
            score = 0.2f,
            primaryAction = RequestPermissionAction(listOf(PERMISSION)),
            isRecordable = false,
        )

    companion object {
        val ID = ProviderId("contacts")
        const val PERMISSION = Manifest.permission.READ_CONTACTS
        private const val DEBOUNCE_MS = 60L
        private const val PHONE_SCORE = 0.7f
        private const val STARRED_BONUS = 0.02f

        fun initials(name: String) =
            name
                .split(' ')
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.take(1).uppercase() }
    }
}
