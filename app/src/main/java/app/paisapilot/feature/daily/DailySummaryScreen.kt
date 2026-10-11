package app.paisapilot.feature.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.core.designsystem.DetailScaffold
import app.paisapilot.core.designsystem.LocalMoneyColors
import app.paisapilot.core.designsystem.ProgressLine
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.designsystem.SectionHeader
import app.paisapilot.core.designsystem.Stat
import app.paisapilot.core.finance.DailySummary
import app.paisapilot.core.finance.DayVerdict
import app.paisapilot.core.finance.Feeling
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.short
import app.paisapilot.feature.activity.TxnRow
import java.time.format.TextStyle
import java.util.Locale

/** Today's spending against today's limit, followed by "how do you feel about it?". */
@Composable
fun DailySummaryScreen(onBack: () -> Unit, viewModel: DailySummaryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailScaffold(title = "Today", onBack = onBack) {
        val current = state ?: return@DetailScaffold
        val summary = current.summary

        VerdictCard(summary)

        SectionCard {
            Text("How do you feel about today's spending?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            // Wraps onto a second line on narrow screens instead of squeezing the last choice.
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Feeling.entries.forEach { feeling ->
                    FilterChip(
                        selected = current.todayFeeling == feeling,
                        onClick = { viewModel.answer(feeling) },
                        leadingIcon = { Icon(feeling.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text(feeling.label) },
                    )
                }
            }
            current.todayFeeling?.let {
                Spacer(Modifier.height(8.dp))
                Text(it.reply, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(14.dp))
            // The week at a glance: one face per day, empty where no answer was given.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                current.week.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            day.feeling?.icon ?: Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = day.feeling?.label ?: "No answer",
                            tint = day.feeling?.color() ?: MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (summary.txns.isNotEmpty()) {
            SectionHeader("What you spent on")
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    summary.txns.forEach { TxnRow(it, showDate = false) }
                }
            }
        }
    }
}

@Composable
private fun VerdictCard(summary: DailySummary) {
    val money = LocalMoneyColors.current
    val color = when (summary.verdict) {
        DayVerdict.OVER, DayVerdict.NO_LIMIT -> money.negative
        DayVerdict.UNDER -> money.positive
        DayVerdict.ON_LIMIT, DayVerdict.NOTHING_RECORDED -> MaterialTheme.colorScheme.onSurface
    }
    SectionCard {
        Text(summary.date.short(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(summary.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(4.dp))
        Text(summary.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (summary.limitMinor > 0) {
            Spacer(Modifier.height(14.dp))
            ProgressLine(
                progress = summary.everydayMinor.toFloat() / summary.limitMinor,
                color = if (summary.verdict == DayVerdict.OVER) money.negative else MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Everyday spending", Money.format(summary.everydayMinor), Modifier.weight(1f))
            Stat("Today's limit", Money.format(summary.limitMinor), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Bills and one-offs", Money.format(summary.otherMinor), Modifier.weight(1f))
            Stat("Tomorrow's limit", summary.tomorrowLimitMinor?.let { Money.format(it) } ?: "Payday", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "The limit is what is safe to spend until payday, spread evenly over the days left. Rent, EMIs, bills and large one-off payments are not counted against it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val Feeling.icon: ImageVector
    get() = when (this) {
        Feeling.GOOD -> Icons.Rounded.SentimentVerySatisfied
        Feeling.OKAY -> Icons.Rounded.SentimentNeutral
        Feeling.REGRET -> Icons.Rounded.SentimentDissatisfied
    }

@Composable
private fun Feeling.color(): Color {
    val money = LocalMoneyColors.current
    return when (this) {
        Feeling.GOOD -> money.positive
        Feeling.OKAY -> money.warning
        Feeling.REGRET -> money.negative
    }
}
