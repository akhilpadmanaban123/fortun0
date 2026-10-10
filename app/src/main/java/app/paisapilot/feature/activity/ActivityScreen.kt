package app.paisapilot.feature.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.core.designsystem.ConfirmDialog
import app.paisapilot.core.designsystem.EmptyState
import app.paisapilot.core.designsystem.LocalMoneyColors
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.designsystem.Stat
import app.paisapilot.core.designsystem.TabScaffold
import app.paisapilot.core.designsystem.tabListPadding
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.pretty
import app.paisapilot.core.util.toLocalDate
import app.paisapilot.data.local.entity.Txn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayHeader = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH)

@Composable
fun ActivityScreen(onAdd: () -> Unit, viewModel: ActivityViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val money = LocalMoneyColors.current
    var confirmDelete by remember { mutableStateOf<Txn?>(null) }
    val today = LocalDate.now()

    TabScaffold(title = "Activity", fabLabel = "Add", onFab = onAdd) { inner ->
        LazyColumn(
            contentPadding = tabListPadding(inner, hasFab = true),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = viewModel::previousMonth) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month") }
                    Text(state.month.pretty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = viewModel::nextMonth, enabled = state.canGoForward) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
                    }
                }
            }
            item {
                SectionCard {
                    Row(Modifier.fillMaxWidth()) {
                        Stat("Income", Money.format(state.incomeMinor), Modifier.weight(1f), money.positive)
                        Stat("Spent", Money.format(state.spentMinor), Modifier.weight(1f))
                        val net = state.incomeMinor - state.spentMinor
                        Stat("Net", Money.format(net), Modifier.weight(1f), if (net < 0) money.negative else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (state.txns.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        Icons.Rounded.SwapVert, "Nothing recorded this month",
                        "Add what you spend and earn. EMIs and bills you mark as paid show up here automatically.",
                    )
                }
            }

            state.byDay.forEach { (epochDay, dayTxns) ->
                val date = epochDay.toLocalDate()
                item(key = epochDay) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            when (date) {
                                today -> "Today"
                                today.minusDays(1) -> "Yesterday"
                                else -> date.format(dayHeader)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        )
                        SectionCard {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                dayTxns.forEach { txn ->
                                    TxnRow(txn, Modifier.clickable { confirmDelete = txn }, showDate = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { txn ->
        ConfirmDialog(
            title = "Delete this entry?",
            message = "${Money.format(txn.amountMinor)} will be removed and the account balance put back as it was.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.delete(txn.id)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null },
        )
    }
}
