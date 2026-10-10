package app.paisapilot.core.finance

import app.paisapilot.core.model.AccountType
import app.paisapilot.core.model.BillCategory
import app.paisapilot.core.model.LoanType
import app.paisapilot.core.model.TxnType
import app.paisapilot.data.local.entity.Account
import app.paisapilot.data.local.entity.Bill
import app.paisapilot.data.local.entity.CreditCard
import app.paisapilot.data.local.entity.Loan
import app.paisapilot.data.local.entity.Txn
import app.paisapilot.data.prefs.UserProfile
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastEngineTest {

    private val today = LocalDate.of(2026, 10, 10)
    private val profile = UserProfile(monthlyIncomeMinor = 8_500_000, payday = 1)

    private val salary = Account(name = "Salary", type = AccountType.SALARY, balanceMinor = 6_200_000)
    private val cash = Account(name = "Cash", type = AccountType.CASH, balanceMinor = 300_000)
    private val deposit = Account(name = "FD", type = AccountType.FIXED_DEPOSIT, balanceMinor = 10_000_000)

    private fun loan(firstEmi: LocalDate, paid: Int = 0) = Loan(
        name = "Bike", type = LoanType.BIKE, principalMinor = 12_000_000, annualRatePct = 10.5,
        tenureMonths = 24, emiMinor = 556_512, firstEmiEpochDay = firstEmi.toEpochDay(), paidInstallments = paid,
    )

    private fun bill(due: LocalDate) = Bill(
        name = "Power", category = BillCategory.ELECTRICITY, amountMinor = 180_000, nextDueEpochDay = due.toEpochDay(),
    )

    private fun snapshot(
        accounts: List<Account> = listOf(salary, cash, deposit),
        cards: List<CreditCard> = emptyList(),
        loans: List<Loan> = emptyList(),
        bills: List<Bill> = emptyList(),
        txns: List<Txn> = emptyList(),
    ) = ForecastEngine.snapshot(today, profile, accounts, cards, loans, bills, txns)

    @Test
    fun `deposits are kept out of spendable money`() {
        val s = snapshot()
        assertEquals(300_000L, s.cashMinor)
        assertEquals(6_200_000L, s.bankMinor)
        assertEquals(10_000_000L, s.lockedMinor)
        assertEquals(6_500_000L, s.safeToSpendMinor)
    }

    @Test
    fun `safe to spend subtracts only what is due before payday`() {
        val s = snapshot(
            loans = listOf(loan(firstEmi = today.plusDays(5))),
            bills = listOf(bill(today.plusDays(2)), bill(LocalDate.of(2026, 11, 5))),
        )
        assertEquals(LocalDate.of(2026, 11, 1), s.nextPayday)
        assertEquals(22, s.daysToPayday)
        assertEquals(556_512L + 180_000L, s.duesBeforePaydayMinor)
        assertEquals(6_500_000L - 736_512L, s.safeToSpendMinor)
    }

    @Test
    fun `overdue items still count and are flagged`() {
        val s = snapshot(bills = listOf(bill(today.minusDays(3))))
        assertEquals(1, s.overdueCount)
        assertEquals(6_500_000L - 180_000L, s.safeToSpendMinor)
    }

    @Test
    fun `card outstanding is due on the card's next due day`() {
        val card = CreditCard(name = "Card", limitMinor = 10_000_000, outstandingMinor = 2_500_000, dueDay = 20)
        val s = snapshot(cards = listOf(card))
        val due = s.upcoming.single()
        assertEquals(DueKind.CARD, due.kind)
        assertEquals(LocalDate.of(2026, 10, 20), due.date)
        assertEquals(6_500_000L - 2_500_000L, s.safeToSpendMinor)
    }

    @Test
    fun `a closed loan produces no dues`() {
        val s = snapshot(loans = listOf(loan(firstEmi = today.minusMonths(30), paid = 24)))
        assertTrue(s.upcoming.isEmpty())
        assertEquals(0L, s.loanOutstandingMinor)
    }

    @Test
    fun `forecast projects everyday spending at the current pace`() {
        // Rs 4,500 of everyday spending over 10 days is Rs 450 a day, for the 21 days left.
        val spend = Txn(type = TxnType.EXPENSE, amountMinor = 450_000, category = "FOOD", dateEpochDay = today.toEpochDay())
        val s = snapshot(bills = listOf(bill(today.plusDays(2))), txns = listOf(spend))
        assertEquals(6_500_000L - 180_000L - 45_000L * 21, s.monthEndForecastMinor)
        assertEquals(450_000L, s.monthSpendMinor)
    }

    @Test
    fun `salary still to come this month is added to the forecast`() {
        val midMonth = profile.copy(payday = 25)
        val s = ForecastEngine.snapshot(today, midMonth, listOf(salary), emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(6_200_000L + 8_500_000L, s.monthEndForecastMinor)
        assertTrue(s.incomeIsExpected)
    }
}
