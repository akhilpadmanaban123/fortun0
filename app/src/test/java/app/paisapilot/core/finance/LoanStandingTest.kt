package app.paisapilot.core.finance

import app.paisapilot.core.model.LoanType
import app.paisapilot.core.model.PrepayPolicy
import app.paisapilot.data.local.entity.Loan
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanStandingTest {

    private val today = LocalDate.of(2026, 10, 11)

    /** A loan whose EMI is [emi] paise and whose next EMI falls on [nextDue]. */
    private fun loan(emi: Long, nextDue: LocalDate = today.plusDays(10), paid: Int = 2, months: Int = 24) = Loan(
        name = "Loan", type = LoanType.PERSONAL, principalMinor = emi * months, annualRatePct = 0.0,
        tenureMonths = months, emiMinor = emi, firstEmiEpochDay = nextDue.minusMonths(paid.toLong()).toEpochDay(),
        paidInstallments = paid,
    ).status()

    private fun rank(income: Long, vararg loans: LoanStatus) = LoanStandings.of(loans.toList(), income, today)?.rank

    @Test
    fun `no loans means no standing`() {
        assertNull(LoanStandings.of(emptyList(), 8_000_000, today))
    }

    @Test
    fun `rank follows the share of income going to emis`() {
        assertEquals("Strong", rank(10_000_000, loan(2_000_000)))
        assertEquals("Steady", rank(10_000_000, loan(4_000_000)))
        assertEquals("Stretched", rank(10_000_000, loan(6_000_000)))
        assertEquals("On time", rank(0, loan(6_000_000)))
    }

    @Test
    fun `an overdue emi outranks everything else`() {
        val standing = LoanStandings.of(listOf(loan(1_000_000), loan(500_000, nextDue = today.minusDays(3))), 10_000_000, today)!!
        assertEquals("Behind", standing.rank)
        assertEquals(Mood.WORRIED, standing.mood)
        assertTrue(standing.headline.startsWith("1 EMI"))
    }

    @Test
    fun `an emi due today is not overdue`() {
        assertEquals("Strong", rank(10_000_000, loan(1_000_000, nextDue = today)))
    }

    @Test
    fun `all loans repaid is debt-free`() {
        assertEquals("Debt-free", rank(10_000_000, loan(1_000_000, paid = 24)))
    }

    @Test
    fun `payoff ideas respect the lender's part-payment rules`() {
        fun personal(policy: PrepayPolicy, chargePct: Double) = Loan(
            name = "Loan", type = LoanType.PERSONAL, principalMinor = 12_000_000, annualRatePct = 10.5,
            tenureMonths = 24, emiMinor = EmiCalculator.emi(12_000_000, 10.5, 24),
            firstEmiEpochDay = today.toEpochDay(), prepayPolicy = policy, prepayChargePct = chargePct,
        ).status()

        assertTrue(PayoffIdeas.forLoan(personal(PrepayPolicy.NOT_ALLOWED, 0.0)).isEmpty())

        val free = PayoffIdeas.forLoan(personal(PrepayPolicy.ALLOWED, 0.0)).first()
        val charged = PayoffIdeas.forLoan(personal(PrepayPolicy.ALLOWED, 2.0)).first()
        assertEquals(0L, free.chargeMinor)
        // 2% of one extra EMI.
        assertEquals(charged.extraMinor * 2 / 100, charged.chargeMinor)
        assertEquals(free.interestSavedMinor - charged.chargeMinor, charged.netSavedMinor)

        // A charge bigger than the interest it saves is not worth suggesting.
        assertTrue(PayoffIdeas.forLoan(personal(PrepayPolicy.ALLOWED, 50.0)).isEmpty())
    }
}
