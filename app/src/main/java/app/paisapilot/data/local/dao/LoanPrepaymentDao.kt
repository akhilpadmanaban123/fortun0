package app.paisapilot.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import app.paisapilot.data.local.entity.LoanPrepayment
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanPrepaymentDao : SyncDao<LoanPrepayment> {
    @Query("SELECT * FROM loan_prepayments WHERE deleted = 0 ORDER BY dateEpochDay")
    fun observeAll(): Flow<List<LoanPrepayment>>

    @Query("SELECT * FROM loan_prepayments WHERE deleted = 0 AND loanId = :loanId ORDER BY dateEpochDay")
    fun observeForLoan(loanId: String): Flow<List<LoanPrepayment>>

    @Query("SELECT * FROM loan_prepayments WHERE deleted = 0 AND loanId = :loanId")
    suspend fun forLoan(loanId: String): List<LoanPrepayment>

    @Query("SELECT * FROM loan_prepayments WHERE id = :id")
    override suspend fun byId(id: String): LoanPrepayment?

    @Query("SELECT * FROM loan_prepayments WHERE dirty = 1")
    override suspend fun dirtyRows(): List<LoanPrepayment>

    @Query("UPDATE loan_prepayments SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    override suspend fun markClean(id: String, updatedAt: Long)

    @Query("UPDATE loan_prepayments SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    override suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE loan_prepayments SET deleted = 1, dirty = 1, updatedAt = :now WHERE txnId = :txnId AND deleted = 0")
    suspend fun softDeleteByTxn(txnId: String, now: Long)
}
