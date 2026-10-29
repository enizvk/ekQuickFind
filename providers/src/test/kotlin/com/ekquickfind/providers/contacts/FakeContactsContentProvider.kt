package com.ekquickfind.providers.contacts

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data

/** In-memory stand-in for the contacts Data view, so real selection SQL runs against SQLite. */
class FakeContactsContentProvider : ContentProvider() {
    private lateinit var db: SQLiteDatabase
    private var nextContact = 1L

    override fun onCreate(): Boolean {
        db = SQLiteDatabase.create(null)
        db.execSQL(
            """CREATE TABLE data (_id INTEGER PRIMARY KEY AUTOINCREMENT, contact_id INTEGER, lookup TEXT, display_name TEXT,
               starred INTEGER, photo_thumb_uri TEXT, mimetype TEXT, data1 TEXT, data2 INTEGER, data3 TEXT)""",
        )
        return true
    }

    fun addContact(
        name: String,
        starred: Boolean = false,
        phones: List<Pair<String, Int>> = emptyList(),
        emails: List<String> = emptyList(),
        nickname: String? = null,
        organization: String? = null,
    ): Long {
        val id = nextContact++

        fun row(
            mime: String,
            data1: String,
            data2: Int? = null,
        ) = db.insert(
            "data",
            null,
            ContentValues().apply {
                put("contact_id", id)
                put("lookup", "lookup$id")
                put("display_name", name)
                put("starred", if (starred) 1 else 0)
                put("mimetype", mime)
                put("data1", data1)
                data2?.let { put("data2", it) }
            },
        )
        row(StructuredName.CONTENT_ITEM_TYPE, name)
        phones.forEach { (number, type) -> row(Phone.CONTENT_ITEM_TYPE, number, type) }
        emails.forEach { row(Email.CONTENT_ITEM_TYPE, it) }
        nickname?.let { row(Nickname.CONTENT_ITEM_TYPE, it) }
        organization?.let { row(Organization.CONTENT_ITEM_TYPE, it) }
        return id
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        require(uri.toString().startsWith(Data.CONTENT_URI.toString())) { "unexpected uri $uri" }
        val limit = uri.getQueryParameter("limit")
        return db.query("data", projection, selection, selectionArgs, null, null, sortOrder, limit)
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = 0
}
