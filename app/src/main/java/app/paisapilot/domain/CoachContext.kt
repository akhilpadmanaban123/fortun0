package app.paisapilot.domain

import app.paisapilot.core.finance.HealthReport
import app.paisapilot.core.finance.MoneyPersona
import app.paisapilot.core.finance.status
import app.paisapilot.core.model.TxnCategory
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.ordinal
import app.paisapilot.core.util.pretty
import app.paisapilot.core.util.toLocalDate

/**
 * Turns the user's finances into the short briefing the AI coach reads before answering.
 *
 * This is everything the AI is told about the user, so it is deliberately a summary:
 * names of loans, banks and bills and their amounts, but never account or card numbers,
 * email address, or individual transactions.
 */
object CoachContext {

    private const val MAX_ITEMS = 8
    private const val TOP_CATEGORIES = 5

    fun build(overview: Overview, health: HealthReport, persona: MoneyPersona): String = buildString {
        val p = overview.profile
        val s = overview.snapshot

        appendLine("Today: ${s.today.pretty()}")
        appendLine(
            "User: ${p.firstName.ifBlank { "not given" }}" +
                (if (p.age > 0) ", age ${p.age}" else "") +
                (if (p.confidence in 1..5) ", rates their confidence about money ${p.confidence}/5" else "") +
                ". Money personality: ${persona.title}."
        )
        appendLine(
            "Income: salary ${Money.format(p.monthlyIncomeMinor)} a month" +
                (if (p.payday in 1..31) " on the ${ordinal(p.payday)}" else "") +
                (if (p.extraIncomeMinor > 0) ", other income ${Money.format(p.extraIncomeMinor)} a month" else "") + "."
        )
        appendLine(
            "Balances: cash ${Money.format(s.cashMinor)}, bank accounts ${Money.format(s.bankMinor)}, " +
                "deposits and investments ${Money.format(s.lockedMinor)}. Net worth ${Money.format(s.netWorthMinor)}."
        )
        appendLine(
            "Safe to spend until payday (${s.nextPayday.pretty()}, ${s.daysToPayday} days): ${Money.format(s.safeToSpendMinor)}. " +
                "Dues before payday: ${Money.format(s.duesBeforePaydayMinor)}. " +
                "Expected month-end balance: ${Money.format(s.monthEndForecastMinor)}."
        )
        appendLine(
            "This month so far: income ${Money.format(s.monthIncomeMinor)}" +
                (if (s.incomeIsExpected) " (expected, not yet recorded)" else "") +
                ", spent ${Money.format(s.monthSpendMinor)}."
        )
        val top = s.spendByCategory.entries.sortedByDescending { it.value }.take(TOP_CATEGORIES)
        if (top.isNotEmpty()) {
            appendLine("Top spending this month: " + top.joinToString(", ") { "${TxnCategory.fromName(it.key).label} ${Money.format(it.value)}" } + ".")
        }

        val loans = overview.loans.map { it.status(overview.prepayments) }.filter { it.isActive }
        if (loans.isEmpty()) {
            appendLine("Loans: none.")
        } else {
            appendLine("Loans (${loans.size}), total EMIs ${Money.format(s.monthlyEmiMinor)} a month, total outstanding ${Money.format(s.loanOutstandingMinor)}:")
            loans.take(MAX_ITEMS).forEach { l ->
                appendLine(
                    "- ${l.loan.name} (${l.loan.type.label}" + (if (l.loan.lender.isNotBlank()) ", ${l.loan.lender}" else "") + "): " +
                        "outstanding ${Money.format(l.outstandingMinor)}, EMI ${Money.format(l.loan.emiMinor)}, " +
                        "${l.loan.annualRatePct}% a year, ${l.remaining} EMIs left, ends ${l.endDate?.pretty()}, " +
                        "interest still to pay ${Money.format(l.interestRemainingMinor)}."
                )
            }
        }

        if (overview.cards.isEmpty()) {
            appendLine("Credit cards: none.")
        } else {
            appendLine("Credit cards:")
            overview.cards.take(MAX_ITEMS).forEach { c ->
                appendLine("- ${c.name}: used ${Money.format(c.outstandingMinor)} of ${Money.format(c.limitMinor)} limit, bill due on the ${ordinal(c.dueDay)}.")
            }
        }

        val bills = overview.bills.filter { it.active }
        if (bills.isNotEmpty()) {
            appendLine("Bills:")
            bills.take(MAX_ITEMS).forEach { b ->
                appendLine("- ${b.name} (${b.category.label}): ${Money.format(b.amountMinor)}, ${b.frequency.label.lowercase()}, next due ${b.nextDueEpochDay.toLocalDate().pretty()}.")
            }
        }

        appendLine("Financial health score: ${health.score}/100 (${health.grade}). " + health.factors.joinToString("; ") { "${it.title} ${it.score}/${it.max}" } + ".")
    }.trim()
}
