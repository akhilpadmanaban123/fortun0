package app.paisapilot.core.finance

import app.paisapilot.core.util.toLocalDate
import app.paisapilot.data.local.entity.Loan
import app.paisapilot.data.local.entity.LoanPrepayment
import java.time.LocalDate

/** Everything derived from a loan's terms, how many EMIs have been paid, and any extra payments. */
data class LoanStatus(
    val loan: Loan,
    val schedule: List<Installment>,
    val prepayments: List<LoanPrepayment> = emptyList(),
) {
    val totalInstallments: Int get() = schedule.size
    val paid: Int get() = loan.paidInstallments.coerceIn(0, totalInstallments)
    val remaining: Int get() = totalInstallments - paid
    val isActive: Boolean get() = !loan.closed && remaining > 0
    val next: Installment? get() = if (isActive) schedule[paid] else null
    val nextDueDate: LocalDate? get() = next?.dueDate
    val prepaidMinor: Long get() = prepayments.sumOf { it.amountMinor }
    val outstandingMinor: Long
        get() = when {
            !isActive -> 0
            // Nothing paid yet: the loan amount, less anything paid extra up front.
            paid == 0 -> (loan.principalMinor - prepayments.filter { it.afterInstallment <= 0 }.sumOf { it.amountMinor })
                .coerceAtLeast(0)
            else -> schedule[paid - 1].balanceMinor
        }
    val progress: Float
        get() = if (totalInstallments == 0) 1f else paid.toFloat() / totalInstallments
    val totalInterestMinor: Long get() = schedule.sumOf { it.interestMinor }
    val interestRemainingMinor: Long get() = schedule.drop(paid).sumOf { it.interestMinor }
    val endDate: LocalDate? get() = schedule.lastOrNull()?.dueDate
}

/**
 * [prepayments] may be the user's full list; only this loan's are used.
 * Extra payments recorded at the same point are added together.
 */
fun Loan.status(prepayments: List<LoanPrepayment> = emptyList()): LoanStatus {
    val mine = prepayments.filter { it.loanId == id }
    return LoanStatus(
        loan = this,
        schedule = EmiCalculator.schedule(
            principalMinor = principalMinor,
            annualRatePct = annualRatePct,
            months = tenureMonths,
            emiMinor = emiMinor,
            firstDue = firstEmiEpochDay.toLocalDate(),
            prepayments = mine.groupBy { it.afterInstallment.coerceAtLeast(0) }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } },
        ),
        prepayments = mine,
    )
}
