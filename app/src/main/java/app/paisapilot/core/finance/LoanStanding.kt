package app.paisapilot.core.finance

import java.time.LocalDate
import kotlin.math.roundToInt

/** How the user is doing on their loans overall, shown at the top of the loans list. */
data class LoanStanding(
    val mood: Mood,
    /** One word the user can remember: Behind, Stretched, Steady, Strong, On time, Debt-free. */
    val rank: String,
    val headline: String,
    val detail: String,
)

/**
 * Ranks loan repayment from two things: whether every EMI is up to date, and how much of
 * the user's income EMIs take. Being behind always outranks everything else, because a
 * missed EMI costs money and credit score whatever the rest looks like.
 */
object LoanStandings {

    /** Null when the user has not added any loans. */
    fun of(statuses: List<LoanStatus>, monthlyIncomeMinor: Long, today: LocalDate): LoanStanding? {
        if (statuses.isEmpty()) return null
        val active = statuses.filter { it.isActive }
        val overdue = active.count { it.nextDueDate?.isBefore(today) == true }
        val emi = active.sumOf { it.loan.emiMinor }

        return when {
            overdue > 0 -> LoanStanding(
                Mood.WORRIED, "Behind",
                if (overdue == 1) "1 EMI is overdue" else "$overdue EMIs are overdue",
                "Pay it, or mark it as paid if you already have. Late EMIs add charges and pull your credit score down.",
            )
            active.isEmpty() -> LoanStanding(
                Mood.GREAT, "Debt-free", "Every loan is fully repaid", "Nothing left to pay here. Well done.",
            )
            monthlyIncomeMinor <= 0 -> LoanStanding(
                Mood.HAPPY, "On time", "All EMIs are up to date",
                "Add your income in your profile and I can also tell you how heavy your EMIs are.",
            )
            else -> {
                val pct = (emi * 100.0 / monthlyIncomeMinor).roundToInt()
                when {
                    pct > 50 -> LoanStanding(
                        Mood.CAREFUL, "Stretched", "Up to date, but EMIs take $pct% of your income",
                        "That leaves little room. Avoid new loans and put anything spare towards the costliest one.",
                    )
                    pct > 30 -> LoanStanding(
                        Mood.HAPPY, "Steady", "All EMIs are up to date",
                        "They take $pct% of your income. Under 30% is the comfortable zone.",
                    )
                    else -> LoanStanding(
                        Mood.GREAT, "Strong", "All EMIs are up to date",
                        "They take only $pct% of your income. You have room to pay extra and finish sooner.",
                    )
                }
            }
        }
    }
}
