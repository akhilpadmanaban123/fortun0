package app.paisapilot.feature.dues

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.core.designsystem.AmountField
import app.paisapilot.core.designsystem.ConfirmDialog
import app.paisapilot.core.designsystem.DateField
import app.paisapilot.core.designsystem.DropdownField
import app.paisapilot.core.designsystem.FormScaffold
import app.paisapilot.core.designsystem.LocalMoneyColors
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.designsystem.SwitchRow
import app.paisapilot.core.designsystem.TextInput
import app.paisapilot.core.model.LoanType
import app.paisapilot.core.model.PrepayPolicy
import app.paisapilot.core.model.RateType
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.pretty
import app.paisapilot.feature.common.AccountPicker

/**
 * Add or edit a loan. The questions are grouped in the order someone reads their loan
 * papers: what it is, its terms, where it stands today, and how the EMI is paid.
 */
@Composable
fun LoanEditScreen(
    onDone: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: LoanEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    val draft = form.draft

    FormScaffold(
        title = if (form.isEdit) "Edit loan" else "Add loan / EMI",
        onBack = onDone,
        onSave = { viewModel.save(onDone) },
        onDelete = if (form.isEdit) ({ confirmDelete = true }) else null,
    ) {
        SectionCard(onClick = { showHelp = true }) {
            Text("Not sure of some of these details?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Get them from your loan statement in about 5 minutes. Tap to see how.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Heading("About the loan")
        DropdownField(
            label = "Type",
            selected = form.type,
            options = LoanType.entries,
            optionLabel = { it.label },
            onSelected = { viewModel.update { copy(type = it) } },
        )
        TextInput(form.name, { viewModel.update { copy(name = it) } }, "Name", error = form.errors["name"], hint = "e.g. Activa loan, iPhone EMI")
        TextInput(form.lender, { viewModel.update { copy(lender = it) } }, "Bank / lender (optional)")

        Heading("Loan terms")
        AmountField(
            form.principal, { viewModel.update { copy(principal = it) } }, "Loan amount",
            error = form.errors["principal"], hint = "The amount you borrowed at the start.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextInput(
                form.rate, { viewModel.update { copy(rate = it) } }, "Interest % a year",
                modifier = Modifier.weight(1f), error = form.errors["rate"], keyboardType = KeyboardType.Decimal, maxLength = 5,
            )
            TextInput(
                form.tenure, { viewModel.update { copy(tenure = it.filter(Char::isDigit)) } }, "Total months",
                modifier = Modifier.weight(1f), error = form.errors["tenure"], keyboardType = KeyboardType.Number, maxLength = 3,
            )
        }
        AmountField(
            form.emi, { viewModel.update { copy(emi = it, emiEdited = true) } }, "Monthly EMI",
            error = form.errors["emi"],
            hint = if (form.calculatedEmiMinor > 0) "Calculated: ${Money.format(form.calculatedEmiMinor)}. Change it if your lender's figure differs." else null,
        )

        Heading("Where it stands today")
        DateField("Date of the first EMI", form.firstEmi, { viewModel.update { copy(firstEmi = it) } }, hint = "Later EMIs fall on the same day every month.")
        TextInput(
            form.paid, { viewModel.update { copy(paid = it.filter(Char::isDigit), paidEdited = true) } }, "How many EMIs have you paid so far?",
            error = form.errors["paid"], keyboardType = KeyboardType.Number, maxLength = 3,
            hint = "Counted from the first EMI date up to today. Change it if you have paid more or fewer.",
        )
        AmountField(
            form.outstanding, { viewModel.update { copy(outstanding = it) } }, "Amount still owed today (optional)",
            error = form.errors["outstanding"],
            hint = "From your lender's app or latest statement. If you paid extra in earlier months, it will be lower than our figure and we " +
                "will record the difference as earlier extra payments. Nothing is deducted from your accounts.",
        )
        if (draft != null) {
            SectionCard {
                Text(
                    if (draft.isActive) "You have paid ${draft.paid} of ${draft.totalInstallments} EMIs, with ${draft.remaining} to go."
                    else "All ${draft.totalInstallments} EMIs are paid. This loan will be saved as closed.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (draft.isActive) {
                    Text(
                        "By our calculation about ${Money.format(draft.outstandingMinor)} is still owed. " +
                            "Next EMI on ${draft.nextDueDate?.pretty()}, last one on ${draft.endDate?.pretty()}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (form.startDateLooksWrong) {
                        Text(
                            "That next EMI is a long way off. With ${draft.paid} EMIs already paid, the first one was probably " +
                                "${draft.paid} months ago, not on ${form.firstEmi.pretty()}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalMoneyColors.current.warning,
                        )
                        TextButton(onClick = viewModel::fixStartDate) { Text("Fix the first EMI date for me") }
                    }
                    val entered = Money.parse(form.outstanding)?.takeIf { form.outstanding.isNotBlank() }
                    if (entered != null && entered < draft.outstandingMinor) {
                        Text(
                            "Your lender says ${Money.format(entered)}. The ${Money.format(draft.outstandingMinor - entered)} difference " +
                                "will be saved as extra you have already paid, so the schedule matches.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalMoneyColors.current.positive,
                        )
                    }
                }
            }
        }

        Heading("Paying extra to finish sooner")
        DropdownField(
            label = "Is the interest rate fixed or floating?",
            selected = form.rateType,
            options = RateType.entries,
            optionLabel = { it.label },
            onSelected = { viewModel.update { copy(rateType = it) } },
            hint = if (form.rateType == RateType.FLOATING) "Lenders generally cannot charge individuals for prepaying a floating-rate loan. Confirm with yours." else null,
        )
        DropdownField(
            label = "Does your lender allow paying extra (part-payment)?",
            selected = form.prepayPolicy,
            options = PrepayPolicy.entries,
            optionLabel = { it.label },
            onSelected = { viewModel.update { copy(prepayPolicy = it) } },
            hint = "It is in your loan agreement, or ask the lender. Extra payments cut the loan amount, so less interest is charged from the next month on.",
        )
        if (form.prepayPolicy != PrepayPolicy.NOT_ALLOWED) {
            TextInput(
                form.prepayCharge, { viewModel.update { copy(prepayCharge = it) } }, "Part-payment / pre-closure charge % (optional)",
                error = form.errors["prepayCharge"], keyboardType = KeyboardType.Decimal, maxLength = 5,
                hint = "What the lender takes on any extra amount you pay, e.g. 2. Leave blank if there is none or you are not sure.",
            )
        }

        Heading("How the EMI is paid")
        AccountPicker(
            label = "Which account is the EMI deducted from?",
            accounts = accounts,
            selectedId = form.accountId,
            onSelected = { viewModel.update { copy(accountId = it) } },
            noneLabel = "Not chosen yet",
            hint = if (accounts.isEmpty()) "Add your bank account in Wallet first, then choose it here." else null,
        )
        if (form.accountId == null && accounts.isNotEmpty()) {
            Text(
                "Choose the account so Fortun0 can warn you when its balance will not cover the EMI, and deduct it when you mark an EMI as paid.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalMoneyColors.current.warning,
            )
        }
        SwitchRow(
            title = "Auto-debit is set up",
            subtitle = "The bank takes the EMI by itself. We will remind you to keep the balance ready.",
            checked = form.autopay,
            onCheckedChange = { viewModel.update { copy(autopay = it) } },
        )
        if (form.errors.isNotEmpty()) {
            Text("Please fix the highlighted fields.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (showHelp) LoanDetailsHelpDialog(onDismiss = { showHelp = false })

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this loan?",
            message = "Its schedule and reminders will be removed. Payments already recorded stay in your activity.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onDeleted)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
}
