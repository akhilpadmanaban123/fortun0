package app.paisapilot.core.finance

import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.roundToLong

data class Installment(
    val number: Int,
    val dueDate: LocalDate,
    val amountMinor: Long,
    val principalMinor: Long,
    val interestMinor: Long,
    /** What is still owed after this EMI and any extra payment made with it. */
    val balanceMinor: Long,
    /** Extra paid towards principal after this EMI. */
    val prepaidMinor: Long = 0,
)

/** Reducing-balance EMI maths, the method Indian banks and NBFCs use. */
object EmiCalculator {

    fun emi(principalMinor: Long, annualRatePct: Double, months: Int): Long {
        if (principalMinor <= 0 || months <= 0) return 0
        val r = annualRatePct / 12.0 / 100.0
        if (r <= 0.0) return (principalMinor.toDouble() / months).roundToLong()
        val factor = (1 + r).pow(months)
        return (principalMinor * r * factor / (factor - 1)).roundToLong()
    }

    /**
     * Full amortisation schedule. Uses the EMI the lender actually charges ([emiMinor]),
     * which may differ slightly from the formula; the last instalment absorbs the difference.
     *
     * [prepayments] maps "number of EMIs paid so far" to the extra amount paid at that point
     * (0 = before the first EMI). Each one cuts the balance directly; the EMI is unchanged,
     * so the schedule simply ends earlier.
     */
    fun schedule(
        principalMinor: Long,
        annualRatePct: Double,
        months: Int,
        emiMinor: Long,
        firstDue: LocalDate,
        prepayments: Map<Int, Long> = emptyMap(),
    ): List<Installment> {
        if (principalMinor <= 0 || months <= 0 || emiMinor <= 0) return emptyList()
        val r = annualRatePct / 12.0 / 100.0
        val rows = ArrayList<Installment>(months)
        var balance = principalMinor - (prepayments[0] ?: 0).coerceIn(0, principalMinor)
        if (balance == 0L) return rows
        for (i in 1..months) {
            val interest = (balance * r).roundToLong()
            var principalPart = (emiMinor - interest).coerceAtLeast(0)
            val last = i == months || principalPart >= balance
            if (last) principalPart = balance
            balance -= principalPart
            val extra = (prepayments[i] ?: 0).coerceIn(0, balance)
            balance -= extra
            rows += Installment(
                number = i,
                dueDate = firstDue.plusMonths((i - 1).toLong()),
                amountMinor = if (last) principalPart + interest else emiMinor,
                principalMinor = principalPart,
                interestMinor = interest,
                balanceMinor = balance,
                prepaidMinor = extra,
            )
            if (balance == 0L) break
        }
        return rows
    }
}
