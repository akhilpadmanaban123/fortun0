package app.paisapilot.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import app.paisapilot.data.local.entity.Txn
import kotlinx.coroutines.flow.Flow

@Dao
interface TxnDao : SyncDao<Txn> {
    @Query(
        "SELECT * FROM transactions WHERE deleted = 0 AND dateEpochDay BETWEEN :from AND :to " +
            "ORDER BY dateEpochDay DESC, createdAt DESC"
    )
    fun observeBetween(from: Long, to: Long): Flow<List<Txn>>

    @Query("SELECT * FROM transactions WHERE deleted = 0 ORDER BY dateEpochDay DESC, createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<Txn>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    override suspend fun byId(id: String): Txn?

    @Query("SELECT * FROM transactions WHERE dirty = 1")
    override suspend fun dirtyRows(): List<Txn>

    @Query("UPDATE transactions SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    override suspend fun markClean(id: String, updatedAt: Long)

    @Query("UPDATE transactions SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    override suspend fun softDelete(id: String, now: Long)
}
