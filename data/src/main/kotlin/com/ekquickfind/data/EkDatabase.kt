package com.ekquickfind.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.ekquickfind.data.history.HistoryDao
import com.ekquickfind.data.history.LaunchEvent
import com.ekquickfind.data.history.Pin
import com.ekquickfind.data.photos.PhotoScanRow
import com.ekquickfind.data.photos.PhotoTagDao
import com.ekquickfind.data.photos.PhotoTagRow

@Database(
    entities = [AppLabelEntity::class, LaunchEvent::class, Pin::class, PhotoTagRow::class, PhotoScanRow::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3, spec = EkDatabase.DropRecentSearches::class),
        AutoMigration(from = 3, to = 4),
    ],
)
abstract class EkDatabase : RoomDatabase() {
    abstract fun appLabels(): AppLabelDao

    abstract fun history(): HistoryDao

    abstract fun photoTags(): PhotoTagDao

    // Recent searches were removed in v3.
    @DeleteTable(tableName = "recent_query")
    class DropRecentSearches : AutoMigrationSpec

    companion object {
        const val NAME = "ekquickfind.db"

        fun create(context: Context): EkDatabase = Room.databaseBuilder(context, EkDatabase::class.java, NAME).build()
    }
}
