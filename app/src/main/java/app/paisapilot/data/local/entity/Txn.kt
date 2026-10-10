package app.paisapilot.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.paisapilot.core.model.TxnType
import kotlinx.serialization.Serializable

/**
 * A money movement.
 * - EXPENSE: paid from [accountId], or charged to [cardId].
 * - INCOME: received into [accountId].
 * - TRANSFER: from [accountId] to [toAccountId], or to [cardId] when paying a card bill.
 */
@Serializable
@Entity(tableName = "transactions", indices = [Index("dateEpochDay")])
data class Txn(
    @PrimaryKey override val id: String = newId(),
    val type: TxnType,
    val amountMinor: Long,
    /** A [app.paisapilot.core.model.TxnCategory] name. */
    val category: String,
    val accountId: String? = null,
    val cardId: String? = null,
    val toAccountId: String? = null,
    val note: String = "",
    val dateEpochDay: Long,
    val createdAt: Long = now(),
    /** Set when this transaction is an EMI or bill payment. */
    val loanId: String? = null,
    val billId: String? = null,
    override val updatedAt: Long = now(),
    override val deleted: Boolean = false,
    override val dirty: Boolean = true,
) : Syncable
