package app.paisapilot.feature.dues

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.core.designsystem.AmountField
import app.paisapilot.core.designsystem.ConfirmDialog
import app.paisapilot.core.designsystem.DateField
import app.paisapilot.core.designsystem.DropdownField
import app.paisapilot.core.designsystem.FormScaffold
import app.paisapilot.core.designsystem.SwitchRow
import app.paisapilot.core.designsystem.TextInput
import app.paisapilot.core.model.LoanType
import app.paisapilot.core.util.Money
import app.paisapilot.feature.common.AccountPicker

@Composable
fun LoanEditScreen(
    onDone: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: LoanEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    FormScaffold(
        title = if (form.isEdit) "Edit loan" else "Add loan / EMI",
        onBack = onDone,
        onSave = { viewModel.save(onDone) },
        onDelete = if (form.isEdit) ({ confirmDelete = true }) else null,
    ) {
        DropdownField(
            label = "Type",
            selected = form.type,
            options = LoanType.entries,
            optionLabel = { it.label },
            onSelected = { viewModel.update { copy(type = it) } },
        )
        TextInput(form.name, { viewModel.update { copy(name = it) } }, "Name", error = form.errors["name"], hint = "e.g. Activa loan, iPhone EMI")
        TextInput(form.lender, { viewModel.update { copy(lender = it) } }, "Bank / lender (optional)")
        AmountField(form.principal, { viewModel.update { copy(principal = it) } }, "Loan amount", error = form.errors["principal"])
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextInput(
                form.rate, { viewModel.update { copy(rate = it) } }, "Interest % a year",
                modifier = Modifier.weight(1f), error = form.errors["rate"], keyboardType = KeyboardType.Decimal, maxLength = 5,
            )
            TextInput(
                form.tenure, { viewModel.update { copy(tenure = it.filter(Char::isDigit)) } }, "Tenure (months)",
                modifier = Modifier.weight(1f), error = form.errors["tenure"], keyboardType = KeyboardType.Number, maxLength = 3,
            )
        }
        AmountField(
            form.emi, { viewModel.update { copy(emi = it, emiEdited = true) } }, "Monthly EMI",
            error = form.errors["emi"],
            hint = if (form.calculatedEmiMinor > 0) "Calculated: ${Money.format(form.calculatedEmiMinor)}. Change it if your lender's figure differs." else null,
        )
        DateField("First EMI date", form.firstEmi, { viewModel.update { copy(firstEmi = it) } }, hint = "Later EMIs fall on the same day every month.")
        TextInput(
            form.paid, { viewModel.update { copy(paid = it.filter(Char::isDigit), paidEdited = true) } }, "EMIs already paid",
            error = form.errors["paid"], keyboardType = KeyboardType.Number, maxLength = 3,
        )
        AccountPicker(
            label = "EMI is paid from",
            accounts = accounts,
            selectedId = form.accountId,
            onSelected = { viewModel.update { copy(accountId = it) } },
            hint = "The balance is reduced when you mark an EMI as paid.",
        )
        SwitchRow(
            title = "Auto-debit is set up",
            subtitle = "We will remind you to keep the balance ready.",
            checked = form.autopay,
            onCheckedChange = { viewModel.update { copy(autopay = it) } },
        )
        if (form.errors.isNotEmpty()) {
            Text("Please fix the highlighted fields.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }

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
