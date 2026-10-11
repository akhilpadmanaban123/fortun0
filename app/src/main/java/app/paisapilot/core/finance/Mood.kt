package app.paisapilot.core.finance

import app.paisapilot.core.util.Money

/** How the user's money is doing right now, shown as a character on the Home screen. */
enum class Mood {
    /** Not enough has been entered to say anything yet. */
    THINKING,
    /** Spending comfortably below what is safe. */
    GREAT,
    /** On track. */
    HAPPY,
    /** Nothing is wrong yet, but the pace is too fast or most of the income is gone. */
    CAREFUL,
    /** Something needs attention: an overdue payment, or money about to run short. */
    WORRIED,
}

data class MoodReading(val mood: Mood, val headline: String, val detail: String)

/**
 * Reads a mood from the same figures the dashboard shows. The rules are checked from most
 * to least serious, and each reading states the figure that caused it, so the character's
 * expression is never a mystery.
 */
object MoodReader {

    /** Spending at or above this share of income is worth a caution. */
    private const val MOST_INCOME_SPENT = 0.8

    /** Spending at or below this share of the safe daily amount counts as doing great. */
    private const val COMFORTABLE_SHARE = 0.6

    fun read(s: MoneySnapshot): MoodReading {
        val pace = s.everydayPerDayMinor
        val safe = s.safePerDayMinor
        return when {
            s.liquidMinor == 0L && s.monthSpendMinor == 0L -> MoodReading(
                Mood.THINKING, "Let's get to know your money",
                "Add your accounts and a few expenses, and I'll tell you how you are doing.",
            )
            s.overdueCount > 0 -> MoodReading(
                Mood.WORRIED,
                if (s.overdueCount == 1) "A payment is overdue" else "${s.overdueCount} payments are overdue",
                "Clear overdue dues first; late fees and credit score damage add up quickly.",
            )
            s.safeToSpendMinor < 0 -> MoodReading(
                Mood.WORRIED, "Dues are more than your balance",
                "${Money.format(s.duesBeforePaydayMinor)} is due before payday and you hold ${Money.format(s.liquidMinor)}.",
            )
            s.monthEndForecastMinor < 0 -> MoodReading(
                Mood.WORRIED, "On course to run short",
                "At this pace you finish the month ${Money.format(-s.monthEndForecastMinor)} short. Hold back where you can.",
            )
            safe > 0 && pace > safe -> MoodReading(
                Mood.CAREFUL, "Spending faster than is safe",
                "You are spending ${Money.format(pace)} a day. ${Money.format(safe)} a day is safe until payday.",
            )
            s.monthIncomeMinor > 0 && s.monthSpendMinor >= s.monthIncomeMinor * MOST_INCOME_SPENT -> MoodReading(
                Mood.CAREFUL, "Most of this month's income is spent",
                "${Money.format(s.monthSpendMinor)} of ${Money.format(s.monthIncomeMinor)} has gone. Go gently until payday.",
            )
            safe > 0 && pace <= safe * COMFORTABLE_SHARE -> MoodReading(
                Mood.GREAT, "You are doing great",
                "Spending ${Money.format(pace)} a day when ${Money.format(safe)} a day is safe. Keep it up.",
            )
            else -> MoodReading(
                Mood.HAPPY, "You are on track",
                "${Money.format(safe)} a day is safe to spend until payday.",
            )
        }
    }
}
