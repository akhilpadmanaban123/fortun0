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
import app.paisapilot.data.prefs.ProfileStore
import app.paisapilot.data.prefs.UserProfile
import app.paisapilot.data.repository.TxnRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Posts one tracking reminder or money quote. Like due-date reminders it runs on the
 * device, with no server. A nudge always replaces the previous one, so they never pile up.
 */
@HiltWorker
class NudgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val profileStore: ProfileStore,
    private val txns: TxnRepository,
    private val notifier: Notifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val profile = profileStore.current()
        val test = inputData.getBoolean(KEY_TEST, false)
        // Only for people who have finished setting up, and never if they switched it off.
        if (!test && (!profile.nudges || !profile.onboarded)) return Result.success()

        val today = LocalDate.now().toEpochDay()
        val loggedToday = txns.observeRecent(1).first().firstOrNull()?.dateEpochDay == today
        // The daily summary already covers a day with spending logged; a quote on top would be noise.
        if (!test && loggedToday && profile.dailySummary) return Result.success()
        val nudge = Nudges.pick(profileStore.nextNudgeSequence(), loggedToday)
        notifier.show(key = NOTIFICATION_KEY, title = nudge.title, body = nudge.body, channel = Notifier.CHANNEL_NUDGES)
        return Result.success()
    }

    companion object {
        const val KEY_TEST = "test"
        private const val NOTIFICATION_KEY = "nudge"
    }
}

@Singleton
class NudgeScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Applies the user's choices. With [replace] false an existing schedule is left running,
     * which is what app start-up wants; settings changes pass true to reschedule.
     */
    fun apply(profile: UserProfile, replace: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!profile.nudges) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = LocalDateTime.now()
        var firstRun = now.toLocalDate().atTime(LocalTime.of(profile.nudgeHour.coerceIn(0, 23), 0))
        if (!firstRun.isAfter(now)) firstRun = firstRun.plusDays(1)
        val request = PeriodicWorkRequestBuilder<NudgeWorker>(profile.nudgeEveryDays.coerceAtLeast(1).toLong(), TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, firstRun).toMinutes(), TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Sends one straight away, for the "Send one now" button in Settings. */
    fun sendNow() {
        val request = OneTimeWorkRequestBuilder<NudgeWorker>()
            .setInputData(workDataOf(NudgeWorker.KEY_TEST to true))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private companion object {
        const val WORK_NAME = "nudges"
    }
}
