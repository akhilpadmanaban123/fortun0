package app.paisapilot.feature.insights

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.core.designsystem.DetailScaffold
import app.paisapilot.core.designsystem.LocalMoneyColors
import app.paisapilot.core.designsystem.ProgressLine
import app.paisapilot.core.designsystem.ScoreRing
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.designsystem.SectionHeader
import app.paisapilot.core.designsystem.Stat
import app.paisapilot.core.finance.HealthFactor
import app.paisapilot.core.premium.Feature
import app.paisapilot.core.util.Money
import app.paisapilot.feature.premium.LockedFeatureCard

@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailScaffold(title = "Financial health", onBack = onBack) {
        val current = state ?: return@DetailScaffold
        val report = current.report
        val money = LocalMoneyColors.current
        val scoreColor = bandColor(report.score, 100)

        SectionCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                ScoreRing(report.score, 100, scoreColor) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${report.score}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text("out of 100", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(report.grade, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = scoreColor)
                Spacer(Modifier.height(4.dp))
                Text(report.headline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }

        SectionCard {
            Row(Modifier.fillMaxWidth()) {
                Stat("Net worth", Money.format(current.snapshot.netWorthMinor), Modifier.weight(1f),
                    if (current.snapshot.netWorthMinor < 0) money.negative else MaterialTheme.colorScheme.onSurface)
                Stat("Total you owe", Money.format(current.snapshot.loanOutstandingMinor + current.snapshot.cardOutstandingMinor), Modifier.weight(1f))
            }
        }

        SectionHeader("What makes up your score")
        report.factors.forEach { FactorCard(it) }

        SectionHeader("Go further")
        LockedFeatureCard(Feature.FUTURE_OUTLOOK, onClick = onOpenPremium)
        LockedFeatureCard(Feature.PAYOFF_PLANNER, onClick = onOpenPremium)
        LockedFeatureCard(Feature.INSURANCE_AI, onClick = onOpenPremium)

        Text(
            "This score is a guide based on what you have entered here. It is not a credit score and not financial advice.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FactorCard(factor: HealthFactor) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(factor.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${factor.score}/${factor.max}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = bandColor(factor.score, factor.max))
        }
        Spacer(Modifier.height(10.dp))
        ProgressLine(factor.score.toFloat() / factor.max, color = bandColor(factor.score, factor.max))
        Spacer(Modifier.height(10.dp))
        Text(factor.detail, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text(factor.tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Green for a strong score, amber for middling, red for weak. */
@Composable
private fun bandColor(score: Int, max: Int): Color {
    val money = LocalMoneyColors.current
    val ratio = score.toFloat() / max
    return when {
        ratio >= 0.7f -> money.positive
        ratio >= 0.45f -> money.warning
        else -> money.negative
    }
}
