package app.paisapilot.feature.dues

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.core.designsystem.ConfirmDialog
import app.paisapilot.core.designsystem.EmptyState
import app.paisapilot.core.designsystem.ListRow
import app.paisapilot.core.designsystem.LocalMoneyColors
import app.paisapilot.core.designsystem.ProgressLine
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.designsystem.Stat
import app.paisapilot.core.designsystem.TabScaffold
import app.paisapilot.core.designsystem.icon
import app.paisapilot.core.designsystem.tabListPadding
import app.paisapilot.core.finance.LoanStatus
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.daysBetween
import app.paisapilot.core.util.dueLabel
import app.paisapilot.core.util.short
import app.paisapilot.core.util.toLocalDate
import app.paisapilot.data.local.entity.Bill
import java.time.LocalDate

private val tabs = listOf("Loans & EMIs", "Bills")

@Composable
fun DuesScreen(
    onOpenLoan: (String) -> Unit,
    onAddLoan: () -> Unit,
    onEditBill: (String?) -> Unit,
    viewModel: DuesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var confirmPaid by remember { mutableStateOf<Bill?>(null) }
    val today = LocalDate.now()
    val showingLoans = tab == 0

    TabScaffold(
        title = "EMIs & Bills",
        fabLabel = if (showingLoans) "Add loan" else "Add bill",
        onFab = { if (showingLoans) onAddLoan() else onEditBill(null) },
    ) { inner ->
        LazyColumn(
            contentPadding = tabListPadding(inner, hasFab = true),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = tab == index,
                            onClick = { tab = index },
                            shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
                        ) { Text(label) }
                    }
                }
            }

            if (showingLoans) {
                if (state.loans.isNotEmpty()) {
                    item {
                        SectionCard {
                            Row(Modifier.fillMaxWidth()) {
                                Stat("EMIs every month", Money.format(state.monthlyEmiMinor), Modifier.weight(1f))
                                Stat("Total still to repay", Money.format(state.outstandingMinor), Modifier.weight(1f))
                            }
                        }
                    }
                }
                items(state.loans, key = { it.loan.id }) { LoanCard(it, today, onClick = { onOpenLoan(it.loan.id) }) }
                if (state.loans.isEmpty() && !state.loading) {
                    item {
                        EmptyState(
                            Icons.Rounded.AccountBalance, "No loans yet",
                            "Add a home, personal, bike or phone EMI to see its schedule, what is left and when it ends.",
                        )
                    }
                }
            } else {
                if (state.bills.isNotEmpty()) {
                    item {
                        SectionCard {
                            Stat("Bills in a typical month", Money.format(state.monthlyBillsMinor))
                        }
                    }
                }
                items(state.bills, key = { it.id }) { bill ->
                    BillCard(bill, today, onClick = { onEditBill(bill.id) }, onMarkPaid = { confirmPaid = bill })
                }
                if (state.bills.isEmpty() && !state.loading) {
                    item {
                        EmptyState(
                            Icons.Rounded.EventNote, "No bills yet",
                            "Add rent, recharges, insurance premiums and SIPs so nothing slips past its due date.",
                        )
                    }
                }
            }
        }
    }

    confirmPaid?.let { bill ->
        ConfirmDialog(
            title = "Mark ${bill.name} as paid?",
            message = "${Money.format(bill.amountMinor)} will be recorded as an expense" +
                (if (bill.accountId != null) " and deducted from the linked account" else "") + ".",
            confirmLabel = "Mark paid",
            onConfirm = {
                viewModel.markBillPaid(bill.id)
                confirmPaid = null
            },
            onDismiss = { confirmPaid = null },
        )
    }
}

@Composable
private fun LoanCard(status: LoanStatus, today: LocalDate, onClick: () -> Unit) {
    val loan = status.loan
    val money = LocalMoneyColors.current
    val due = status.nextDueDate
    SectionCard(onClick = onClick) {
        ListRow(
            icon = loan.type.icon,
            title = loan.name,
            subtitle = loan.lender.ifBlank { loan.type.label },
            value = Money.format(status.next?.amountMinor ?: loan.emiMinor),
            valueNote = if (due != null) dueLabel(due, today) else "Closed",
            valueNoteColor = when {
                due == null -> money.positive
                due.isBefore(today) -> money.negative
                daysBetween(today, due) <= 3 -> money.warning
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.height(14.dp))
        ProgressLine(status.progress)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${status.paid} of ${status.totalInstallments} EMIs paid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${Money.format(status.outstandingMinor)} left", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BillCard(bill: Bill, today: LocalDate, onClick: () -> Unit, onMarkPaid: () -> Unit) {
    val money = LocalMoneyColors.current
    val due = bill.nextDueEpochDay.toLocalDate()
    SectionCard(onClick = onClick) {
        ListRow(
            icon = bill.category.icon,
            title = bill.name,
            subtitle = "${bill.frequency.label} · ${due.short()}" + if (bill.autopay) " · Auto-pay" else "",
            tint = MaterialTheme.colorScheme.secondary,
            value = Money.format(bill.amountMinor),
            valueNote = if (bill.active) dueLabel(due, today) else "Paid",
            valueNoteColor = when {
                !bill.active -> money.positive
                due.isBefore(today) -> money.negative
                daysBetween(today, due) <= 3 -> money.warning
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (bill.active) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onMarkPaid) { Text("Mark as paid") }
            }
        }
    }
}
