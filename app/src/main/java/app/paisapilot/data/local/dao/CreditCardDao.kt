package app.paisapilot.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import app.paisapilot.data.local.entity.CreditCard
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao : SyncDao<CreditCard> {
    @Query("SELECT * FROM credit_cards WHERE deleted = 0 ORDER BY name")
    fun observeAll(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    override suspend fun byId(id: String): CreditCard?

    @Query("SELECT * FROM credit_cards WHERE dirty = 1")
    override suspend fun dirtyRows(): List<CreditCard>

    @Query("UPDATE credit_cards SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    override suspend fun markClean(id: String, updatedAt: Long)

    @Query("UPDATE credit_cards SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    override suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE credit_cards SET outstandingMinor = outstandingMinor + :delta, dirty = 1, updatedAt = :now WHERE id = :id")
    suspend fun adjustOutstanding(id: String, delta: Long, now: Long)
}
