package app.paisapilot.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.paisapilot.core.model.LoanType
import kotlinx.serialization.Serializable

/**
 * A loan or EMI plan. The repayment schedule is derived from these terms
 * (see core/finance/LoanMath.kt) rather than stored, so it can never go stale.
 */
@Serializable
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey override val id: String = newId(),
    val name: String,
    val type: LoanType,
    val lender: String = "",
    val principalMinor: Long,
    val annualRatePct: Double,
    val tenureMonths: Int,
    val emiMinor: Long,
    val firstEmiEpochDay: Long,
    val paidInstallments: Int = 0,
    /** Account the EMI is debited from, if the user linked one. */
    val accountId: String? = null,
    val autopay: Boolean = false,
    val closed: Boolean = false,
    override val updatedAt: Long = now(),
    override val deleted: Boolean = false,
    override val dirty: Boolean = true,
) : Syncable
