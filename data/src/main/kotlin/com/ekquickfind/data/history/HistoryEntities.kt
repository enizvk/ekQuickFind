package com.ekquickfind.data.history

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "launch_event", indices = [Index("resultId"), Index("at")])
data class LaunchEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val resultId: String,
    val providerId: String,
    val at: Long,
)

@Entity(tableName = "pin")
data class Pin(
    @PrimaryKey val resultId: String,
    val providerId: String,
    val position: Int,
)

data class LaunchAggregate(
    val resultId: String,
    val count: Int,
    val lastAt: Long,
)

data class RecentResult(
    val resultId: String,
    val providerId: String,
    val lastAt: Long,
)

@Dao
interface HistoryDao {
    @Insert
    suspend fun insertLaunch(event: LaunchEvent)

    @Query("SELECT resultId, COUNT(*) AS count, MAX(at) AS lastAt FROM launch_event GROUP BY resultId")
    suspend fun launchStats(): List<LaunchAggregate>

    @Query("SELECT * FROM launch_event WHERE resultId = :resultId")
    suspend fun launchesOf(resultId: String): List<LaunchEvent>

    @Query("SELECT resultId, providerId, MAX(at) AS lastAt FROM launch_event GROUP BY resultId ORDER BY lastAt DESC LIMIT :limit")
    fun recentResults(limit: Int): Flow<List<RecentResult>>

    @Query("DELETE FROM launch_event WHERE at < :cutoff")
    suspend fun pruneLaunchesBefore(cutoff: Long)

    @Query("DELETE FROM launch_event WHERE resultId = :resultId")
    suspend fun forgetResult(resultId: String)

    @Query("DELETE FROM launch_event")
    suspend fun clearLaunches()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun pin(pin: Pin)

    @Query("DELETE FROM pin WHERE resultId = :resultId")
    suspend fun unpin(resultId: String)

    @Query("SELECT * FROM pin ORDER BY position")
    fun pins(): Flow<List<Pin>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM pin")
    suspend fun nextPinPosition(): Int
}
