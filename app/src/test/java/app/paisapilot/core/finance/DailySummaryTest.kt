package app.paisapilot.core.finance

import app.paisapilot.core.model.TxnType
import app.paisapilot.data.local.entity.Txn
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailySummaryTest {

    private val today = LocalDate.of(2026, 10, 10)
    private val payday = LocalDate.of(2026, 10, 20) // 10 days away
    private val income = 8_000_000L

    /** [safeAfter] is what is safe to spend now, i.e. after today's spending has left the balance. */
    private fun snapshot(safeAfter: Long, nextPayday: LocalDate = payday) = MoneySnapshot(
        today = today, cashMinor = 0, bankMinor = safeAfter.coerceAtLeast(0), lockedMinor = 0,
        monthIncomeMinor = income, incomeIsExpected = false, monthSpendMinor = 0,
        nextPayday = nextPayday, duesBeforePayday = emptyList(),
        safeToSpendMinor = safeAfter, safePerDayMinor = 0, monthEndForecastMinor = 0,
        upcoming = emptyList(), monthlyEmiMinor = 0, loanOutstandingMinor = 0,
        cardOutstandingMinor = 0, cardLimitMinor = 0, spendByCategory = emptyMap(),
    )

    private fun spend(amount: Long, category: String = "FOOD", date: LocalDate = today) =
        Txn(type = TxnType.EXPENSE, amountMinor = amount, category = category, dateEpochDay = date.toEpochDay())

    @Test
    fun `the limit is what was safe at the start of the day spread over the days to payday`() {
        // Rs 4,380 safe now after spending Rs 620 means Rs 5,000 this morning: Rs 500 a day for 10 days.
        val s = DailySummary.of(snapshot(438_000), listOf(spend(62_000)), income)
        assertEquals(50_000L, s.limitMinor)
        assertEquals(DayVerdict.OVER, s.verdict)
        assertEquals("₹120 over today's limit", s.title)
        // Rs 4,380 over the 9 days after today.
        assertEquals(48_666L, s.tomorrowLimitMinor)
        assertTrue(s.body.contains("drops to"))
    }

    @Test
    fun `under the limit raises tomorrow's limit`() {
        val s = DailySummary.of(snapshot(480_000), listOf(spend(20_000)), income)
        assertEquals(DayVerdict.UNDER, s.verdict)
        assertEquals("₹300 under today's limit", s.title)
        assertTrue(s.body.contains("rises to"))
    }

    @Test
    fun `within ten percent of the limit counts as on it`() {
        val s = DailySummary.of(snapshot(452_000), listOf(spend(48_000)), income)
        assertEquals(DayVerdict.ON_LIMIT, s.verdict)
    }

    @Test
    fun `rent and large one-offs are reported but not measured against the limit`() {
        val s = DailySummary.of(snapshot(480_000), listOf(spend(20_000), spend(1_800_000, "RENT")), income)
        assertEquals(20_000L, s.everydayMinor)
        assertEquals(1_800_000L, s.otherMinor)
        assertEquals(DayVerdict.UNDER, s.verdict)
    }

    @Test
    fun `only today's expenses count`() {
        val s = DailySummary.of(snapshot(500_000), listOf(spend(90_000, date = today.minusDays(1))), income)
        assertEquals(DayVerdict.NOTHING_RECORDED, s.verdict)
        assertEquals(0L, s.totalMinor)
    }

    @Test
    fun `no safe amount gives no limit, and payday tomorrow gives no tomorrow limit`() {
        assertEquals(DayVerdict.NO_LIMIT, DailySummary.of(snapshot(-900_000), listOf(spend(20_000)), income).verdict)
        assertNull(DailySummary.of(snapshot(100_000, nextPayday = today.plusDays(1)), listOf(spend(20_000)), income).tomorrowLimitMinor)
    }
}
