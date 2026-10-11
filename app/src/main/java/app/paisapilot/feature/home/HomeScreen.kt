package app.paisapilot.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.designsystem.SectionHeader
import app.paisapilot.core.designsystem.TabScaffold
import app.paisapilot.core.designsystem.tabListPadding
import app.paisapilot.core.model.TxnType
import app.paisapilot.feature.activity.TxnRow
import app.paisapilot.feature.dues.DueRow
import app.paisapilot.navigation.Routes

private const val UPCOMING_ON_HOME = 4

@Composable
fun HomeScreen(
    onOpenTab: (String) -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val overview = state.overview
    val greeting = overview?.profile?.firstName?.takeIf { it.isNotBlank() }?.let { "Hi, $it" } ?: "Fortun0"

    TabScaffold(
        title = greeting,
        fabLabel = "Add",
        onFab = { onNavigate(Routes.txnAdd(TxnType.EXPENSE.name)) },
        actions = {
            IconButton(onClick = { onNavigate(Routes.SAFETY) }) {
                Icon(Icons.Rounded.Shield, contentDescription = "Scam safety tips")
            }
        },
    ) { inner ->
        if (overview == null) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@TabScaffold
        }
        val snapshot = overview.snapshot
        val accountNames = overview.accounts.associate { it.id to it.name }
        LazyColumn(
            contentPadding = tabListPadding(inner, hasFab = true),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isEmpty) {
                item { GettingStartedCard(onNavigate) }
                return@LazyColumn
            }

            item { SafeToSpendCard(snapshot) }

            if (snapshot.overdueCount > 0) {
                item { OverdueBanner(snapshot.overdueCount, onClick = { onOpenTab(Routes.DUES) }) }
            }

            item { ThisMonthCard(snapshot) }
            if (overview.accounts.isNotEmpty()) {
                item {
                    AccountsCard(
                        accounts = overview.accounts,
                        s = snapshot,
                        onOpenAccount = { onNavigate(Routes.accountEdit(it)) },
                        onOpenDues = { onOpenTab(Routes.DUES) },
                    )
                }
            }
            if (snapshot.monthSpendMinor > 0) {
                item { DailySpendCard(state.dailySpend, snapshot) }
            }
            item { ForecastCard(snapshot) }

            item {
                SectionHeader("Upcoming EMIs & bills", action = "See all", onAction = { onOpenTab(Routes.DUES) })
            }
            item {
                SectionCard {
                    if (snapshot.upcoming.isEmpty()) {
                        Text(
                            "Nothing due in the coming weeks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            snapshot.upcoming.take(UPCOMING_ON_HOME).forEach {
                                DueRow(it, snapshot.today, accountName = accountNames[it.accountId])
                            }
                        }
                    }
                }
            }

            if (snapshot.spendByCategory.isNotEmpty()) {
                item { SectionHeader("Where it went this month") }
                item { SpendingCard(snapshot) }
            }

            item { SectionHeader("The bigger picture") }
            // Only worth drawing once there is something from before this month to compare with.
            if (state.trend.dropLast(1).any { it.incomeMinor > 0 || it.spentMinor > 0 }) {
                item { TrendCard(state.trend) }
            }
            item { MoneyLocationCard(snapshot, onClick = { onOpenTab(Routes.WALLET) }) }
            if (state.loanPrincipalMinor > 0) {
                item { DebtCard(snapshot, state.loanPrincipalMinor, onClick = { onOpenTab(Routes.DUES) }) }
            }
            item { HealthCard(state.healthScore, onClick = { onNavigate(Routes.INSIGHTS) }) }

            if (state.recent.isNotEmpty()) {
                item {
                    SectionHeader("Recent activity", action = "See all", onAction = { onOpenTab(Routes.ACTIVITY) })
                }
                item {
                    SectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            state.recent.forEach { TxnRow(it) }
                        }
                    }
                }
            }
        }
    }
}
