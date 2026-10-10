package app.paisapilot.feature.dues

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.core.designsystem.AmountField
import app.paisapilot.core.designsystem.ConfirmDialog
import app.paisapilot.core.designsystem.DateField
import app.paisapilot.core.designsystem.DropdownField
import app.paisapilot.core.designsystem.FormScaffold
import app.paisapilot.core.designsystem.SwitchRow
import app.paisapilot.core.designsystem.TextInput
import app.paisapilot.core.model.BillCategory
import app.paisapilot.core.model.BillFrequency
import app.paisapilot.feature.common.AccountPicker

@Composable
fun BillEditScreen(onDone: () -> Unit, viewModel: BillEditViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    FormScaffold(
        title = if (form.isEdit) "Edit bill" else "Add bill",
        onBack = onDone,
        onSave = { viewModel.save(onDone) },
        onDelete = if (form.isEdit) ({ confirmDelete = true }) else null,
    ) {
        DropdownField(
            label = "Category",
            selected = form.category,
            options = BillCategory.entries,
            optionLabel = { it.label },
            onSelected = viewModel::onCategory,
            hint = if (form.category.isInsurance) "Tracking insurance also improves your financial health score." else null,
        )
        TextInput(form.name, { viewModel.update { copy(name = it) } }, "Name", error = form.errors["name"])
        AmountField(form.amount, { viewModel.update { copy(amount = it) } }, "Amount", error = form.errors["amount"])
        DropdownField(
            label = "Repeats",
            selected = form.frequency,
            options = BillFrequency.entries,
            optionLabel = { it.label },
            onSelected = { viewModel.update { copy(frequency = it) } },
        )
        DateField("Next due date", form.nextDue, { viewModel.update { copy(nextDue = it) } })
        AccountPicker(
            label = "Paid from",
            accounts = accounts,
            selectedId = form.accountId,
            onSelected = { viewModel.update { copy(accountId = it) } },
            hint = "The balance is reduced when you mark this bill as paid.",
        )
        SwitchRow(
            title = "Auto-pay is set up",
            subtitle = "We will remind you to keep the balance ready.",
            checked = form.autopay,
            onCheckedChange = { viewModel.update { copy(autopay = it) } },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this bill?",
            message = "You will stop getting reminders for it. Payments already recorded stay in your activity.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onDone)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
