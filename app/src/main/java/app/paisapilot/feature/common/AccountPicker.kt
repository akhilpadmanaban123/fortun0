package app.paisapilot.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paisapilot.core.designsystem.DropdownField
import app.paisapilot.core.util.Money
import app.paisapilot.data.local.entity.Account

private data class AccountOption(val id: String?, val label: String)

/** Dropdown of the user's accounts. [noneLabel] adds a "no account" choice at the top. */
@Composable
fun AccountPicker(
    label: String,
    accounts: List<Account>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    noneLabel: String? = "Not linked",
    hint: String? = null,
) {
    val options = buildList {
        if (noneLabel != null) add(AccountOption(null, noneLabel))
        accounts.forEach { add(AccountOption(it.id, "${it.name} · ${Money.format(it.balanceMinor)}")) }
    }
    DropdownField(
        label = label,
        selected = options.firstOrNull { it.id == selectedId },
        options = options,
        optionLabel = { it.label },
        onSelected = { onSelected(it.id) },
        modifier = modifier,
        hint = hint,
    )
}
