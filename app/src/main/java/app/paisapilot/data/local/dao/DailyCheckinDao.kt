package app.paisapilot.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import app.paisapilot.data.local.entity.DailyCheckin
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyCheckinDao : SyncDao<DailyCheckin> {
    @Query("SELECT * FROM daily_checkins WHERE deleted = 0 AND dayEpochDay >= :from ORDER BY dayEpochDay")
    fun observeSince(from: Long): Flow<List<DailyCheckin>>

    @Query("SELECT * FROM daily_checkins WHERE id = :id")
    override suspend fun byId(id: String): DailyCheckin?

    @Query("SELECT * FROM daily_checkins WHERE dirty = 1")
    override suspend fun dirtyRows(): List<DailyCheckin>

    @Query("UPDATE daily_checkins SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    override suspend fun markClean(id: String, updatedAt: Long)

    @Query("UPDATE daily_checkins SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    override suspend fun softDelete(id: String, now: Long)
}
