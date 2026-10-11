package app.paisapilot.core.model

/** [liquid] accounts count towards money you can actually spend today. */
enum class AccountType(val label: String, val liquid: Boolean, val isCash: Boolean = false) {
    CASH("Cash in hand", true, isCash = true),
    SALARY("Salary account", true),
    SAVINGS("Savings account", true),
    CURRENT("Current account", true),
    WALLET("Wallet / UPI Lite", true),
    FIXED_DEPOSIT("Fixed / recurring deposit", false),
    INVESTMENT("Investments", false),
    OTHER("Other", true),
}

enum class LoanType(val label: String) {
    PERSONAL("Personal loan"),
    HOME("Home loan"),
    BIKE("Bike loan"),
    CAR("Car loan"),
    EDUCATION("Education loan"),
    GOLD("Gold loan"),
    CONSUMER("Phone / appliance EMI"),
    CREDIT_CARD_EMI("Credit card EMI"),
    OTHER("Other EMI"),
}

/**
 * How often a bill comes round. Listed in the order shown to the user: every 1 to 9 months
 * (electricity is billed every 2 months in several states), then yearly.
 *
 * Bills are stored and synced by these names, so never rename one; QUARTERLY and
 * HALF_YEARLY keep their original names for that reason.
 */
enum class BillFrequency(val label: String, val months: Int) {
    ONE_TIME("One time", 0),
    MONTHLY("Every month", 1),
    EVERY_2_MONTHS("Every 2 months", 2),
    QUARTERLY("Every 3 months", 3),
    EVERY_4_MONTHS("Every 4 months", 4),
    EVERY_5_MONTHS("Every 5 months", 5),
    HALF_YEARLY("Every 6 months", 6),
    EVERY_7_MONTHS("Every 7 months", 7),
    EVERY_8_MONTHS("Every 8 months", 8),
    EVERY_9_MONTHS("Every 9 months", 9),
    YEARLY("Every year", 12),
}

enum class BillCategory(val label: String) {
    RENT("Rent"),
    ELECTRICITY("Electricity"),
    WATER("Water"),
    GAS("Gas"),
    MOBILE("Mobile recharge"),
    INTERNET("Internet / Wi-Fi"),
    SUBSCRIPTION("Subscription"),
    HEALTH_INSURANCE("Health insurance"),
    LIFE_INSURANCE("Life / term insurance"),
    VEHICLE_INSURANCE("Vehicle insurance"),
    SCHOOL_FEES("School / tuition fees"),
    MAINTENANCE("Society maintenance"),
    SIP("SIP / investment"),
    OTHER("Other"),
    ;

    val isInsurance: Boolean
        get() = this == HEALTH_INSURANCE || this == LIFE_INSURANCE || this == VEHICLE_INSURANCE
}

enum class TxnType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer"),
}

enum class TxnCategory(val label: String, val type: TxnType) {
    SALARY("Salary", TxnType.INCOME),
    FREELANCE("Freelance / business", TxnType.INCOME),
    INTEREST("Interest / dividends", TxnType.INCOME),
    REFUND("Refund / cashback", TxnType.INCOME),
    GIFT("Gift received", TxnType.INCOME),
    OTHER_INCOME("Other income", TxnType.INCOME),

    FOOD("Food & dining", TxnType.EXPENSE),
    GROCERIES("Groceries", TxnType.EXPENSE),
    TRANSPORT("Transport", TxnType.EXPENSE),
    FUEL("Fuel", TxnType.EXPENSE),
    SHOPPING("Shopping", TxnType.EXPENSE),
    RENT("Rent", TxnType.EXPENSE),
    UTILITIES("Bills & utilities", TxnType.EXPENSE),
    HEALTH("Health", TxnType.EXPENSE),
    ENTERTAINMENT("Entertainment", TxnType.EXPENSE),
    EDUCATION("Education", TxnType.EXPENSE),
    TRAVEL("Travel", TxnType.EXPENSE),
    FAMILY("Family & home", TxnType.EXPENSE),
    EMI("EMI payment", TxnType.EXPENSE),
    OTHER_EXPENSE("Other", TxnType.EXPENSE),

    TRANSFER("Transfer", TxnType.TRANSFER),
    CARD_PAYMENT("Card bill payment", TxnType.TRANSFER),
    ;

    companion object {
        fun of(type: TxnType): List<TxnCategory> = entries.filter { it.type == type }

        fun fromName(name: String): TxnCategory =
            entries.firstOrNull { it.name == name } ?: OTHER_EXPENSE
    }
}
