package app.paisapilot.core.finance

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodReaderTest {

    private val today = LocalDate.of(2026, 10, 10)

    /** A healthy month: Rs 50,000 in the bank, Rs 2,000 a day safe, Rs 1,500 a day being spent. */
    private fun snapshot(
        bank: Long = 5_000_000,
        income: Long = 8_000_000,
        spent: Long = 1_500_000,
        safeToSpend: Long = 4_400_000,
        safePerDay: Long = 200_000,
        pace: Long = 150_000,
        forecast: Long = 1_000_000,
        overdue: Boolean = false,
    ) = MoneySnapshot(
        today = today, cashMinor = 0, bankMinor = bank, lockedMinor = 0,
        monthIncomeMinor = income, incomeIsExpected = false, monthSpendMinor = spent,
        nextPayday = LocalDate.of(2026, 11, 1), duesBeforePayday = emptyList(),
        safeToSpendMinor = safeToSpend, safePerDayMinor = safePerDay, monthEndForecastMinor = forecast,
        upcoming = if (overdue) listOf(Due("x", DueKind.BILL, "Rent", "", 100, today.minusDays(2))) else emptyList(),
        monthlyEmiMinor = 0, loanOutstandingMinor = 0, cardOutstandingMinor = 0, cardLimitMinor = 0,
        spendByCategory = emptyMap(), everydayPerDayMinor = pace,
    )

    private fun mood(s: MoneySnapshot) = MoodReader.read(s).mood

    @Test
    fun `nothing entered yet is thinking`() {
        assertEquals(Mood.THINKING, mood(snapshot(bank = 0, spent = 0)))
    }

    @Test
    fun `on track is happy and well under the safe pace is great`() {
        assertEquals(Mood.HAPPY, mood(snapshot()))
        assertEquals(Mood.GREAT, mood(snapshot(pace = 100_000)))
    }

    @Test
    fun `spending above the safe pace or most of income gone is careful`() {
        assertEquals(Mood.CAREFUL, mood(snapshot(pace = 250_000)))
        assertEquals(Mood.CAREFUL, mood(snapshot(spent = 6_500_000)))
    }

    @Test
    fun `overdue dues, negative safe to spend or a negative forecast is worried`() {
        assertEquals(Mood.WORRIED, mood(snapshot(overdue = true)))
        assertEquals(Mood.WORRIED, mood(snapshot(safeToSpend = -100, safePerDay = 0)))
        assertEquals(Mood.WORRIED, mood(snapshot(forecast = -500_000)))
    }

    @Test
    fun `the most serious problem wins and the reading names the figure`() {
        val reading = MoodReader.read(snapshot(overdue = true, pace = 250_000, forecast = -500_000))
        assertEquals(Mood.WORRIED, reading.mood)
        assertTrue(reading.headline.contains("overdue"))
        assertTrue(MoodReader.read(snapshot(forecast = -500_000)).detail.contains("₹5,000"))
    }
}
