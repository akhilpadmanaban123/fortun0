package app.paisapilot.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * An extra payment made on top of the EMIs, which goes straight to the principal.
 * The EMI stays the same, so the loan finishes sooner and costs less interest.
 */
@Serializable
@Entity(tableName = "loan_prepayments", indices = [Index("loanId")])
data class LoanPrepayment(
    @PrimaryKey override val id: String = newId(),
    val loanId: String,
    val amountMinor: Long,
    val dateEpochDay: Long,
    /** How many EMIs had been paid when this was made; it takes effect from the next one. */
    val afterInstallment: Int,
    /** The expense recorded for this payment, so removing one removes the other. */
    val txnId: String? = null,
    override val updatedAt: Long = now(),
    override val deleted: Boolean = false,
    override val dirty: Boolean = true,
) : Syncable
