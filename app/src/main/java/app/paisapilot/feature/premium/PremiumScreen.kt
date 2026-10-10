package app.paisapilot.feature.premium

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paisapilot.core.designsystem.DetailScaffold
import app.paisapilot.core.premium.Feature

@Composable
fun PremiumScreen(onBack: () -> Unit) {
    DetailScaffold(title = "Premium", onBack = onBack) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(8.dp))
            Text("Fortun0 Premium", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Everything you use today stays free. Premium adds deeper planning on top.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(4.dp))
        Feature.premium.forEach { LockedFeatureCard(it) }
        Spacer(Modifier.height(4.dp))
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Coming soon", style = MaterialTheme.typography.titleMedium)
        }
    }
}
