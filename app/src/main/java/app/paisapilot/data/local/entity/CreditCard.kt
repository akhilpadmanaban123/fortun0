package app.paisapilot.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "credit_cards")
data class CreditCard(
    @PrimaryKey override val id: String = newId(),
    val name: String,
    val issuer: String = "",
    val last4: String = "",
    val limitMinor: Long = 0,
    val outstandingMinor: Long = 0,
    /** Day of month the statement is generated. */
    val statementDay: Int = 1,
    /** Day of month the payment is due. */
    val dueDay: Int = 20,
    override val updatedAt: Long = now(),
    override val deleted: Boolean = false,
    override val dirty: Boolean = true,
) : Syncable {
    val availableMinor: Long get() = (limitMinor - outstandingMinor).coerceAtLeast(0)
    val utilisation: Float
        get() = if (limitMinor <= 0) 0f else (outstandingMinor.toFloat() / limitMinor).coerceIn(0f, 1f)
}
