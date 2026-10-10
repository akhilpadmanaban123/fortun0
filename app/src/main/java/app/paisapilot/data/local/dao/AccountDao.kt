package app.paisapilot.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import app.paisapilot.data.local.entity.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao : SyncDao<Account> {
    @Query("SELECT * FROM accounts WHERE deleted = 0 ORDER BY type, name")
    fun observeAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    override suspend fun byId(id: String): Account?

    @Query("SELECT * FROM accounts WHERE dirty = 1")
    override suspend fun dirtyRows(): List<Account>

    @Query("UPDATE accounts SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    override suspend fun markClean(id: String, updatedAt: Long)

    @Query("UPDATE accounts SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    override suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE accounts SET balanceMinor = balanceMinor + :delta, dirty = 1, updatedAt = :now WHERE id = :id")
    suspend fun adjustBalance(id: String, delta: Long, now: Long)
}
