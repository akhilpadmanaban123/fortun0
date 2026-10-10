package app.paisapilot.feature.safety

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paisapilot.core.designsystem.DetailScaffold
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.designsystem.SectionHeader

private const val CYBER_HELPLINE = "1930"
private const val CYBER_PORTAL = "https://cybercrime.gov.in"

@Composable
fun SafetyScreen(onBack: () -> Unit) {
    DetailScaffold(title = "Stay safe from scams", onBack = onBack) {
        EmergencyCard()
        SectionHeader("Know the tricks")
        safetyTips.forEachIndexed { index, tip -> TipCard(tip, initiallyOpen = index == 0) }
    }
}

/** What to do in the first minutes after losing money, when acting fast can still freeze it. */
@Composable
private fun EmergencyCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Lost money to a fraud?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(4.dp))
            Text(
                "Call the national cyber crime helpline straight away. Reporting within the first hour gives the best chance of freezing the money. Then call your bank to block the card or account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    // ACTION_DIAL only opens the dialler with the number filled in; the user places the call.
                    onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$CYBER_HELPLINE"))) } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Call $CYBER_HELPLINE") }
                OutlinedButton(
                    onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CYBER_PORTAL))) } },
                ) { Text("Report online", color = MaterialTheme.colorScheme.onErrorContainer) }
            }
        }
    }
}

@Composable
private fun TipCard(tip: SafetyTip, initiallyOpen: Boolean) {
    var open by rememberSaveable { mutableStateOf(initiallyOpen) }
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { open = !open },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tip.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = if (open) "Collapse" else "Expand")
        }
        AnimatedVisibility(open) {
            Column {
                Spacer(Modifier.height(8.dp))
                Text(tip.howItWorks, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                tip.whatToDo.forEach { line ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("•  ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
