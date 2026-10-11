package app.paisapilot.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.paisapilot.core.finance.DailySummary
import app.paisapilot.data.prefs.ProfileStore
import app.paisapilot.data.prefs.UserProfile
import app.paisapilot.domain.ObserveOverview
import app.paisapilot.navigation.Routes
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * The end-of-day notification: what was spent today against today's limit. Tapping it opens
 * the summary screen, which also asks how the user feels about the day. Runs on the device.
 */
@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val profileStore: ProfileStore,
    private val observeOverview: ObserveOverview,
    private val notifier: Notifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val profile = profileStore.current()
        val test = inputData.getBoolean(KEY_TEST, false)
        if (!test && (!profile.dailySummary || !profile.onboarded)) return Result.success()

        val overview = observeOverview().first()
        val summary = DailySummary.of(overview.snapshot, overview.monthTxns, overview.profile.totalIncomeMinor)
        notifier.show(
            key = NOTIFICATION_KEY,
            title = summary.title,
            body = summary.body + " Tap to see your day.",
            channel = Notifier.CHANNEL_NUDGES,
            destination = Routes.DAILY,
        )
        return Result.success()
    }

    companion object {
        const val KEY_TEST = "test"
        private const val NOTIFICATION_KEY = "daily-summary"
    }
}

@Singleton
class DailySummaryScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    /** Same contract as [NudgeScheduler.apply]: keep an existing schedule unless [replace]. */
    fun apply(profile: UserProfile, replace: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!profile.dailySummary) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = LocalDateTime.now()
        var firstRun = now.toLocalDate().atTime(LocalTime.of(profile.dailySummaryHour.coerceIn(0, 23), 0))
        if (!firstRun.isAfter(now)) firstRun = firstRun.plusDays(1)
        val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, firstRun).toMinutes(), TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun sendNow() {
        val request = OneTimeWorkRequestBuilder<DailySummaryWorker>()
            .setInputData(workDataOf(DailySummaryWorker.KEY_TEST to true))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private companion object {
        const val WORK_NAME = "daily-summary"
    }
}
