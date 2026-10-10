package app.paisapilot.core.finance

import kotlin.math.roundToInt

/** A friendly reading of someone's money situation, built from their profile. */
data class MoneyPersona(
    val title: String,
    val summary: String,
    val nextStep: String,
    /** A line about where they are in life, when their age is known. */
    val lifeStage: String?,
)

/**
 * Turns income, EMIs, age and how in control the user feels into a money personality.
 * It is deliberately rule-based: it works offline, costs nothing and is easy to explain.
 * The premium AI coach builds on the same inputs.
 */
object MoneyPersonas {

    private const val LOW_CONFIDENCE = 2
    private const val HIGH_CONFIDENCE = 4

    fun of(incomeMinor: Long, emiMinor: Long, age: Int, confidence: Int): MoneyPersona {
        val stage = lifeStage(age)
        if (incomeMinor <= 0) {
            return MoneyPersona(
                title = "The Newcomer",
                summary = "You are just getting started here.",
                nextStep = "Add your monthly income so your money picture can take shape.",
                lifeStage = stage,
            )
        }
        val emiPct = (emiMinor * 100.0 / incomeMinor).roundToInt()
        val share = "$emiPct% of your income goes to EMIs."
        return when {
            emiPct > 50 -> MoneyPersona(
                "The Heavy Lifter",
                "$share You are carrying a lot, and still standing.",
                "Put any spare rupee towards the costliest loan, and avoid new EMIs until this drops under 40%.",
                stage,
            )
            emiPct > 30 -> MoneyPersona(
                "The Balancer",
                "$share Manageable, with little room for surprises.",
                "Build a buffer of three months of EMIs before taking on anything new.",
                stage,
            )
            emiMinor == 0L && confidence in 1..LOW_CONFIDENCE -> MoneyPersona(
                "The Careful Starter",
                "No EMIs weighing on you, but money still feels uncertain.",
                "Track one month of spending. Seeing where it goes is the quickest way to feel in control.",
                stage,
            )
            emiMinor == 0L -> MoneyPersona(
                "The Free Bird",
                "No EMIs at all. Your income is entirely yours to direct.",
                "Give that freedom a job: automate a monthly investment on payday.",
                stage,
            )
            confidence in 1..LOW_CONFIDENCE -> MoneyPersona(
                "The Quiet Achiever",
                "$share Your numbers are healthier than they feel to you.",
                "Check your safe-to-spend figure each week. The facts are on your side.",
                stage,
            )
            confidence >= HIGH_CONFIDENCE -> MoneyPersona(
                "The Captain",
                "$share Comfortable debt, and a steady hand on the wheel.",
                "Aim higher: push your savings past 20% of income and prepay when you can.",
                stage,
            )
            else -> MoneyPersona(
                "The Steady Builder",
                "$share You are on solid ground and moving forward.",
                "Keep EMIs under 30% of income and grow an emergency fund of six months.",
                stage,
            )
        }
    }

    private fun lifeStage(age: Int): String? = when {
        age <= 0 -> null
        age < 30 -> "In your twenties, time is your biggest asset. Small amounts invested now grow the most."
        age < 45 -> "These are peak earning and peak responsibility years. Protect your income with term and health cover."
        age < 60 -> "Retirement is within sight. Clearing loans before it arrives matters more than new ones."
        else -> "Steady income and low risk come first now. Keep debts minimal and medical cover strong."
    }
}
