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
import app.paisapilot.core.designsystem.DayOfMonthField
import app.paisapilot.core.designsystem.FormScaffold
import app.paisapilot.core.designsystem.TextInput

@Composable
fun CardEditScreen(onDone: () -> Unit, viewModel: CardEditViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    FormScaffold(
        title = if (form.isEdit) "Edit credit card" else "Add credit card",
        onBack = onDone,
        onSave = { viewModel.save(onDone) },
        onDelete = if (form.isEdit) ({ confirmDelete = true }) else null,
    ) {
        TextInput(form.name, { viewModel.update { copy(name = it) } }, "Card name", error = form.errors["name"], hint = "e.g. HDFC Millennia")
        TextInput(form.issuer, { viewModel.update { copy(issuer = it) } }, "Bank (optional)")
        TextInput(
            form.last4, { viewModel.update { copy(last4 = it.filter(Char::isDigit)) } }, "Last 4 digits (optional)",
            keyboardType = KeyboardType.Number, maxLength = 4,
            hint = "Never enter the full card number, expiry or CVV in any app.",
        )
        AmountField(form.limit, { viewModel.update { copy(limit = it) } }, "Total credit limit", error = form.errors["limit"])
        AmountField(
            form.outstanding, { viewModel.update { copy(outstanding = it) } }, "Amount used so far",
            hint = "Your current outstanding. Card spends you record will add to this.",
        )
        DayOfMonthField("Statement date", form.statementDay, { viewModel.update { copy(statementDay = it) } })
        DayOfMonthField("Payment due date", form.dueDay, { viewModel.update { copy(dueDay = it) } }, hint = "We will remind you before this day.")
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this card?",
            message = "Its limit and dues will no longer be tracked. Past activity is kept.",
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
