package app.paisapilot.data.repository

import androidx.room.withTransaction
import app.paisapilot.core.finance.status
import app.paisapilot.core.model.TxnCategory
import app.paisapilot.core.model.TxnType
import app.paisapilot.data.local.AppDatabase
import app.paisapilot.data.local.entity.Loan
import app.paisapilot.data.local.entity.LoanPrepayment
import app.paisapilot.data.local.entity.Txn
import app.paisapilot.data.local.entity.now
import app.paisapilot.data.sync.SyncScheduler
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class LoanRepository @Inject constructor(
    private val db: AppDatabase,
    private val txns: TxnRepository,
    private val sync: SyncScheduler,
) {
    private val dao = db.loanDao()
    private val prepaymentDao = db.loanPrepaymentDao()

    val loans: Flow<List<Loan>> = dao.observeAll()

    /** Extra principal payments across all loans. */
    val prepayments: Flow<List<LoanPrepayment>> = prepaymentDao.observeAll()

    fun observe(id: String): Flow<Loan?> = dao.observe(id)

    fun observePrepayments(loanId: String): Flow<List<LoanPrepayment>> = prepaymentDao.observeForLoan(loanId)

    suspend fun loan(id: String): Loan? = dao.byId(id)?.takeUnless { it.deleted }

    suspend fun save(loan: Loan) {
        dao.upsert(loan.copy(updatedAt = now(), dirty = true))
        sync.requestSync()
    }

    suspend fun delete(id: String) {
        dao.softDelete(id, now())
        sync.requestSync()
    }

    /**
     * Marks the next EMI as paid. It is recorded as an expense, and debited from the
     * linked account when there is one. The loan closes itself after the final EMI.
     */
    suspend fun payNextInstallment(loanId: String, paidOn: LocalDate = LocalDate.now()) {
        db.withTransaction {
            val loan = loan(loanId) ?: return@withTransaction
            val status = loan.status(prepaymentDao.forLoan(loanId))
            val installment = status.next ?: return@withTransaction
            val paid = status.paid + 1
            dao.upsert(
                loan.copy(
                    paidInstallments = paid,
                    closed = paid >= status.totalInstallments,
                    updatedAt = now(),
                    dirty = true,
                )
            )
            txns.addInTransaction(
                Txn(
                    type = TxnType.EXPENSE,
                    amountMinor = installment.amountMinor,
                    category = TxnCategory.EMI.name,
                    accountId = loan.accountId,
                    note = "${loan.name} · EMI ${installment.number}/${status.totalInstallments}",
                    dateEpochDay = paidOn.toEpochDay(),
                    loanId = loan.id,
                )
            )
        }
        sync.requestSync()
    }

    /**
     * Records an extra payment that goes entirely to the principal. Anything above what is
     * still owed is ignored. Paying off the whole balance ends the schedule, which is what
     * marks the loan as finished; nothing is stored, so undoing the payment reopens it.
     * Returns the amount actually applied (0 when the loan is already finished).
     */
    suspend fun prepay(loanId: String, amountMinor: Long, paidOn: LocalDate = LocalDate.now()): Long {
        val applied = db.withTransaction {
            val loan = loan(loanId) ?: return@withTransaction 0L
            val status = loan.status(prepaymentDao.forLoan(loanId))
            val amount = amountMinor.coerceIn(0, status.outstandingMinor)
            if (amount == 0L) return@withTransaction 0L

            val expense = Txn(
                type = TxnType.EXPENSE,
                amountMinor = amount,
                category = TxnCategory.EMI.name,
                accountId = loan.accountId,
                note = "${loan.name} · extra payment to principal",
                dateEpochDay = paidOn.toEpochDay(),
                loanId = loan.id,
            )
            txns.addInTransaction(expense)
            prepaymentDao.upsert(
                LoanPrepayment(
                    loanId = loan.id,
                    amountMinor = amount,
                    dateEpochDay = paidOn.toEpochDay(),
                    afterInstallment = status.paid,
                    txnId = expense.id,
                )
            )
            amount
        }
        sync.requestSync()
        return applied
    }

    /** Undoes an extra payment: the principal goes back up and the recorded expense is removed. */
    suspend fun removePrepayment(prepaymentId: String) {
        val prepayment = prepaymentDao.byId(prepaymentId)?.takeUnless { it.deleted } ?: return
        prepaymentDao.softDelete(prepaymentId, now())
        // Deleting the expense also restores the account balance.
        prepayment.txnId?.let { txns.delete(it) }
        sync.requestSync()
    }
}
