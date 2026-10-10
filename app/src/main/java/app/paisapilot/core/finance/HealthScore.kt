package app.paisapilot.core.finance

import app.paisapilot.core.model.BillCategory
import app.paisapilot.core.util.Money
import app.paisapilot.data.local.entity.Bill
import kotlin.math.roundToInt

data class HealthFactor(
    val title: String,
    val score: Int,
    val max: Int,
    val detail: String,
    val tip: String,
)

data class HealthReport(
    val score: Int,
    val grade: String,
    val headline: String,
    val factors: List<HealthFactor>,
)

/**
 * A 0-100 snapshot of financial health built from five equally weighted factors.
 * The thresholds follow common personal-finance rules of thumb (EMIs under 30-40% of
 * income, card utilisation under 30%, six months of obligations as an emergency fund).
 */
object HealthScore {

    private const val FACTOR_MAX = 20

    fun evaluate(snapshot: MoneySnapshot, monthlyIncomeMinor: Long, bills: List<Bill>): HealthReport {
        val activeBills = bills.filter { it.active }
        val monthlyBills = activeBills.sumOf { bill ->
            if (bill.frequency.months == 0) 0L else bill.amountMinor / bill.frequency.months
        }
        val fixedOutflow = snapshot.monthlyEmiMinor + monthlyBills

        val factors = listOf(
            emiBurden(snapshot.monthlyEmiMinor, monthlyIncomeMinor),
            cardUtilisation(snapshot.cardOutstandingMinor, snapshot.cardLimitMinor),
            emergencyFund(snapshot.liquidMinor, fixedOutflow),
            savingsMargin(monthlyIncomeMinor, fixedOutflow, snapshot.monthSpendMinor),
            protection(activeBills),
        )
        val score = factors.sumOf { it.score }
        val (grade, headline) = when {
            score >= 85 -> "Excellent" to "You are in great financial shape."
            score >= 70 -> "Good" to "Solid footing, with a little room to improve."
            score >= 50 -> "Fair" to "Manageable, but a few areas need attention."
            score >= 30 -> "Stretched" to "Your money is under pressure. Start with the lowest score below."
            else -> "At risk" to "Focus on reducing dues and building a small buffer first."
        }
        return HealthReport(score, grade, headline, factors)
    }

    private fun emiBurden(emi: Long, income: Long): HealthFactor {
        val title = "EMI burden"
        if (income <= 0) return unknown(title, "Add your monthly income to measure this.")
        val pct = emi * 100.0 / income
        val score = band(pct, 0.0 to 20, 30.0 to 17, 40.0 to 12, 50.0 to 7, 60.0 to 3)
        return HealthFactor(
            title, score, FACTOR_MAX,
            detail = "${pct.roundToInt()}% of your income goes to EMIs (${Money.format(emi)}).",
            tip = if (pct <= 30) "Comfortable. Lenders like to see this under 40%."
            else "Try to bring EMIs under 40% of income: prepay the costliest loan first.",
        )
    }

    private fun cardUtilisation(outstanding: Long, limit: Long): HealthFactor {
        val title = "Credit card usage"
        if (limit <= 0) {
            return HealthFactor(
                title, 16, FACTOR_MAX,
                detail = "No credit cards added.",
                tip = "If you have cards, add them to track usage and due dates.",
            )
        }
        val pct = outstanding * 100.0 / limit
        val score = band(pct, 10.0 to 20, 30.0 to 16, 50.0 to 11, 75.0 to 6, 90.0 to 3)
        return HealthFactor(
            title, score, FACTOR_MAX,
            detail = "Using ${pct.roundToInt()}% of your total limit.",
            tip = if (pct <= 30) "Great. Staying under 30% helps your credit score."
            else "Pay cards down below 30% of the limit, and always pay the full bill.",
        )
    }

    private fun emergencyFund(liquid: Long, fixedOutflow: Long): HealthFactor {
        val title = "Emergency buffer"
        if (fixedOutflow <= 0) {
            val score = if (liquid > 0) 16 else 8
            return HealthFactor(
                title, score, FACTOR_MAX,
                detail = "You hold ${Money.format(liquid)} in cash and accounts.",
                tip = "Add your EMIs and bills so we can tell how many months this covers.",
            )
        }
        val months = liquid.toDouble() / fixedOutflow
        val score = when {
            months >= 6 -> 20
            months >= 3 -> 15
            months >= 1 -> 9
            months >= 0.5 -> 5
            else -> 2
        }
        return HealthFactor(
            title, score, FACTOR_MAX,
            detail = "Your balance covers ${"%.1f".format(months)} months of EMIs and bills.",
            tip = if (months >= 6) "Well protected against a rough patch."
            else "Aim for 6 months of EMIs and bills kept aside in a separate account.",
        )
    }

    private fun savingsMargin(income: Long, fixedOutflow: Long, spend: Long): HealthFactor {
        val title = "Savings margin"
        if (income <= 0) return unknown(title, "Add your monthly income to measure this.")
        // Month-to-date spend already includes any EMIs/bills paid, so take the larger view.
        val outflow = maxOf(fixedOutflow, spend)
        val pct = (income - outflow) * 100.0 / income
        val score = when {
            pct >= 30 -> 20
            pct >= 20 -> 16
            pct >= 10 -> 11
            pct >= 0 -> 6
            else -> 1
        }
        return HealthFactor(
            title, score, FACTOR_MAX,
            detail = if (pct >= 0) "About ${pct.roundToInt()}% of your income is left after outflows."
            else "Outflows exceed your income this month.",
            tip = if (pct >= 20) "Healthy. Put the surplus to work with a SIP or deposit."
            else "Try saving at least 20% of income. Move it out on payday itself.",
        )
    }

    private fun protection(bills: List<Bill>): HealthFactor {
        val categories = bills.map { it.category }.toSet()
        val hasHealth = BillCategory.HEALTH_INSURANCE in categories
        val hasLife = BillCategory.LIFE_INSURANCE in categories
        val score = when {
            hasHealth && hasLife -> 20
            hasHealth || hasLife -> 12
            else -> 4
        }
        return HealthFactor(
            title = "Insurance cover",
            score = score,
            max = FACTOR_MAX,
            detail = when {
                hasHealth && hasLife -> "Health and life cover are both being tracked."
                hasHealth -> "Health cover tracked. No life / term cover found."
                hasLife -> "Life cover tracked. No health cover found."
                else -> "No insurance premiums are being tracked."
            },
            tip = if (hasHealth && hasLife) "Review your cover amount once a year."
            else "Add your insurance premiums under Bills. A medical emergency is the most common way savings get wiped out.",
        )
    }

    private fun unknown(title: String, tip: String) =
        HealthFactor(title, FACTOR_MAX / 2, FACTOR_MAX, "Not enough information yet.", tip)

    /** Returns the score of the first band whose upper limit [value] does not exceed, else 0. */
    private fun band(value: Double, vararg bands: Pair<Double, Int>): Int =
        bands.firstOrNull { value <= it.first }?.second ?: 0
}
