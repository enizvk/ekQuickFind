package com.ekquickfind.providers.files

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore

/** In-memory stand-in for MediaStore's files table, answering Bundle-style queries. */
class FakeMediaContentProvider : ContentProvider() {
    private lateinit var db: SQLiteDatabase

    override fun onCreate(): Boolean {
        db = SQLiteDatabase.create(null)
        db.execSQL(
            """CREATE TABLE files (_id INTEGER PRIMARY KEY AUTOINCREMENT, _display_name TEXT, mime_type TEXT, media_type INTEGER,
               date_modified INTEGER, _size INTEGER, relative_path TEXT, volume_name TEXT)""",
        )
        return true
    }

    fun add(
        name: String,
        mime: String,
        mediaType: Int,
        modifiedSeconds: Long,
        path: String = "Download/",
    ): Long =
        db.insert(
            "files",
            null,
            ContentValues().apply {
                put("_display_name", name)
                put("mime_type", mime)
                put("media_type", mediaType)
                put("date_modified", modifiedSeconds)
                put("_size", 1000)
                put("relative_path", path)
                put("volume_name", MediaStore.VOLUME_EXTERNAL)
            },
        )

    var lastQueryArgs: Bundle? = null

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        queryArgs: Bundle?,
        cancellationSignal: CancellationSignal?,
    ): Cursor {
        lastQueryArgs = queryArgs
        val selection = queryArgs?.getString(ContentResolver.QUERY_ARG_SQL_SELECTION)
        val args = queryArgs?.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS)
        val sort = queryArgs?.getString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER)
        val limit = queryArgs?.getInt(ContentResolver.QUERY_ARG_LIMIT, 0)?.takeIf { it > 0 }?.toString()
        return db.query("files", projection, selection, args, null, null, sort, limit)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor = db.query("files", projection, selection, selectionArgs, null, null, sortOrder)

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
