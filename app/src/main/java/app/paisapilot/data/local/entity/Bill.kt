package app.paisapilot.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.paisapilot.core.model.BillCategory
import app.paisapilot.core.model.BillFrequency
import kotlinx.serialization.Serializable

/** A recurring (or one-off) payment: rent, recharge, insurance premium, SIP... */
@Serializable
@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey override val id: String = newId(),
    val name: String,
    val category: BillCategory,
    val amountMinor: Long,
    val frequency: BillFrequency = BillFrequency.MONTHLY,
    val nextDueEpochDay: Long,
    val accountId: String? = null,
    val autopay: Boolean = false,
    /** False once a one-time bill has been paid. */
    val active: Boolean = true,
    override val updatedAt: Long = now(),
    override val deleted: Boolean = false,
    override val dirty: Boolean = true,
) : Syncable
