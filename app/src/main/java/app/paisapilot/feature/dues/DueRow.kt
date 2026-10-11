package app.paisapilot.feature.dues

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paisapilot.core.designsystem.ListRow
import app.paisapilot.core.designsystem.LocalMoneyColors
import app.paisapilot.core.finance.Due
import app.paisapilot.core.finance.DueKind
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.daysBetween
import app.paisapilot.core.util.dueLabel
import app.paisapilot.core.util.short
import java.time.LocalDate

/** One upcoming EMI, bill or card payment, with urgency shown by colour. */
@Composable
fun DueRow(due: Due, today: LocalDate, modifier: Modifier = Modifier, accountName: String? = null) {
    val money = LocalMoneyColors.current
    val days = daysBetween(today, due.date)
    val urgency = when {
        days < 0 -> money.negative
        days <= 3 -> money.warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    ListRow(
        icon = when (due.kind) {
            DueKind.EMI -> Icons.Rounded.AccountBalance
            DueKind.BILL -> Icons.Rounded.Receipt
            DueKind.CARD -> Icons.Rounded.CreditCard
        },
        title = due.title,
        subtitle = "${due.subtitle} · ${due.date.short()}" +
            (if (accountName != null) " · from $accountName" else "") +
            if (due.autopay) " · Auto-pay" else "",
        modifier = modifier,
        tint = when (due.kind) {
            DueKind.EMI -> MaterialTheme.colorScheme.primary
            DueKind.BILL -> MaterialTheme.colorScheme.secondary
            DueKind.CARD -> MaterialTheme.colorScheme.tertiary
        },
        value = Money.format(due.amountMinor),
        valueNote = dueLabel(due.date, today),
        valueNoteColor = urgency,
    )
}
