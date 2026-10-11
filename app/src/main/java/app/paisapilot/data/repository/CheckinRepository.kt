package app.paisapilot.data.repository

import app.paisapilot.core.finance.DailySummary
import app.paisapilot.core.finance.Feeling
import app.paisapilot.data.local.dao.DailyCheckinDao
import app.paisapilot.data.local.entity.DailyCheckin
import app.paisapilot.data.sync.SyncScheduler
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** The user's daily "how do you feel about today's spending" answers. */
@Singleton
class CheckinRepository @Inject constructor(
    private val dao: DailyCheckinDao,
    private val sync: SyncScheduler,
) {
    fun observeSince(from: LocalDate): Flow<List<DailyCheckin>> = dao.observeSince(from.toEpochDay())

    /** Records (or changes) the answer for the summary's day. */
    suspend fun save(summary: DailySummary, feeling: Feeling) {
        val day = summary.date.toEpochDay()
        dao.upsert(
            DailyCheckin(
                id = day.toString(),
                dayEpochDay = day,
                feeling = feeling.name,
                everydayMinor = summary.everydayMinor,
                limitMinor = summary.limitMinor,
            )
        )
        sync.requestSync()
    }
}
