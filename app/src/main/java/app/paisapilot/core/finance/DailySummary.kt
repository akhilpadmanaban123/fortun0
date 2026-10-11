package app.paisapilot.core.finance

import app.paisapilot.core.model.TxnType
import app.paisapilot.core.util.Money
import app.paisapilot.data.local.entity.Txn
import java.time.LocalDate

enum class DayVerdict { NOTHING_RECORDED, NO_LIMIT, UNDER, ON_LIMIT, OVER }

/** How the user said they felt about a day's spending. Stored by name; do not rename. */
enum class Feeling(val label: String, val reply: String) {
    GOOD("Happy with it", "Good. Days like this add up."),
    OKAY("It was okay", "Fair enough. Tomorrow is a fresh start."),
    REGRET("I overdid it", "It happens. One day does not decide the month."),
}

/**
 * One day's spending measured against that day's limit.
 *
 * The limit is the safe-to-spend amount spread evenly over the days left until payday, as it
 * stood at the start of the day. Only everyday spending is measured against it; rent, EMIs,
 * bills and large one-offs are reported separately because they are not what a daily limit is for.
 */
data class DailySummary(
    val date: LocalDate,
    val everydayMinor: Long,
    val otherMinor: Long,
    val limitMinor: Long,
    /** What the limit becomes tomorrow as a result of today; null when payday is tomorrow. */
    val tomorrowLimitMinor: Long?,
    val verdict: DayVerdict,
    val title: String,
    val body: String,
    val txns: List<Txn>,
) {
    val totalMinor: Long get() = everydayMinor + otherMinor

    companion object {
        /** Within this share either side of the limit counts as "on it". */
        private const val ON_LIMIT_BAND = 0.10

        fun of(snapshot: MoneySnapshot, monthTxns: List<Txn>, monthlyIncomeMinor: Long): DailySummary {
            val today = snapshot.today
            val txns = monthTxns.filter { it.type == TxnType.EXPENSE && it.dateEpochDay == today.toEpochDay() }
            val everyday = txns.filter { ForecastEngine.isEverydaySpend(it, monthlyIncomeMinor) }.sumOf { it.amountMinor }
            val other = txns.sumOf { it.amountMinor } - everyday

            // Today's spending has already left the balance, so add it back to see the limit the day began with.
            val limit = ((snapshot.safeToSpendMinor + everyday) / snapshot.daysToPayday).coerceAtLeast(0)
            val daysAfterToday = snapshot.daysToPayday - 1
            val tomorrow = if (daysAfterToday > 0) (snapshot.safeToSpendMinor / daysAfterToday).coerceAtLeast(0) else null
            val tomorrowNote = when {
                tomorrow == null -> " Payday is tomorrow."
                tomorrow > limit -> " Tomorrow's limit rises to ${Money.format(tomorrow)}."
                tomorrow < limit -> " Tomorrow's limit drops to ${Money.format(tomorrow)}."
                else -> " Tomorrow's limit stays at ${Money.format(tomorrow)}."
            }

            val verdict = when {
                txns.isEmpty() -> DayVerdict.NOTHING_RECORDED
                limit == 0L -> DayVerdict.NO_LIMIT
                everyday > limit * (1 + ON_LIMIT_BAND) -> DayVerdict.OVER
                everyday < limit * (1 - ON_LIMIT_BAND) -> DayVerdict.UNDER
                else -> DayVerdict.ON_LIMIT
            }
            val spentOfLimit = "You spent ${Money.format(everyday)} of your ${Money.format(limit)} limit."
            val (title, body) = when (verdict) {
                DayVerdict.NOTHING_RECORDED ->
                    "Nothing recorded today" to "A no-spend day, or something to add? Your limit was ${Money.format(limit)}."
                DayVerdict.NO_LIMIT ->
                    "${Money.format(everyday + other)} spent today" to
                        "Your dues before payday are more than your balance, so there was no safe amount to spend today."
                DayVerdict.OVER ->
                    "${Money.format(everyday - limit)} over today's limit" to spentOfLimit + tomorrowNote
                // Only bills or one-offs went out: say so, rather than praise a figure of zero.
                DayVerdict.UNDER if everyday == 0L ->
                    "No everyday spending today" to
                        "Only bills and one-off payments went out, and those are not counted against your ${Money.format(limit)} limit.$tomorrowNote"
                DayVerdict.UNDER ->
                    "${Money.format(limit - everyday)} under today's limit" to "$spentOfLimit Well done.$tomorrowNote"
                DayVerdict.ON_LIMIT ->
                    "Right on today's limit" to spentOfLimit + tomorrowNote
            }
            return DailySummary(today, everyday, other, limit, tomorrow, verdict, title, body, txns)
        }
    }
}
