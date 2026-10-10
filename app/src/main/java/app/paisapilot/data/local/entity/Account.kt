package app.paisapilot.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.paisapilot.core.model.AccountType
import kotlinx.serialization.Serializable

/** Anywhere money is held: cash, a bank account, a wallet, a deposit. */
@Serializable
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey override val id: String = newId(),
    val name: String,
    val type: AccountType,
    val institution: String = "",
    val last4: String = "",
    val balanceMinor: Long = 0,
    /** What the user keeps this account for, e.g. "Salary + EMIs", "Emergency fund". */
    val purpose: String = "",
    override val updatedAt: Long = now(),
    override val deleted: Boolean = false,
    override val dirty: Boolean = true,
) : Syncable
