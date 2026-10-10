package app.paisapilot.core.premium

enum class Tier { FREE, PREMIUM }

/**
 * Single source of truth for what is free and what is premium.
 * Moving a feature between tiers is a one-word change here.
 */
enum class Feature(val title: String, val description: String, val tier: Tier) {
    INSURANCE_AI(
        "Insurance vault with AI",
        "Upload your policies and get a plain-language summary: what is covered, what is not, and exactly when and how you can claim.",
        Tier.PREMIUM,
    ),
    AI_COACH(
        "AI money coach",
        "Your avatar comes alive as a personal coach that knows your profile, loans and habits. Ask it anything, from \"can I afford this?\" to \"which loan do I close first?\".",
        Tier.PREMIUM,
    ),
    STATEMENT_AI(
        "Statement analysis with AI",
        "Attach the monthly loan or card statement from your email. AI reads it, checks it against what you track here and flags charges, rate changes and mistakes.",
        Tier.PREMIUM,
    ),
    FUTURE_OUTLOOK(
        "5-year financial outlook",
        "See where your loans, savings and net worth are heading, and how you rank year by year.",
        Tier.PREMIUM,
    ),
    PAYOFF_PLANNER(
        "Loan payoff planner",
        "Find out how much interest and how many months a prepayment saves, and which loan to close first.",
        Tier.PREMIUM,
    ),
    BUDGETS(
        "Category budgets & alerts",
        "Set a limit for each spending category and get nudged before you cross it.",
        Tier.PREMIUM,
    ),
    REPORTS(
        "Reports & export",
        "Monthly PDF statements and Excel export of everything you track.",
        Tier.PREMIUM,
    ),
    FAMILY(
        "Family sharing",
        "Manage household money together with a shared view for your partner or parents.",
        Tier.PREMIUM,
    ),
    ;

    companion object {
        val premium: List<Feature> get() = entries.filter { it.tier == Tier.PREMIUM }
    }
}
