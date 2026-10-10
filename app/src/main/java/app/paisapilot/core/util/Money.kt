package app.paisapilot.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * All amounts are stored as whole paise (minor units) in a Long, so arithmetic is exact.
 * Formatting uses the Indian grouping system: 12,34,567.
 */
object Money {
    private const val SYMBOL = "₹"
    private val symbols = DecimalFormatSymbols(Locale.ENGLISH)

    fun format(minor: Long, showPaise: Boolean = false): String {
        val negative = minor < 0
        val absolute = abs(minor)
        val rupees = absolute / 100
        val paise = (absolute % 100).toInt()
        val body = buildString {
            append(groupIndian(rupees))
            if (showPaise || paise != 0) append('.').append(paise.toString().padStart(2, '0'))
        }
        return (if (negative) "-" else "") + SYMBOL + body
    }

    /** ₹1.2L / ₹3.4Cr style for tight spaces. */
    fun compact(minor: Long): String {
        val rupees = abs(minor) / 100.0
        val sign = if (minor < 0) "-" else ""
        val oneDecimal = DecimalFormat("0.#", symbols)
        return when {
            rupees >= 1_00_00_000 -> "$sign$SYMBOL${oneDecimal.format(rupees / 1_00_00_000)}Cr"
            rupees >= 1_00_000 -> "$sign$SYMBOL${oneDecimal.format(rupees / 1_00_000)}L"
            rupees >= 1_000 -> "$sign$SYMBOL${oneDecimal.format(rupees / 1_000)}K"
            else -> format(minor)
        }
    }

    /** Parses user input such as "12,500.50" into paise; null when it is not a valid amount. */
    fun parse(input: String): Long? {
        val cleaned = input.replace(",", "").replace(SYMBOL, "").trim()
        if (cleaned.isEmpty()) return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        if (value.signum() < 0) return null
        return value.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
    }

    /** Plain editable text for an amount, e.g. 1250050 -> "12500.5". */
    fun toInput(minor: Long): String {
        if (minor == 0L) return ""
        return BigDecimal(minor).movePointLeft(2).stripTrailingZeros().toPlainString()
    }

    private fun groupIndian(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val grouped = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$grouped,$lastThree"
    }
}
