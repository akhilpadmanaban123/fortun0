package app.paisapilot.core.finance

import app.paisapilot.core.util.toLocalDate
import java.time.LocalDate

/** One worked example of paying a loan down faster, and what it would save. */
data class PayoffIdea(
    /** Extra paid towards principal: once now, or with every remaining EMI when [everyMonth]. */
    val extraMinor: Long,
    val everyMonth: Boolean,
    val emisSaved: Int,
    val interestSavedMinor: Long,
    val newEndDate: LocalDate?,
)

/**
 * Works out what paying extra towards a loan's principal would save. Every figure comes from
 * re-running the loan's own schedule with the extra payment added, so it matches what the
 * loan screen shows after the payment is really made.
 */
object PayoffIdeas {

    /** Below this many EMIs left there is too little interest remaining for extra payments to matter. */
    private const val MIN_REMAINING = 4
    private const val ROUND_TO_MINOR = 50_000L // ₹500

    fun forLoan(status: LoanStatus): List<PayoffIdea> {
        val loan = status.loan
        if (!status.isActive || loan.annualRatePct <= 0.0 || status.remaining < MIN_REMAINING) return emptyList()

        // A meaningful but realistic monthly top-up: about a tenth of the EMI, rounded to ₹500.
        val monthlyExtra = roundUp(loan.emiMinor / 10)
        return listOfNotNull(
            simulate(status, extraMinor = loan.emiMinor, everyMonth = false),
            simulate(status, extraMinor = monthlyExtra, everyMonth = true),
        ).filter { it.interestSavedMinor > 0 }
    }

    /** What a specific extra amount would save; used to preview a payment before it is recorded. */
    fun simulate(status: LoanStatus, extraMinor: Long, everyMonth: Boolean): PayoffIdea? {
        if (!status.isActive || extraMinor <= 0) return null
        val loan = status.loan
        val existing = status.prepayments.groupBy { it.afterInstallment.coerceAtLeast(0) }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
            .toMutableMap()
        val points = if (everyMonth) status.paid until status.totalInstallments else status.paid..status.paid
        for (point in points) existing[point] = (existing[point] ?: 0) + extraMinor

        val schedule = EmiCalculator.schedule(
            principalMinor = loan.principalMinor,
            annualRatePct = loan.annualRatePct,
            months = loan.tenureMonths,
            emiMinor = loan.emiMinor,
            firstDue = loan.firstEmiEpochDay.toLocalDate(),
            prepayments = existing,
        )
        return PayoffIdea(
            extraMinor = extraMinor,
            everyMonth = everyMonth,
            emisSaved = status.totalInstallments - schedule.size,
            interestSavedMinor = status.totalInterestMinor - schedule.sumOf { it.interestMinor },
            newEndDate = schedule.lastOrNull()?.dueDate,
        )
    }

    /**
     * The loan where extra money does the most good: the highest interest rate among running
     * loans. Null when there is nothing to choose between (fewer than two interest-bearing loans).
     */
    fun payFirst(statuses: List<LoanStatus>): LoanStatus? {
        val candidates = statuses.filter { it.isActive && it.loan.annualRatePct > 0.0 }
        if (candidates.size < 2) return null
        val best = candidates.maxBy { it.loan.annualRatePct }
        // If every loan charges the same rate there is no "first".
        return best.takeIf { candidates.any { it.loan.annualRatePct < best.loan.annualRatePct } }
    }

    private fun roundUp(amountMinor: Long): Long =
        ((amountMinor + ROUND_TO_MINOR - 1) / ROUND_TO_MINOR * ROUND_TO_MINOR).coerceAtLeast(ROUND_TO_MINOR)
}
