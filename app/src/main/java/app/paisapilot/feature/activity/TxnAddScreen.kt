package app.paisapilot.feature.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.core.designsystem.AmountField
import app.paisapilot.core.designsystem.DateField
import app.paisapilot.core.designsystem.DropdownField
import app.paisapilot.core.designsystem.FormScaffold
import app.paisapilot.core.designsystem.TextInput
import app.paisapilot.core.model.TxnCategory
import app.paisapilot.core.model.TxnType

@Composable
fun TxnAddScreen(onDone: () -> Unit, viewModel: TxnAddViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val types = TxnType.entries

    FormScaffold(
        title = when (form.type) {
            TxnType.EXPENSE -> "Add expense"
            TxnType.INCOME -> "Add income"
            TxnType.TRANSFER -> "Move money"
        },
        onBack = onDone,
        onSave = { viewModel.save(onDone) },
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            types.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = form.type == type,
                    onClick = { viewModel.onType(type) },
                    shape = SegmentedButtonDefaults.itemShape(index, types.size),
                ) { Text(type.label) }
            }
        }
        AmountField(form.amount, { viewModel.update { copy(amount = it) } }, "Amount", error = form.errors["amount"])

        if (form.type == TxnType.TRANSFER) {
            MoneyPicker(
                title = "From",
                options = form.accounts,
                selected = form.from,
                onSelected = { viewModel.update { copy(from = it, to = to?.takeUnless { t -> t == it }) } },
                error = form.errors["from"],
            )
            DropdownField(
                label = "To",
                selected = form.to,
                options = form.destinations,
                optionLabel = { it.label },
                onSelected = { viewModel.update { copy(to = it) } },
                hint = form.errors["to"] ?: "Another account, or a credit card to pay its bill.",
            )
        } else {
            MoneyPicker(
                title = if (form.type == TxnType.EXPENSE) "Paid from" else "Received in",
                options = form.sources,
                selected = form.from,
                onSelected = { viewModel.update { copy(from = it) } },
                error = form.errors["from"],
                hint = if (form.accounts.none { it.kind == MoneyKind.BANK }) "Add your bank accounts in Wallet to choose them here." else null,
            )
            DropdownField(
                label = "Category",
                selected = form.category,
                options = TxnCategory.of(form.type),
                optionLabel = { it.label },
                onSelected = { viewModel.update { copy(category = it) } },
            )
        }
        DateField("Date", form.date, { viewModel.update { copy(date = it) } })
        TextInput(form.note, { viewModel.update { copy(note = it) } }, "Note (optional)", maxLength = 80)
    }
}

/** One-tap choice between the user's bank accounts, cash and cards. */
@Composable
private fun MoneyPicker(
    title: String,
    options: List<MoneyOption>,
    selected: MoneyOption?,
    onSelected: (MoneyOption) -> Unit,
    error: String?,
    hint: String? = null,
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    leadingIcon = {
                        Icon(
                            when (option.kind) {
                                MoneyKind.BANK -> Icons.Rounded.AccountBalance
                                MoneyKind.CASH -> Icons.Rounded.Payments
                                MoneyKind.CARD -> Icons.Rounded.CreditCard
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    label = {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(option.name, style = MaterialTheme.typography.labelLarge)
                            if (option.detail.isNotEmpty()) {
                                Text(option.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                )
            }
        }
        (error ?: hint)?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
