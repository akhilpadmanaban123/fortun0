package app.paisapilot.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.paisapilot.core.finance.Due
import app.paisapilot.core.finance.DueKind
import app.paisapilot.core.finance.ForecastEngine
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.daysBetween
import app.paisapilot.core.util.dueLabel
import app.paisapilot.data.prefs.ProfileStore
import app.paisapilot.data.repository.BillRepository
import app.paisapilot.data.repository.LoanRepository
import app.paisapilot.data.repository.WalletRepository
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
 * Runs once a day and reminds the user about EMIs, bills and card payments that are
 * coming up, due today or overdue. It works entirely on the device: no server and no
 * internet connection are needed for due-date reminders.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val profileStore: ProfileStore,
    private val loans: LoanRepository,
    private val bills: BillRepository,
    private val wallet: WalletRepository,
    private val notifier: Notifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val profile = profileStore.current()
        if (!profile.reminders) return Result.success()

        val today = LocalDate.now()
        val dues = ForecastEngine.dues(
            today, loans.loans.first(), bills.bills.first(), wallet.cards.first(), loans.prepayments.first(),
        )
        dues.filter { shouldRemind(it, today, profile.reminderDaysBefore) }.forEach { due ->
            val kind = when (due.kind) {
                DueKind.EMI -> "EMI"
                DueKind.BILL -> "Bill"
                DueKind.CARD -> "Card payment"
            }
            val autopay = if (due.autopay) " Auto-pay is on: keep the balance ready." else ""
            notifier.show(
                key = "${due.kind}:${due.id}",
                title = "${due.title}: ${Money.format(due.amountMinor)}",
                body = "$kind · ${dueLabel(due.date, today)}.$autopay",
            )
        }
        return Result.success()
    }

    /** Remind [daysBefore] days ahead, the day before, on the day, and daily while overdue. */
    private fun shouldRemind(due: Due, today: LocalDate, daysBefore: Int): Boolean {
        val days = daysBetween(today, due.date)
        return days <= 1 || days == daysBefore
    }
}

@Singleton
class ReminderScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    /** Schedules the daily check for around 9 in the morning. Safe to call on every launch. */
    fun scheduleDaily() {
        val now = LocalDateTime.now()
        var firstRun = now.toLocalDate().atTime(LocalTime.of(REMINDER_HOUR, 0))
        if (!firstRun.isAfter(now)) firstRun = firstRun.plusDays(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, firstRun).toMinutes(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Checks immediately; used by "Check reminders now" in Settings. */
    fun runNow() {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<ReminderWorker>().build())
    }

    private companion object {
        const val DAILY_WORK = "daily-reminders"
        const val REMINDER_HOUR = 9
    }
}
