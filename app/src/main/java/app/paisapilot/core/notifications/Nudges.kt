package app.paisapilot.core.notifications

data class Nudge(val title: String, val body: String)

/**
 * The messages behind the optional daily nudge. They ship inside the app, so no server or
 * internet connection is involved. To change them, edit these lists and release an update.
 */
object Nudges {

    /** Prompts to record spending; only sent on days when nothing has been logged yet. */
    val reminders = listOf(
        Nudge("Don't wait until later!", "That ₹50 matters too. Track your expense now before you forget."),
        Nudge("Spent it? Track it!", "Your money moves fast. Keep your records one step ahead."),
        Nudge("A little expense, a big picture", "Every rupee counts. Log your latest purchase in seconds."),
        Nudge("Your memory isn't your ledger 😉", "Record that expense now. Your future self will thank you."),
    )

    /** Motivation; fine to send whether or not the user has logged anything today. */
    val quotes = listOf(
        Nudge("Update your expenses today", "“Control your money before it controls your month.”"),
        Nudge("Keep your tracking streak alive", "“Small habits today. Financial freedom tomorrow.”"),
        Nudge("Take 30 seconds to update your spending", "“What gets tracked gets managed.”"),
        Nudge("Check your spending summary", "“Don't guess where your salary went. Know where every rupee went.”"),
        Nudge("Log it before you forget", "“Your future budget starts with today's expense.”"),
    )

    /**
     * Picks the message for a given send. [sequence] counts sends (not days), so every message
     * is used once before any repeats, whatever interval the user chose.
     */
    fun pick(sequence: Long, loggedToday: Boolean): Nudge {
        val pool = if (loggedToday) quotes else interleaved
        return pool[(sequence % pool.size).toInt()]
    }

    /** Reminder, quote, reminder, quote... so the tone varies from one nudge to the next. */
    private val interleaved: List<Nudge> = buildList {
        for (i in 0 until maxOf(reminders.size, quotes.size)) {
            reminders.getOrNull(i)?.let(::add)
            quotes.getOrNull(i)?.let(::add)
        }
    }
}
