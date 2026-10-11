package app.paisapilot.core.finance

import app.paisapilot.core.model.LoanType
import app.paisapilot.data.local.entity.Loan
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PayoffIdeasTest {

    private fun loan(rate: Double, paid: Int = 0, months: Int = 24, name: String = "Loan") = Loan(
        name = name, type = LoanType.PERSONAL, principalMinor = 12_000_000, annualRatePct = rate,
        tenureMonths = months, emiMinor = EmiCalculator.emi(12_000_000, rate, months).coerceAtLeast(500_000),
        firstEmiEpochDay = LocalDate.of(2026, 1, 10).toEpochDay(), paidInstallments = paid,
    )

    @Test
    fun `offers a one-time and a monthly idea that both save interest`() {
        val ideas = PayoffIdeas.forLoan(loan(10.5).status())
        assertEquals(2, ideas.size)
        assertEquals(false, ideas[0].everyMonth)
        assertEquals(true, ideas[1].everyMonth)
        ideas.forEach {
            assertTrue(it.interestSavedMinor > 0)
            assertTrue(it.emisSaved >= 1)
        }
    }

    @Test
    fun `the one-time idea is one extra emi and matches recording that payment`() {
        val status = loan(10.5, paid = 3).status()
        val idea = PayoffIdeas.forLoan(status).first()
        assertEquals(status.loan.emiMinor, idea.extraMinor)

        val after = EmiCalculator.schedule(
            12_000_000, 10.5, 24, status.loan.emiMinor, LocalDate.of(2026, 1, 10), mapOf(3 to status.loan.emiMinor),
        )
        assertEquals(status.totalInstallments - after.size, idea.emisSaved)
        assertEquals(status.totalInterestMinor - after.sumOf { it.interestMinor }, idea.interestSavedMinor)
    }

    @Test
    fun `the monthly top-up is about a tenth of the emi rounded to 500 rupees`() {
        val idea = PayoffIdeas.forLoan(loan(10.5).status()).last()
        assertEquals(0L, idea.extraMinor % 50_000)
        assertEquals(100_000L, idea.extraMinor) // EMI is about Rs 5,565, so Rs 1,000
    }

    @Test
    fun `no ideas for interest-free, nearly finished or closed loans`() {
        assertTrue(PayoffIdeas.forLoan(loan(0.0).status()).isEmpty())
        assertTrue(PayoffIdeas.forLoan(loan(10.5, paid = 22).status()).isEmpty())
        assertTrue(PayoffIdeas.forLoan(loan(10.5, paid = 24).status()).isEmpty())
    }

    @Test
    fun `pay first picks the highest rate and needs a real difference`() {
        val cheap = loan(8.5, name = "Home").status()
        val costly = loan(14.0, name = "Personal").status()
        val free = loan(0.0, name = "Phone").status()

        assertEquals("Personal", PayoffIdeas.payFirst(listOf(cheap, costly, free))?.loan?.name)
        assertNull(PayoffIdeas.payFirst(listOf(costly, free)))
        assertNull(PayoffIdeas.payFirst(listOf(cheap, loan(8.5, name = "Other").status())))
        assertNotNull(PayoffIdeas.payFirst(listOf(cheap, costly)))
    }
}
