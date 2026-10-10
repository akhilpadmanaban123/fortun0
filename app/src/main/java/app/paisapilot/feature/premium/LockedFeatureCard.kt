package app.paisapilot.feature.premium

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paisapilot.core.designsystem.IconBadge
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.premium.Feature

/** A premium feature shown greyed out with a lock, wherever it will eventually live. */
@Composable
fun LockedFeatureCard(feature: Feature, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    SectionCard(modifier = modifier.alpha(0.6f), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Lock, tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(feature.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("PREMIUM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                }
                Text(feature.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
