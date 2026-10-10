package app.paisapilot.core.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyPersonaTest {

    private val income = 10_000_000L // Rs 1,00,000

    private fun title(emi: Long, confidence: Int = 3) = MoneyPersonas.of(income, emi, 30, confidence).title

    @Test
    fun `no income means there is nothing to judge yet`() {
        assertEquals("The Newcomer", MoneyPersonas.of(0, 500_000, 30, 3).title)
    }

    @Test
    fun `emi share of income decides the heavier personas`() {
        assertEquals("The Heavy Lifter", title(emi = 6_000_000))
        assertEquals("The Balancer", title(emi = 4_000_000))
        assertEquals("The Steady Builder", title(emi = 2_000_000))
    }

    @Test
    fun `how the user feels separates people with the same numbers`() {
        assertEquals("The Quiet Achiever", title(emi = 2_000_000, confidence = 1))
        assertEquals("The Captain", title(emi = 2_000_000, confidence = 5))
        assertEquals("The Careful Starter", title(emi = 0, confidence = 2))
        assertEquals("The Free Bird", title(emi = 0, confidence = 4))
    }

    @Test
    fun `an unanswered confidence question is treated as neutral`() {
        assertEquals("The Steady Builder", title(emi = 2_000_000, confidence = 0))
        assertEquals("The Free Bird", title(emi = 0, confidence = 0))
    }

    @Test
    fun `summary states the emi share and life stage follows age`() {
        val persona = MoneyPersonas.of(income, 2_500_000, 27, 3)
        assertTrue(persona.summary.startsWith("25% of your income"))
        assertNotNull(persona.lifeStage)
        assertNull(MoneyPersonas.of(income, 2_500_000, 0, 3).lifeStage)
    }
}
