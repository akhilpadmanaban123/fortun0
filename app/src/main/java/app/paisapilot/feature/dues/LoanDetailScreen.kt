package app.paisapilot.feature.dues

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import app.paisapilot.core.designsystem.AmountField
import app.paisapilot.core.util.toLocalDate
import app.paisapilot.data.local.entity.LoanPrepayment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import app.paisapilot.core.designsystem.LocalMoneyColors
import app.paisapilot.core.designsystem.ProgressLine
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.designsystem.Stat
import app.paisapilot.core.finance.Installment
import app.paisapilot.core.finance.LoanStatus
import app.paisapilot.core.model.PrepayPolicy
import app.paisapilot.data.local.entity.Loan
import app.paisapilot.core.finance.PayoffIdea
import app.paisapilot.core.finance.PayoffIdeas
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.dueLabel
import app.paisapilot.core.util.pretty
import java.time.LocalDate

@Composable
fun LoanDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: LoanDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status = state.status
    var confirmPay by remember { mutableStateOf(false) }
    var showPrepay by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf<LoanPrepayment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(status?.loan?.name.orEmpty(), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (status != null) {
                        IconButton(onClick = { onEdit(status.loan.id) }) { Icon(Icons.Rounded.Edit, contentDescription = "Edit loan") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (status == null) return@Scaffold
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SummaryCard(status, state.accountName) }
            status.next?.let { next ->
                item { NextEmiCard(next, status, onPay = { confirmPay = true }, onPayExtra = { showPrepay = true }) }
            }
            if (status.isActive && status.loan.prepayPolicy == PrepayPolicy.NOT_ALLOWED) {
                item {
                    SectionCard {
                        Text("Extra payments are not allowed on this loan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "You told us your lender does not accept part-payments, so the EMIs are the only way to pay it down. If that changes, update it with the pencil above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (state.ideas.isNotEmpty()) {
                item { FinishSoonerCard(state.ideas, status.loan, onPayExtra = { showPrepay = true }) }
            }
            if (status.prepayments.isNotEmpty()) {
                item { ExtraPaymentsCard(state, onRemove = { confirmRemove = it }) }
            }
            item {
                Text("Repayment schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            }
            item {
                SectionCard {
                    status.schedule.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        ScheduleRow(row, paid = index < status.paid)
                    }
                }
            }
        }
    }

    val next = status?.next
    if (confirmPay && next != null) {
        ConfirmDialog(
            title = "Mark EMI ${next.number} as paid?",
            message = "${Money.format(next.amountMinor)} will be recorded as an expense" +
                (state.accountName?.let { " and deducted from $it" } ?: "") + ".",
            confirmLabel = "Mark paid",
            onConfirm = {
                viewModel.payNext()
                confirmPay = false
            },
            onDismiss = { confirmPay = false },
        )
    }

    if (showPrepay && status != null) {
        PrepayDialog(
            status = status,
            accountName = state.accountName,
            onConfirm = {
                viewModel.prepay(it)
                showPrepay = false
            },
            onDismiss = { showPrepay = false },
        )
    }

    confirmRemove?.let { prepayment ->
        ConfirmDialog(
            title = "Remove this extra payment?",
            message = "${Money.format(prepayment.amountMinor)} will be added back to what you owe, and the expense recorded for it will be deleted.",
            confirmLabel = "Remove",
            destructive = true,
            onConfirm = {
                viewModel.removePrepayment(prepayment.id)
                confirmRemove = null
            },
            onDismiss = { confirmRemove = null },
        )
    }
}

/** "Saves ₹600 in interest and 1 EMI.", or the same after the lender's charge when there is one. */
private fun PayoffIdea.savingText(): String {
    val emis = if (emisSaved > 0) " and $emisSaved EMI${if (emisSaved == 1) "" else "s"}" else ""
    return when {
        chargeMinor <= 0 -> "Saves ${Money.format(interestSavedMinor)} in interest$emis."
        netSavedMinor > 0 -> "Saves ${Money.format(netSavedMinor)}$emis, after a ${Money.format(chargeMinor)} charge from the lender."
        else -> "Not worth it: the lender's ${Money.format(chargeMinor)} charge is more than the ${Money.format(interestSavedMinor)} interest saved."
    }
}

/** Worked examples of paying extra, so the saving is concrete before the user decides. */
@Composable
private fun FinishSoonerCard(ideas: List<PayoffIdea>, loan: Loan, onPayExtra: () -> Unit) {
    SectionCard {
        Text("Finish this loan sooner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(
            "Anything you pay above the EMI goes straight to the loan amount, so less interest is charged every month after.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ideas.forEach { idea ->
            Spacer(Modifier.height(12.dp))
            Text(
                if (idea.everyMonth) "Add ${Money.format(idea.extraMinor)} to every EMI"
                else "Pay one extra EMI (${Money.format(idea.extraMinor)}) now",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                idea.savingText() + (idea.newEndDate?.let { " Loan ends ${it.pretty()}." } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalMoneyColors.current.positive,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                loan.prepayChargePct > 0 -> "These figures are after your lender's ${loan.prepayChargePct}% part-payment charge."
                loan.prepayPolicy == PrepayPolicy.ALLOWED -> "You told us your lender allows part-payments with no charge."
                else -> "Check that your lender allows part-payments and what it charges, then add it with the pencil above for exact figures."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onPayExtra) { Text("Record an extra payment") }
    }
}

/** Asks how much extra is being paid towards the principal. */
@Composable
private fun PrepayDialog(
    status: LoanStatus,
    accountName: String?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val amount = Money.parse(input) ?: 0
    val outstandingMinor = status.outstandingMinor
    // What this exact amount would save, worked out before the user commits to it.
    val preview = PayoffIdeas.simulate(status, amount.coerceAtMost(outstandingMinor), everyMonth = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay extra towards principal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "This goes straight to the loan amount, on top of your EMIs. Your EMI stays the same and the loan finishes sooner.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                AmountField(
                    value = input,
                    onValueChange = { input = it },
                    label = "Extra amount",
                    hint = if (amount > outstandingMinor) "More than you owe. Only ${Money.format(outstandingMinor)} will be applied."
                    else "You still owe ${Money.format(outstandingMinor)}.",
                )
                if (preview != null && preview.interestSavedMinor > 0) {
                    Text(
                        preview.savingText(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalMoneyColors.current.positive,
                    )
                }
                if (accountName != null) {
                    Text("It will be deducted from $accountName.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(amount) }, enabled = amount > 0) { Text("Record payment") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ExtraPaymentsCard(state: LoanDetailUiState, onRemove: (LoanPrepayment) -> Unit) {
    val status = state.status ?: return
    SectionCard {
        Text("Extra payments to principal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            buildString {
                append("${Money.format(status.prepaidMinor)} paid extra")
                if (state.emisSaved > 0) append(" · ${state.emisSaved} fewer EMI${if (state.emisSaved == 1) "" else "s"}")
                if (state.interestSavedMinor > 0) append(" · ${Money.format(state.interestSavedMinor)} interest saved")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = LocalMoneyColors.current.positive,
        )
        status.prepayments.forEach { prepayment ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(prepayment.dateEpochDay.toLocalDate().pretty(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(Money.format(prepayment.amountMinor), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { onRemove(prepayment) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove this extra payment", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(status: LoanStatus, accountName: String?) {
    val loan = status.loan
    SectionCard {
        Text(
            listOf(loan.type.label, loan.lender).filter { it.isNotBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(Money.format(status.outstandingMinor), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("principal still to repay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        ProgressLine(status.progress)
        Spacer(Modifier.height(6.dp))
        Text(
            if (status.isActive) "${status.paid} of ${status.totalInstallments} EMIs paid · ${status.remaining} to go"
            else "Loan fully repaid.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Loan amount", Money.format(loan.principalMinor), Modifier.weight(1f))
            Stat("Interest rate", "${loan.annualRatePct}% a year", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Monthly EMI", Money.format(loan.emiMinor), Modifier.weight(1f))
            Stat("Ends", status.endDate?.pretty().orEmpty(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Total interest", Money.format(status.totalInterestMinor), Modifier.weight(1f))
            Stat("Interest still to pay", Money.format(status.interestRemainingMinor), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Stat(
            "EMI is deducted from",
            if (accountName != null) accountName + (if (loan.autopay) " (auto-debit)" else "") else "Not chosen yet. Tap the pencil to choose.",
            valueColor = if (accountName != null) MaterialTheme.colorScheme.onSurface else LocalMoneyColors.current.warning,
        )
    }
}

@Composable
private fun NextEmiCard(next: Installment, status: LoanStatus, onPay: () -> Unit, onPayExtra: () -> Unit) {
    val overdue = next.dueDate.isBefore(LocalDate.now())
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("EMI ${next.number} of ${status.totalInstallments}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Money.format(next.amountMinor), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${dueLabel(next.dueDate)} · ${next.dueDate.pretty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) LocalMoneyColors.current.negative else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onPay) { Text("Mark paid") }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onPayExtra, modifier = Modifier.fillMaxWidth()) {
            Text("Pay extra towards principal")
        }
    }
}

@Composable
private fun ScheduleRow(row: Installment, paid: Boolean) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (paid) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = if (paid) "Paid" else "Not paid yet",
            tint = if (paid) LocalMoneyColors.current.positive else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${row.number}. ${row.dueDate.pretty()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "Principal ${Money.format(row.principalMinor)} · Interest ${Money.format(row.interestMinor)}",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
            if (row.prepaidMinor > 0) {
                Text(
                    "+ ${Money.format(row.prepaidMinor)} extra to principal",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMoneyColors.current.positive,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Money.format(row.amountMinor), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Bal ${Money.compact(row.balanceMinor)}", style = MaterialTheme.typography.labelSmall, color = muted)
        }
    }
}
