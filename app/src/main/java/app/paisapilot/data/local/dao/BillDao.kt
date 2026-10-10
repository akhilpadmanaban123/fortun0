package app.paisapilot.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import app.paisapilot.data.local.entity.Bill
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao : SyncDao<Bill> {
    @Query("SELECT * FROM bills WHERE deleted = 0 ORDER BY active DESC, nextDueEpochDay")
    fun observeAll(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE id = :id")
    override suspend fun byId(id: String): Bill?

    @Query("SELECT * FROM bills WHERE dirty = 1")
    override suspend fun dirtyRows(): List<Bill>

    @Query("UPDATE bills SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    override suspend fun markClean(id: String, updatedAt: Long)

    @Query("UPDATE bills SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    override suspend fun softDelete(id: String, now: Long)
}
