package app.paisapilot.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paisapilot.core.designsystem.BarPair
import app.paisapilot.core.designsystem.ChartPalette
import app.paisapilot.core.designsystem.DailyBars
import app.paisapilot.core.designsystem.LegendItem
import app.paisapilot.core.designsystem.PairedBars
import app.paisapilot.core.designsystem.ScoreRing
import app.paisapilot.core.designsystem.StackedBar
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import app.paisapilot.core.designsystem.ChartSlice
import app.paisapilot.core.designsystem.DonutChart
import app.paisapilot.core.designsystem.LocalMoneyColors
import app.paisapilot.core.designsystem.ProgressLine
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.designsystem.Stat
import app.paisapilot.core.finance.MoneySnapshot
import app.paisapilot.core.model.CategoryInfo
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.daysBetween
import app.paisapilot.core.util.short
import app.paisapilot.navigation.Routes

/** The headline number: what can be spent before payday without touching money owed. */
@Composable
internal fun SafeToSpendCard(s: MoneySnapshot) {
    val money = LocalMoneyColors.current
    val onHero = money.onHero
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(money.heroStart, money.heroEnd)))
            .padding(20.dp),
    ) {
        Text(
            "Safe to spend until payday (${s.nextPayday.short()})",
            style = MaterialTheme.typography.labelLarge,
            color = onHero.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            Money.format(s.safeToSpendMinor),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = onHero,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                s.liquidMinor < 0 -> "Your balance is below zero. Update your account balances in Wallet."
                s.safeToSpendMinor < 0 -> "Your dues before payday are more than your balance."
                s.liquidMinor == 0L -> "Add your accounts to see what is safe to spend."
                else -> "About ${Money.format(s.safePerDayMinor)} a day for ${s.daysToPayday} days"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = onHero.copy(alpha = 0.9f),
        )
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            HeroStat("Balance now", Money.format(s.liquidMinor), onHero, Modifier.weight(1f))
            HeroStat("Due before payday", Money.format(s.duesBeforePaydayMinor), onHero, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.75f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
internal fun OverdueBanner(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.width(10.dp))
        Text(
            if (count == 1) "1 payment is overdue. Tap to review." else "$count payments are overdue. Tap to review.",
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun ThisMonthCard(s: MoneySnapshot) {
    val money = LocalMoneyColors.current
    val hasIncome = s.monthIncomeMinor > 0
    val used = if (hasIncome) s.monthSpendMinor.toFloat() / s.monthIncomeMinor else 0f
    val ringColor = when {
        used >= 1f -> money.negative
        used >= 0.8f -> money.warning
        else -> MaterialTheme.colorScheme.primary
    }
    SectionCard {
        Text("This month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The ring fills as income gets spent, so one glance shows how far through it you are.
            ScoreRing((used * 100).toInt(), 100, ringColor, diameter = 116.dp, thickness = 12.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (hasIncome) "${(used * 100).toInt()}%" else "–",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(if (s.incomeIsExpected) "Income (expected)" else "Income", Money.format(s.monthIncomeMinor), valueColor = money.positive)
                Stat("Spent", Money.format(s.monthSpendMinor))
                // With no income known, "remaining" would just be the spending shown as a loss.
                if (hasIncome) {
                    Stat(
                        "Remaining", Money.format(s.remainingMinor),
                        valueColor = if (s.remainingMinor < 0) money.negative else MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    Stat("Remaining", "Add income", valueColor = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun ForecastCard(s: MoneySnapshot) {
    val money = LocalMoneyColors.current
    val daysLeft = daysBetween(s.today, YearMonth.from(s.today).atEndOfMonth())
    SectionCard {
        Text("Expected month-end balance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(
            Money.format(s.monthEndForecastMinor),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (s.monthEndForecastMinor < 0) money.negative else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        // The working, so the figure can be checked rather than taken on trust.
        ForecastLine("In cash and accounts now", Money.format(s.liquidMinor))
        if (s.forecastIncomeMinor > 0) ForecastLine("Salary still to come", "+" + Money.format(s.forecastIncomeMinor))
        ForecastLine("EMIs, bills and card dues left this month", "-" + Money.format(s.forecastDuesMinor))
        ForecastLine(
            "Everyday spending, ${Money.format(s.everydayPerDayMinor)} a day for $daysLeft days",
            "-" + Money.format(s.forecastSpendMinor),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (s.monthEndForecastMinor < 0) "You are on course to run short. Hold back on non-essentials, or check your account balances are up to date."
            else "Everyday spending is your daily average so far, leaving out rent, EMIs, bills and large one-off payments.",
            style = MaterialTheme.typography.bodySmall,
            color = if (s.monthEndForecastMinor < 0) money.negative else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ForecastLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun MoneyLocationCard(s: MoneySnapshot, onClick: () -> Unit) {
    val slices = listOf(
        ChartSlice("Cash", s.cashMinor, ChartPalette[2]),
        ChartSlice("Bank accounts", s.bankMinor, ChartPalette[0]),
        ChartSlice("Deposits", s.lockedMinor, ChartPalette[1]),
    )
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Where your money is", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            Money.format(s.liquidMinor + s.lockedMinor),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        StackedBar(slices)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            slices.forEach { LegendItem(it.color, it.label, Money.compact(it.value), Modifier.weight(1f)) }
        }
    }
}

@Composable
internal fun HealthCard(score: Int, onClick: () -> Unit) {
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Financial health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "See what is helping and hurting your score",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("$score", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("/100", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val monthLabel = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

/** A bar for every day of the month, so heavy days and quiet stretches stand out. */
@Composable
internal fun DailySpendCard(daily: List<Long>, s: MoneySnapshot) {
    val todayIndex = s.today.dayOfMonth - 1
    val soFar = daily.take(todayIndex + 1)
    val average = if (soFar.isEmpty()) 0 else soFar.sum() / soFar.size
    val peakIndex = soFar.indices.maxByOrNull { soFar[it] }
    SectionCard {
        Text("Spending by day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(
            "About ${Money.format(average)} a day so far" +
                if (peakIndex != null && soFar[peakIndex] > 0) ". Biggest: ${Money.format(soFar[peakIndex])} on ${s.today.withDayOfMonth(peakIndex + 1).short()}" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        DailyBars(daily, todayIndex, color = MaterialTheme.colorScheme.primary, todayColor = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("1", "${daily.size / 2}", "${daily.size}").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        LegendItem(MaterialTheme.colorScheme.tertiary, "Today")
    }
}

/** Income beside spending for each of the last six months. */
@Composable
internal fun TrendCard(trend: List<MonthTotal>) {
    val money = LocalMoneyColors.current
    val spentColor = MaterialTheme.colorScheme.secondary
    SectionCard {
        Text("Last ${trend.size} months", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LegendItem(money.positive, "Income")
            LegendItem(spentColor, "Spent")
        }
        Spacer(Modifier.height(14.dp))
        PairedBars(
            pairs = trend.map { BarPair(it.month.format(monthLabel), it.incomeMinor, it.spentMinor) },
            firstColor = money.positive,
            secondColor = spentColor,
        )
        // This month is unfinished, so compare the daily pace rather than the totals.
        val current = trend.lastOrNull()
        val previous = trend.getOrNull(trend.size - 2)
        if (current != null && previous != null && previous.spentMinor > 0 && current.spentMinor > 0) {
            Spacer(Modifier.height(10.dp))
            val paceNow = current.spentMinor / LocalDate.now().dayOfMonth
            val paceBefore = previous.spentMinor / previous.month.lengthOfMonth()
            val last = previous.month.format(monthLabel)
            Text(
                "You are spending about ${Money.format(paceNow)} a day this month, against ${Money.format(paceBefore)} a day in $last.",
                style = MaterialTheme.typography.bodySmall,
                color = if (paceNow <= paceBefore) MaterialTheme.colorScheme.onSurfaceVariant else money.warning,
            )
        }
    }
}

/** How much of what was borrowed has been paid back, across all running loans. */
@Composable
internal fun DebtCard(s: MoneySnapshot, principalMinor: Long, onClick: () -> Unit) {
    val repaid = (principalMinor - s.loanOutstandingMinor).coerceAtLeast(0)
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Loans paid off", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "${(repaid * 100 / principalMinor.coerceAtLeast(1)).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(10.dp))
        ProgressLine(repaid.toFloat() / principalMinor.coerceAtLeast(1))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Still to repay", Money.format(s.loanOutstandingMinor), Modifier.weight(1f))
            Stat("EMIs every month", Money.format(s.monthlyEmiMinor), Modifier.weight(1f))
        }
    }
}

private const val MAX_SLICES = 5

@Composable
internal fun SpendingCard(s: MoneySnapshot) {
    // Largest categories get their own slice; the long tail is grouped so the ring stays readable.
    val sorted = s.spendByCategory.entries.sortedByDescending { it.value }
    val top = sorted.take(MAX_SLICES).mapIndexed { i, (name, value) ->
        ChartSlice(CategoryInfo.of(name).label, value, ChartPalette[i])
    }
    val rest = sorted.drop(MAX_SLICES).sumOf { it.value }
    val slices = if (rest > 0) top + ChartSlice("Everything else", rest, ChartPalette[MAX_SLICES]) else top

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(slices) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Money.compact(s.monthSpendMinor), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slices.forEach { slice ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(slice.color))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(slice.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(slice.value), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

/** First-run guide: the three things that make the dashboard useful. */
@Composable
internal fun GettingStartedCard(onNavigate: (String) -> Unit) {
    SectionCard {
        Text("Let's get you set up", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Add these and your dashboard will show what is safe to spend, what is due next and where you will be at month end.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StartStep(Icons.Rounded.AccountBalanceWallet, "Add where your money is", "Cash, bank accounts, wallets") { onNavigate(Routes.accountEdit()) }
            StartStep(Icons.Rounded.AccountBalance, "Add a loan or EMI", "Home, personal, bike, phone EMI") { onNavigate(Routes.loanEdit()) }
            StartStep(Icons.Rounded.EventNote, "Add a bill", "Rent, recharge, insurance, SIP") { onNavigate(Routes.billEdit()) }
        }
    }
}

@Composable
private fun StartStep(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
    }
}
