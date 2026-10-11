package app.paisapilot.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paisapilot.core.designsystem.IconBadge
import app.paisapilot.core.designsystem.LocalMoneyColors
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.designsystem.icon
import app.paisapilot.core.finance.DueKind
import app.paisapilot.core.finance.MoneySnapshot
import app.paisapilot.core.util.Money
import app.paisapilot.data.local.entity.Account

/**
 * Every account with what it is for, what it holds, and how much in EMIs and bills will be
 * taken from it before payday, so a shortfall in the wrong account is visible in advance.
 */
@Composable
internal fun AccountsCard(
    accounts: List<Account>,
    s: MoneySnapshot,
    onOpenAccount: (String) -> Unit,
    onOpenDues: () -> Unit,
) {
    val money = LocalMoneyColors.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    // EMIs and bills only; a card bill has no fixed account until the user pays it.
    val scheduled = s.duesBeforePayday.filter { it.kind != DueKind.CARD }
    val unassigned = scheduled.count { it.accountId == null || accounts.none { a -> a.id == it.accountId } }

    SectionCard {
        Text("Your accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        // Spendable accounts first, then deposits and investments.
        accounts.sortedBy { !it.type.liquid }.forEachIndexed { index, account ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val dueFromHere = scheduled.filter { it.accountId == account.id }.sumOf { it.amountMinor }
            val shortBy = dueFromHere - account.balanceMinor
            Column(Modifier.fillMaxWidth().clickable { onOpenAccount(account.id) }.padding(vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(account.type.icon)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        if (account.purpose.isNotBlank()) {
                            Text("Used for: ${account.purpose}", style = MaterialTheme.typography.bodySmall, color = muted)
                        } else {
                            Text("Tap to add what you use this for", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(
                        Money.format(account.balanceMinor),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (account.balanceMinor < 0) money.negative else MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (dueFromHere > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (shortBy > 0) "${Money.format(dueFromHere)} in EMIs and bills is due from here before payday. Short by ${Money.format(shortBy)}."
                        else "${Money.format(dueFromHere)} in EMIs and bills is due from here before payday.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (shortBy > 0) money.negative else muted,
                        fontWeight = if (shortBy > 0) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
        if (unassigned > 0) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                if (unassigned == 1) "1 upcoming payment has no account chosen. Tap to choose where it is paid from."
                else "$unassigned upcoming payments have no account chosen. Tap to choose where they are paid from.",
                style = MaterialTheme.typography.bodySmall,
                color = money.warning,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDues).padding(top = 12.dp),
            )
        }
    }
}
