package app.paisapilot.feature.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.core.designsystem.EmptyState
import app.paisapilot.core.designsystem.ListRow
import app.paisapilot.core.designsystem.LocalMoneyColors
import app.paisapilot.core.designsystem.ProgressLine
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.designsystem.SectionHeader
import app.paisapilot.core.designsystem.Stat
import app.paisapilot.core.designsystem.TabScaffold
import app.paisapilot.core.designsystem.icon
import app.paisapilot.core.designsystem.tabListPadding
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.ordinal
import app.paisapilot.data.local.entity.Account
import app.paisapilot.data.local.entity.CreditCard

@Composable
fun WalletScreen(
    onEditAccount: (String?) -> Unit,
    onEditCard: (String?) -> Unit,
    onPayCard: (String) -> Unit,
    viewModel: WalletViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TabScaffold(title = "Wallet") { inner ->
        LazyColumn(
            contentPadding = tabListPadding(inner),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Row(Modifier.fillMaxWidth()) {
                        Stat("Cash", Money.format(state.cashMinor), Modifier.weight(1f))
                        Stat("In accounts", Money.format(state.bankMinor), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Stat("Deposits & investments", Money.format(state.lockedMinor), Modifier.weight(1f))
                        Stat("Card dues", Money.format(state.cardOutstandingMinor), Modifier.weight(1f))
                    }
                }
            }

            item { SectionHeader("Cash & accounts", action = "Add", onAction = { onEditAccount(null) }) }
            if (state.accounts.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        Icons.Rounded.AccountBalanceWallet, "No accounts yet",
                        "Add your cash in hand and each bank account, with what you use it for.",
                        actionLabel = "Add account", onAction = { onEditAccount(null) },
                    )
                }
            }
            items(state.accounts, key = { it.id }) { AccountCard(it, onClick = { onEditAccount(it.id) }) }

            item { SectionHeader("Credit cards", action = "Add", onAction = { onEditCard(null) }) }
            if (state.cards.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        Icons.Rounded.CreditCard, "No credit cards yet",
                        "Add a card to track its limit, what you have used and when the bill is due.",
                        actionLabel = "Add card", onAction = { onEditCard(null) },
                    )
                }
            }
            items(state.cards, key = { it.id }) { card ->
                CardCard(card, onClick = { onEditCard(card.id) }, onPay = { onPayCard(card.id) })
            }
        }
    }
}

@Composable
private fun AccountCard(account: Account, onClick: () -> Unit) {
    SectionCard(onClick = onClick) {
        ListRow(
            icon = account.type.icon,
            title = account.name,
            subtitle = listOf(
                account.type.label,
                account.last4.takeIf { it.isNotBlank() }?.let { "··$it" }.orEmpty(),
            ).filter { it.isNotBlank() }.joinToString(" "),
            value = Money.format(account.balanceMinor),
            valueColor = if (account.balanceMinor < 0) LocalMoneyColors.current.negative else MaterialTheme.colorScheme.onSurface,
        )
        if (account.purpose.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text("Used for: ${account.purpose}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CardCard(card: CreditCard, onClick: () -> Unit, onPay: () -> Unit) {
    val money = LocalMoneyColors.current
    SectionCard(onClick = onClick) {
        ListRow(
            icon = Icons.Rounded.CreditCard,
            title = card.name,
            subtitle = listOf(card.issuer, card.last4.takeIf { it.isNotBlank() }?.let { "··$it" }.orEmpty())
                .filter { it.isNotBlank() }.joinToString(" "),
            tint = MaterialTheme.colorScheme.tertiary,
            value = Money.format(card.outstandingMinor),
            valueNote = "used",
        )
        Spacer(Modifier.height(14.dp))
        ProgressLine(
            progress = card.utilisation,
            color = when {
                card.utilisation > 0.75f -> money.negative
                card.utilisation > 0.3f -> money.warning
                else -> MaterialTheme.colorScheme.primary
            },
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Available", Money.format(card.availableMinor), Modifier.weight(1f))
            Stat("Limit", Money.format(card.limitMinor), Modifier.weight(1f))
            Stat("Bill due", ordinal(card.dueDay), Modifier.weight(1f))
        }
        if (card.outstandingMinor > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onPay) { Text("Pay card bill") }
            }
        }
    }
}
