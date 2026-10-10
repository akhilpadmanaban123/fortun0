package app.paisapilot.data.repository

import androidx.room.withTransaction
import app.paisapilot.core.model.TxnType
import app.paisapilot.data.local.AppDatabase
import app.paisapilot.data.local.entity.Txn
import app.paisapilot.data.local.entity.now
import app.paisapilot.data.sync.SyncScheduler
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Transactions, and the balance changes they cause. Recording or deleting a transaction
 * and adjusting the affected account / card happen in one database transaction, so
 * balances can never drift from the history.
 */
@Singleton
class TxnRepository @Inject constructor(
    private val db: AppDatabase,
    private val sync: SyncScheduler,
) {
    private val txnDao = db.txnDao()
    private val accountDao = db.accountDao()
    private val cardDao = db.creditCardDao()

    fun observeMonth(month: YearMonth): Flow<List<Txn>> =
        txnDao.observeBetween(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay())

    /** Every transaction from the first day of [from] to the last day of [to]. */
    fun observeMonths(from: YearMonth, to: YearMonth): Flow<List<Txn>> =
        txnDao.observeBetween(from.atDay(1).toEpochDay(), to.atEndOfMonth().toEpochDay())

    fun observeRecent(limit: Int): Flow<List<Txn>> = txnDao.observeRecent(limit)

    suspend fun add(txn: Txn) {
        db.withTransaction { addInTransaction(txn) }
        sync.requestSync()
    }

    /** For callers that are already inside a database transaction (EMI and bill payments). */
    suspend fun addInTransaction(txn: Txn) {
        txnDao.upsert(txn.copy(updatedAt = now(), dirty = true))
        applyToBalances(txn, direction = 1)
    }

    suspend fun delete(id: String) {
        db.withTransaction {
            val txn = txnDao.byId(id)?.takeUnless { it.deleted } ?: return@withTransaction
            txnDao.softDelete(id, now())
            applyToBalances(txn, direction = -1)
            // If this expense was an extra loan payment, the loan's principal goes back up too.
            db.loanPrepaymentDao().softDeleteByTxn(id, now())
        }
        sync.requestSync()
    }

    /** [direction] is +1 to apply the transaction and -1 to undo it. */
    private suspend fun applyToBalances(txn: Txn, direction: Int) {
        val amount = txn.amountMinor * direction
        val stamp = now()
        when (txn.type) {
            TxnType.INCOME -> txn.accountId?.let { accountDao.adjustBalance(it, amount, stamp) }
            TxnType.EXPENSE -> when {
                txn.cardId != null -> cardDao.adjustOutstanding(txn.cardId, amount, stamp)
                txn.accountId != null -> accountDao.adjustBalance(txn.accountId, -amount, stamp)
            }
            TxnType.TRANSFER -> {
                txn.accountId?.let { accountDao.adjustBalance(it, -amount, stamp) }
                txn.toAccountId?.let { accountDao.adjustBalance(it, amount, stamp) }
                txn.cardId?.let { cardDao.adjustOutstanding(it, -amount, stamp) }
            }
        }
    }
}
