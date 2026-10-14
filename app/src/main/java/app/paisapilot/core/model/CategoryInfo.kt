package app.paisapilot.core.model

/**
 * What a transaction's stored category means: one of the built-in [TxnCategory] names, or a
 * category the user typed themselves.
 *
 * A custom category lives inside the transaction as `custom:<iconKey>:<name>`, so it is backed
 * up with the transaction and needs no table of its own. An app version that does not know the
 * format reads it as "Other".
 */
data class CategoryInfo(
    /** The value saved in [app.paisapilot.data.local.entity.Txn.category]. */
    val stored: String,
    val label: String,
    /** Set for custom categories: a key from the icon picker. */
    val iconKey: String? = null,
    val builtIn: TxnCategory? = null,
) {
    val isCustom: Boolean get() = builtIn == null

    companion object {
        private const val PREFIX = "custom:"
        const val MAX_NAME_LENGTH = 24

        fun of(category: TxnCategory) = CategoryInfo(category.name, category.label, builtIn = category)

        fun of(stored: String): CategoryInfo {
            if (stored.startsWith(PREFIX)) {
                val parts = stored.removePrefix(PREFIX).split(':', limit = 2)
                val name = parts.getOrNull(1)?.trim().orEmpty()
                if (name.isNotEmpty()) return CategoryInfo(stored, name, iconKey = parts[0])
            }
            return of(TxnCategory.fromName(stored))
        }

        fun custom(name: String, iconKey: String): CategoryInfo {
            val clean = name.trim().take(MAX_NAME_LENGTH)
            return CategoryInfo("$PREFIX$iconKey:$clean", clean, iconKey = iconKey)
        }

        /** For queries that list the custom categories already in use. */
        const val STORED_PATTERN = "$PREFIX%"
    }
}
