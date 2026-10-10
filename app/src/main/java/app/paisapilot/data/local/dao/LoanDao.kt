package app.paisapilot.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import app.paisapilot.data.local.entity.Loan
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao : SyncDao<Loan> {
    @Query("SELECT * FROM loans WHERE deleted = 0 ORDER BY closed, name")
    fun observeAll(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id AND deleted = 0")
    fun observe(id: String): Flow<Loan?>

    @Query("SELECT * FROM loans WHERE id = :id")
    override suspend fun byId(id: String): Loan?

    @Query("SELECT * FROM loans WHERE dirty = 1")
    override suspend fun dirtyRows(): List<Loan>

    @Query("UPDATE loans SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    override suspend fun markClean(id: String, updatedAt: Long)

    @Query("UPDATE loans SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    override suspend fun softDelete(id: String, now: Long)
}
