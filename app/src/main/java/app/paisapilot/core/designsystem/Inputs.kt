package app.paisapilot.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paisapilot.core.util.ordinal
import app.paisapilot.core.util.pretty
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLength: Int = 60,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onValueChange(it) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = error != null,
        supportingText = (error ?: hint)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Words else KeyboardCapitalization.None,
        ),
    )
}

/** Rupee amount entry. Accepts digits and a single decimal point with up to two places. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val cleaned = input.filter { it.isDigit() || it == '.' }
            val valid = cleaned.count { it == '.' } <= 1 &&
                cleaned.substringAfter('.', "").length <= 2 &&
                cleaned.substringBefore('.').length <= 11
            if (valid) onValueChange(cleaned)
        },
        label = { Text(label) },
        prefix = { Text("₹ ") },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = error != null,
        supportingText = (error ?: hint)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

/** A read-only field that opens something (menu, dialog) when tapped. */
@Composable
private fun PickerField(
    text: String,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            singleLine = true,
            trailingIcon = { Icon(icon, contentDescription = null) },
            supportingText = hint?.let { { Text(it) } },
            // Screen readers get one clear control from the layer below instead of a dead text box.
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
        )
        // The text field would swallow taps; this transparent layer makes the whole field a button.
        Box(
            Modifier
                .matchParentSize()
                .clickable(onClickLabel = "change", role = Role.DropdownList, onClick = onClick)
                .semantics { contentDescription = "$label: ${text.ifEmpty { "not set" }}" },
        )
    }
}

@Composable
fun <T> DropdownField(
    label: String,
    selected: T?,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        PickerField(
            text = selected?.let(optionLabel).orEmpty(),
            label = label,
            icon = Icons.Rounded.ArrowDropDown,
            onClick = { expanded = true },
            hint = hint,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

@Composable
fun DayOfMonthField(
    label: String,
    day: Int,
    onDayChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    DropdownField(
        label = label,
        selected = day.takeIf { it in 1..31 },
        options = (1..31).toList(),
        optionLabel = { "${ordinal(it)} of the month" },
        onSelected = onDayChange,
        modifier = modifier,
        hint = hint,
    )
}

@Composable
fun DateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    PickerField(
        text = date.pretty(),
        label = label,
        icon = Icons.Rounded.CalendarMonth,
        onClick = { open = true },
        modifier = modifier,
        hint = hint,
    )
    if (open) {
        // The Material date picker works in UTC midnights.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(enabled = enabled) { onCheckedChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
