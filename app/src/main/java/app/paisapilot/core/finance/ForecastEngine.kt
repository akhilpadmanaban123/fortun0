package app.paisapilot.core.finance

import app.paisapilot.core.model.TxnCategory
import app.paisapilot.core.model.TxnType
import app.paisapilot.core.util.daysBetween
import app.paisapilot.core.util.nextDayOfMonth
import app.paisapilot.core.util.toLocalDate
import app.paisapilot.data.local.entity.Account
import app.paisapilot.data.local.entity.Bill
import app.paisapilot.data.local.entity.CreditCard
import app.paisapilot.data.local.entity.Loan
import app.paisapilot.data.local.entity.LoanPrepayment
import app.paisapilot.data.local.entity.Txn
import app.paisapilot.data.prefs.UserProfile
import java.time.LocalDate
import java.time.YearMonth

enum class DueKind { EMI, BILL, CARD }

/** One upcoming obligation: an EMI instalment, a bill, or a credit card payment. */
data class Due(
    val id: String,
    val kind: DueKind,
    val title: String,
    val subtitle: String,
    val amountMinor: Long,
    val date: LocalDate,
    val autopay: Boolean = false,
    /** The account this is paid from, when the user has chosen one. */
    val accountId: String? = null,
) {
    fun isOverdue(today: LocalDate) = date.isBefore(today)
}

data class MoneySnapshot(
    val today: LocalDate,
    val cashMinor: Long,
    val bankMinor: Long,
    val lockedMinor: Long,
    val monthIncomeMinor: Long,
    val incomeIsExpected: Boolean,
    val monthSpendMinor: Long,
    val nextPayday: LocalDate,
    val duesBeforePayday: List<Due>,
    val safeToSpendMinor: Long,
    val safePerDayMinor: Long,
    val monthEndForecastMinor: Long,
    /** The parts of the forecast, so the screen can show how the figure was reached. */
    val forecastIncomeMinor: Long = 0,
    val forecastDuesMinor: Long = 0,
    val forecastSpendMinor: Long = 0,
    /** Typical everyday spending per day so far this month; see [ForecastEngine.isEverydaySpend]. */
    val everydayPerDayMinor: Long = 0,
    val upcoming: List<Due>,
    val monthlyEmiMinor: Long,
    val loanOutstandingMinor: Long,
    val cardOutstandingMinor: Long,
    val cardLimitMinor: Long,
    val spendByCategory: Map<String, Long>,
) {
    val liquidMinor: Long get() = cashMinor + bankMinor
    val netWorthMinor: Long
        get() = liquidMinor + lockedMinor - loanOutstandingMinor - cardOutstandingMinor
    val remainingMinor: Long get() = monthIncomeMinor - monthSpendMinor
    val daysToPayday: Int get() = daysBetween(today, nextPayday).coerceAtLeast(1)
    val duesBeforePaydayMinor: Long get() = duesBeforePayday.sumOf { it.amountMinor }
    val overdueCount: Int get() = upcoming.count { it.isOverdue(today) }
}

/**
 * Pure calculations behind the dashboard. No Android or database types, so the
 * rules for "safe to spend" and the month-end forecast are easy to test and change.
 */
object ForecastEngine {

    private const val UPCOMING_WINDOW_DAYS = 45L

    /** Payments in these categories happen about once a month, however the user recorded them. */
    private val ONCE_A_MONTH = setOf(TxnCategory.RENT, TxnCategory.EMI, TxnCategory.UTILITIES, TxnCategory.EDUCATION)

    /** A single expense above this share of monthly income is treated as a one-off. */
    private const val ONE_OFF_INCOME_SHARE = 0.10

    /** The one-off threshold when income is not known: Rs 5,000. */
    private const val ONE_OFF_FALLBACK_MINOR = 500_000L

    /**
     * Whether an expense is the kind that repeats day to day (food, travel, small shopping)
     * and so is fair to project forward. Rent, EMIs, bills and large one-off payments are
     * not: having paid rent on the 3rd says nothing about what will be spent on the 20th.
     * Counting them as daily spending is what once made the forecast wildly negative.
     */
    fun isEverydaySpend(txn: Txn, monthlyIncomeMinor: Long): Boolean {
        if (txn.type != TxnType.EXPENSE || txn.loanId != null || txn.billId != null) return false
        if (TxnCategory.fromName(txn.category) in ONCE_A_MONTH) return false
        val oneOffAbove = if (monthlyIncomeMinor > 0) (monthlyIncomeMinor * ONE_OFF_INCOME_SHARE).toLong() else ONE_OFF_FALLBACK_MINOR
        return txn.amountMinor <= oneOffAbove
    }

    fun dues(
        today: LocalDate,
        loans: List<Loan>,
        bills: List<Bill>,
        cards: List<CreditCard>,
        prepayments: List<LoanPrepayment> = emptyList(),
    ): List<Due> {
        val list = ArrayList<Due>()
        loans.forEach { loan ->
            val status = loan.status(prepayments)
            val next = status.next ?: return@forEach
            list += Due(
                id = loan.id,
                kind = DueKind.EMI,
                title = loan.name,
                subtitle = "EMI ${next.number} of ${status.totalInstallments}",
                amountMinor = next.amountMinor,
                date = next.dueDate,
                autopay = loan.autopay,
                accountId = loan.accountId,
            )
        }
        bills.filter { it.active }.forEach { bill ->
            list += Due(
                id = bill.id,
                kind = DueKind.BILL,
                title = bill.name,
                subtitle = bill.category.label,
                amountMinor = bill.amountMinor,
                date = bill.nextDueEpochDay.toLocalDate(),
                autopay = bill.autopay,
                accountId = bill.accountId,
            )
        }
        cards.filter { it.outstandingMinor > 0 }.forEach { card ->
            list += Due(
                id = card.id,
                kind = DueKind.CARD,
                title = card.name,
                subtitle = "Credit card bill",
                amountMinor = card.outstandingMinor,
                date = nextDayOfMonth(card.dueDay, today),
            )
        }
        return list.sortedBy { it.date }
    }

    fun snapshot(
        today: LocalDate,
        profile: UserProfile,
        accounts: List<Account>,
        cards: List<CreditCard>,
        loans: List<Loan>,
        bills: List<Bill>,
        monthTxns: List<Txn>,
        prepayments: List<LoanPrepayment> = emptyList(),
    ): MoneySnapshot {
        val month = YearMonth.from(today)
        val monthEnd = month.atEndOfMonth()

        val cash = accounts.filter { it.type.isCash }.sumOf { it.balanceMinor }
        val bank = accounts.filter { it.type.liquid && !it.type.isCash }.sumOf { it.balanceMinor }
        val locked = accounts.filter { !it.type.liquid }.sumOf { it.balanceMinor }
        val liquid = cash + bank

        val recordedIncome = monthTxns.filter { it.type == TxnType.INCOME }.sumOf { it.amountMinor }
        val expenses = monthTxns.filter { it.type == TxnType.EXPENSE }
        val spend = expenses.sumOf { it.amountMinor }
        val everyday = expenses.filter { isEverydaySpend(it, profile.totalIncomeMinor) }.sumOf { it.amountMinor }

        val allDues = dues(today, loans, bills, cards, prepayments)
        val nextPayday = nextDayOfMonth(profile.paydayOrDefault, today, inclusive = false)
        val beforePayday = allDues.filter { it.date.isBefore(nextPayday) }
        val safe = liquid - beforePayday.sumOf { it.amountMinor }
        val daysToPayday = daysBetween(today, nextPayday).coerceAtLeast(1)

        // Month-end forecast: what you hold now, plus salary still to arrive this month,
        // minus what is still due this month, minus everyday spending at your current pace.
        val salaryStillDue = profile.monthlyIncomeMinor > 0 &&
            !nextPayday.isAfter(monthEnd) && recordedIncome < profile.monthlyIncomeMinor
        val incomeAhead = if (salaryStillDue) profile.monthlyIncomeMinor else 0L
        val duesThisMonth = allDues.filter { !it.date.isAfter(monthEnd) }.sumOf { it.amountMinor }
        val dailyBurn = everyday / today.dayOfMonth
        val daysLeft = daysBetween(today, monthEnd)
        val spendAhead = dailyBurn * daysLeft
        val forecast = liquid + incomeAhead - duesThisMonth - spendAhead

        val statuses = loans.map { it.status(prepayments) }.filter { it.isActive }

        return MoneySnapshot(
            today = today,
            cashMinor = cash,
            bankMinor = bank,
            lockedMinor = locked,
            monthIncomeMinor = if (recordedIncome > 0) recordedIncome else profile.monthlyIncomeMinor,
            incomeIsExpected = recordedIncome == 0L && profile.monthlyIncomeMinor > 0,
            monthSpendMinor = spend,
            nextPayday = nextPayday,
            duesBeforePayday = beforePayday,
            safeToSpendMinor = safe,
            safePerDayMinor = if (safe > 0) safe / daysToPayday else 0,
            monthEndForecastMinor = forecast,
            forecastIncomeMinor = incomeAhead,
            forecastDuesMinor = duesThisMonth,
            forecastSpendMinor = spendAhead,
            everydayPerDayMinor = dailyBurn,
            upcoming = allDues.filter { !it.date.isAfter(today.plusDays(UPCOMING_WINDOW_DAYS)) },
            monthlyEmiMinor = statuses.sumOf { it.loan.emiMinor },
            loanOutstandingMinor = statuses.sumOf { it.outstandingMinor },
            cardOutstandingMinor = cards.sumOf { it.outstandingMinor },
            cardLimitMinor = cards.sumOf { it.limitMinor },
            spendByCategory = expenses.groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } },
        )
    }
}
