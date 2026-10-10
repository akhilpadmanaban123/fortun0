package app.paisapilot.data.repository

import androidx.room.withTransaction
import app.paisapilot.core.model.BillCategory
import app.paisapilot.core.model.BillFrequency
import app.paisapilot.core.model.TxnCategory
import app.paisapilot.core.model.TxnType
import app.paisapilot.core.util.toLocalDate
import app.paisapilot.data.local.AppDatabase
import app.paisapilot.data.local.entity.Bill
import app.paisapilot.data.local.entity.Txn
import app.paisapilot.data.local.entity.now
import app.paisapilot.data.sync.SyncScheduler
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class BillRepository @Inject constructor(
    private val db: AppDatabase,
    private val txns: TxnRepository,
    private val sync: SyncScheduler,
) {
    private val dao = db.billDao()

    val bills: Flow<List<Bill>> = dao.observeAll()

    suspend fun bill(id: String): Bill? = dao.byId(id)?.takeUnless { it.deleted }

    suspend fun save(bill: Bill) {
        dao.upsert(bill.copy(updatedAt = now(), dirty = true))
        sync.requestSync()
    }

    suspend fun delete(id: String) {
        dao.softDelete(id, now())
        sync.requestSync()
    }

    /** Records the payment as an expense and rolls the bill forward to its next due date. */
    suspend fun markPaid(billId: String, paidOn: LocalDate = LocalDate.now()) {
        db.withTransaction {
            val bill = bill(billId)?.takeIf { it.active } ?: return@withTransaction
            val oneTime = bill.frequency == BillFrequency.ONE_TIME
            val nextDue = bill.nextDueEpochDay.toLocalDate().plusMonths(bill.frequency.months.toLong())
            dao.upsert(
                bill.copy(
                    nextDueEpochDay = nextDue.toEpochDay(),
                    active = !oneTime,
                    updatedAt = now(),
                    dirty = true,
                )
            )
            txns.addInTransaction(
                Txn(
                    type = TxnType.EXPENSE,
                    amountMinor = bill.amountMinor,
                    category = bill.category.txnCategory().name,
                    accountId = bill.accountId,
                    note = bill.name,
                    dateEpochDay = paidOn.toEpochDay(),
                    billId = bill.id,
                )
            )
        }
        sync.requestSync()
    }

    private fun BillCategory.txnCategory(): TxnCategory = when (this) {
        BillCategory.RENT -> TxnCategory.RENT
        BillCategory.HEALTH_INSURANCE -> TxnCategory.HEALTH
        BillCategory.SCHOOL_FEES -> TxnCategory.EDUCATION
        BillCategory.SUBSCRIPTION -> TxnCategory.ENTERTAINMENT
        else -> TxnCategory.UTILITIES
    }
}
