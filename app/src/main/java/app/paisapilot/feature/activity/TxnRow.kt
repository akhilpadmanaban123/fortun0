package app.paisapilot.feature.activity

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paisapilot.core.designsystem.ListRow
import app.paisapilot.core.designsystem.LocalMoneyColors
import app.paisapilot.core.designsystem.icon
import app.paisapilot.core.model.CategoryInfo
import app.paisapilot.core.model.TxnType
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.short
import app.paisapilot.core.util.toLocalDate
import app.paisapilot.data.local.entity.Txn

@Composable
fun TxnRow(txn: Txn, modifier: Modifier = Modifier, showDate: Boolean = true) {
    val money = LocalMoneyColors.current
    val category = CategoryInfo.of(txn.category)
    val (sign, color) = when (txn.type) {
        TxnType.INCOME -> "+" to money.positive
        TxnType.EXPENSE -> "-" to MaterialTheme.colorScheme.onSurface
        TxnType.TRANSFER -> "" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    ListRow(
        icon = category.icon,
        title = txn.note.ifBlank { category.label },
        subtitle = listOfNotNull(
            category.label.takeIf { txn.note.isNotBlank() },
            txn.dateEpochDay.toLocalDate().short().takeIf { showDate },
        ).joinToString(" · "),
        modifier = modifier,
        tint = when (txn.type) {
            TxnType.INCOME -> money.positive
            TxnType.EXPENSE -> MaterialTheme.colorScheme.secondary
            TxnType.TRANSFER -> MaterialTheme.colorScheme.tertiary
        },
        value = sign + Money.format(txn.amountMinor),
        valueColor = color,
    )
}
