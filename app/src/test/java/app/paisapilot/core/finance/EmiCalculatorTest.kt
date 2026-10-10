package app.paisapilot.core.finance

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmiCalculatorTest {

    private val start = LocalDate.of(2026, 1, 31)

    @Test
    fun `emi matches the standard reducing-balance formula`() {
        // Rs 1,20,000 at 10.5% for 24 months is Rs 5,565.12 a month.
        assertEquals(556_512L, EmiCalculator.emi(12_000_000, 10.5, 24))
    }

    @Test
    fun `zero interest splits the principal evenly`() {
        assertEquals(500_000L, EmiCalculator.emi(6_000_000, 0.0, 12))
    }

    @Test
    fun `schedule repays exactly the principal and ends at zero`() {
        val emi = EmiCalculator.emi(12_000_000, 10.5, 24)
        val schedule = EmiCalculator.schedule(12_000_000, 10.5, 24, emi, start)

        assertEquals(24, schedule.size)
        assertEquals(12_000_000L, schedule.sumOf { it.principalMinor })
        assertEquals(0L, schedule.last().balanceMinor)
        // First month's interest is one month of 10.5% on the full amount.
        assertEquals(105_000L, schedule.first().interestMinor)
    }

    @Test
    fun `due dates stay on the same day and clamp in short months`() {
        val schedule = EmiCalculator.schedule(1_200_000, 12.0, 3, EmiCalculator.emi(1_200_000, 12.0, 3), start)
        assertEquals(LocalDate.of(2026, 2, 28), schedule[1].dueDate)
        assertEquals(LocalDate.of(2026, 3, 31), schedule[2].dueDate)
    }

    @Test
    fun `a higher emi than required closes the loan early`() {
        val schedule = EmiCalculator.schedule(1_000_000, 12.0, 12, 300_000, start)
        assertTrue(schedule.size < 12)
        assertEquals(0L, schedule.last().balanceMinor)
    }

    @Test
    fun `an extra payment cuts the principal and shortens the loan`() {
        val emi = EmiCalculator.emi(12_000_000, 10.5, 24)
        val plain = EmiCalculator.schedule(12_000_000, 10.5, 24, emi, start)
        // Rs 20,000 extra after the 3rd EMI.
        val prepaid = EmiCalculator.schedule(12_000_000, 10.5, 24, emi, start, mapOf(3 to 2_000_000L))

        assertEquals(plain[2].balanceMinor - 2_000_000, prepaid[2].balanceMinor)
        assertEquals(2_000_000L, prepaid[2].prepaidMinor)
        assertTrue(prepaid.size < plain.size)
        assertTrue(prepaid.sumOf { it.interestMinor } < plain.sumOf { it.interestMinor })
        // Everything paid, EMIs plus the extra, still adds up to the loan amount.
        assertEquals(12_000_000L, prepaid.sumOf { it.principalMinor + it.prepaidMinor })
        assertEquals(0L, prepaid.last().balanceMinor)
        // Earlier instalments are untouched.
        assertEquals(plain[1], prepaid[1])
    }

    @Test
    fun `an extra payment larger than the balance is capped and closes the loan`() {
        val emi = EmiCalculator.emi(1_200_000, 12.0, 12)
        val schedule = EmiCalculator.schedule(1_200_000, 12.0, 12, emi, start, mapOf(2 to 99_000_000L))
        assertEquals(2, schedule.size)
        assertEquals(0L, schedule.last().balanceMinor)
        assertEquals(1_200_000L, schedule.sumOf { it.principalMinor + it.prepaidMinor })
    }

    @Test
    fun `an extra payment before the first emi lowers the starting balance`() {
        val schedule = EmiCalculator.schedule(1_200_000, 12.0, 12, 106_619, start, mapOf(0 to 200_000L))
        // One month of 12% a year on Rs 10,000 is Rs 100.
        assertEquals(10_000L, schedule.first().interestMinor)
    }

    @Test
    fun `invalid terms give an empty schedule`() {
        assertTrue(EmiCalculator.schedule(0, 10.0, 12, 1000, start).isEmpty())
        assertEquals(0L, EmiCalculator.emi(100_000, 10.0, 0))
    }
}
