package app.paisapilot.feature.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.core.designsystem.AmountField
import app.paisapilot.core.designsystem.ConfirmDialog
import app.paisapilot.core.designsystem.DropdownField
import app.paisapilot.core.designsystem.FormScaffold
import app.paisapilot.core.designsystem.TextInput
import app.paisapilot.core.model.AccountType

@Composable
fun AccountEditScreen(onDone: () -> Unit, viewModel: AccountEditViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val isCash = form.type == AccountType.CASH

    FormScaffold(
        title = if (form.isEdit) "Edit account" else "Add account",
        onBack = onDone,
        onSave = { viewModel.save(onDone) },
        onDelete = if (form.isEdit) ({ confirmDelete = true }) else null,
    ) {
        DropdownField(
            label = "Type",
            selected = form.type,
            options = AccountType.entries,
            optionLabel = { it.label },
            onSelected = viewModel::onType,
            hint = if (!form.type.liquid) "Kept out of your safe-to-spend amount, but counted in net worth." else null,
        )
        TextInput(form.name, { viewModel.update { copy(name = it) } }, "Name", error = form.nameError, hint = if (isCash) null else "e.g. HDFC Salary")
        if (!isCash) {
            TextInput(form.institution, { viewModel.update { copy(institution = it) } }, "Bank / provider (optional)")
            TextInput(
                form.last4, { viewModel.update { copy(last4 = it.filter(Char::isDigit)) } }, "Last 4 digits (optional)",
                keyboardType = KeyboardType.Number, maxLength = 4,
                hint = "Only to tell accounts apart. Never enter the full number.",
            )
        }
        AmountField(form.balance, { viewModel.update { copy(balance = it) } }, "Current balance")
        TextInput(
            form.purpose, { viewModel.update { copy(purpose = it) } }, "What do you use it for? (optional)",
            hint = "e.g. Salary and EMIs, Daily spends, Emergency fund", maxLength = 80,
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this account?",
            message = "Its balance will no longer count towards your totals. Past activity is kept.",
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
