package app.paisapilot.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * How the user felt about one day's spending, with that day's figures kept alongside
 * so the answer still makes sense later. One row per day; the id is the day itself.
 */
@Serializable
@Entity(tableName = "daily_checkins")
data class DailyCheckin(
    @PrimaryKey override val id: String,
    val dayEpochDay: Long,
    /** A [app.paisapilot.core.finance.Feeling] name. */
    val feeling: String,
    val everydayMinor: Long,
    val limitMinor: Long,
    override val updatedAt: Long = now(),
    override val deleted: Boolean = false,
    override val dirty: Boolean = true,
) : Syncable
