package com.ekquickfind.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/** Last known launcher labels, so the app index can render before LauncherApps answers. */
@Entity(tableName = "app_label", primaryKeys = ["packageName", "activityName", "userSerial"])
data class AppLabelEntity(
    val packageName: String,
    val activityName: String,
    val userSerial: Long,
    val label: String,
    val profileBadge: String?,
    val isSystemApp: Boolean,
)

@Dao
interface AppLabelDao {
    @Query("SELECT * FROM app_label")
    suspend fun all(): List<AppLabelEntity>

    @Insert
    suspend fun insertAll(rows: List<AppLabelEntity>)

    @Query("DELETE FROM app_label")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(rows: List<AppLabelEntity>) {
        clear()
        insertAll(rows)
    }
}
