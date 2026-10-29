package com.ekquickfind.providers.contacts

import android.content.ContentResolver
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data

data class ContactPhone(
    val number: String,
    /** One of [Phone] TYPE_* constants. */
    val type: Int,
    val customLabel: String? = null,
)

data class ContactRow(
    val contactId: Long,
    val lookupKey: String,
    val displayName: String,
    val photoUri: String?,
    val starred: Boolean,
    val phones: List<ContactPhone>,
    val emails: List<String>,
    val nickname: String?,
    val organization: String?,
)

fun interface ContactsSource {
    /** Contacts matching name, nickname, organisation, email or phone digits. Blocking; call off the main thread. */
    fun query(text: String): List<ContactRow>

    /** Full details for known contact ids (used to rebuild pinned and recent rows). */
    fun byIds(ids: List<Long>): List<ContactRow> = emptyList()
}

class ContentResolverContactsSource(
    private val resolver: ContentResolver,
) : ContactsSource {
    override fun query(text: String): List<ContactRow> {
        val ids = matchingContactIds(text)
        if (ids.isEmpty()) return emptyList()
        return loadDetails(ids)
    }

    override fun byIds(ids: List<Long>): List<ContactRow> = if (ids.isEmpty()) emptyList() else loadDetails(ids)

    private fun matchingContactIds(text: String): List<Long> {
        val like = "%${escapeLike(text)}%"
        val digits = text.filter(Char::isDigit)
        // Strip common phone formatting in SQL so "0871" finds "(087) 1999-000".
        val strippedNumber = "replace(replace(replace(replace(replace($DATA1,' ',''),'-',''),'(',''),')',''),'.','')"
        val clauses =
            mutableListOf(
                "$DISPLAY_NAME LIKE ? ESCAPE '\\'",
                "($MIMETYPE IN (?, ?, ?) AND $DATA1 LIKE ? ESCAPE '\\')",
            )
        val args = mutableListOf(like, Nickname.CONTENT_ITEM_TYPE, Organization.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE, like)
        if (digits.length >= 3) {
            clauses += "($MIMETYPE = ? AND $strippedNumber LIKE ?)"
            args += listOf(Phone.CONTENT_ITEM_TYPE, "%$digits%")
        }
        val uri =
            Data.CONTENT_URI
                .buildUpon()
                .appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY, "400")
                .build()
        val ids = LinkedHashSet<Long>()
        resolver
            .query(uri, arrayOf(Data.CONTACT_ID), clauses.joinToString(" OR "), args.toTypedArray(), "${Data.STARRED} DESC, $DISPLAY_NAME")
            ?.use { c ->
                while (c.moveToNext() && ids.size < MAX_CONTACTS) ids += c.getLong(0)
            }
        return ids.toList()
    }

    private fun loadDetails(ids: List<Long>): List<ContactRow> {
        val projection =
            arrayOf(Data.CONTACT_ID, Data.LOOKUP_KEY, DISPLAY_NAME, Data.PHOTO_THUMBNAIL_URI, Data.STARRED, MIMETYPE, DATA1, DATA2, DATA3)
        val placeholders = ids.joinToString(",") { "?" }
        val mimes =
            listOf(
                StructuredName.CONTENT_ITEM_TYPE,
                Phone.CONTENT_ITEM_TYPE,
                Email.CONTENT_ITEM_TYPE,
                Nickname.CONTENT_ITEM_TYPE,
                Organization.CONTENT_ITEM_TYPE,
            )
        val selection = "${Data.CONTACT_ID} IN ($placeholders) AND $MIMETYPE IN (${mimes.joinToString(",") { "?" }})"
        val builders = LinkedHashMap<Long, Builder>()
        ids.forEach { builders[it] = Builder(it) }
        resolver.query(Data.CONTENT_URI, projection, selection, (ids.map(Long::toString) + mimes).toTypedArray(), "${Data._ID}")?.use { c ->
            while (c.moveToNext()) {
                val b = builders[c.getLong(0)] ?: continue
                b.lookup = c.getString(1) ?: b.lookup
                b.name = c.getString(2) ?: b.name
                b.photo = c.getString(3) ?: b.photo
                b.starred = c.getInt(4) == 1
                val data1 = c.getString(6) ?: continue
                when (c.getString(5)) {
                    Phone.CONTENT_ITEM_TYPE -> b.phones += ContactPhone(data1, c.getInt(7), c.getString(8))
                    Email.CONTENT_ITEM_TYPE -> b.emails += data1
                    Nickname.CONTENT_ITEM_TYPE -> b.nickname = data1
                    Organization.CONTENT_ITEM_TYPE -> b.organization = data1
                }
            }
        }
        return builders.values.filter { it.name.isNotEmpty() }.map { it.build() }
    }

    private class Builder(
        val id: Long,
    ) {
        var lookup = ""
        var name = ""
        var photo: String? = null
        var starred = false
        val phones = mutableListOf<ContactPhone>()
        val emails = mutableListOf<String>()
        var nickname: String? = null
        var organization: String? = null

        fun build() =
            ContactRow(
                id,
                lookup,
                name,
                photo,
                starred,
                phones.distinctBy { it.number },
                emails.distinct(),
                nickname,
                organization,
            )
    }

    private companion object {
        const val MAX_CONTACTS = 50
        const val DISPLAY_NAME = Data.DISPLAY_NAME_PRIMARY
        const val MIMETYPE = Data.MIMETYPE
        const val DATA1 = Data.DATA1
        const val DATA2 = Data.DATA2
        const val DATA3 = Data.DATA3

        fun escapeLike(text: String) = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    }
}
