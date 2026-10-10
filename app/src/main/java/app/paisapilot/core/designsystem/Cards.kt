package app.paisapilot.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** The standard white surface used for every block of content. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val body: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(16.dp), content = content)
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colors, content = body)
    } else {
        Card(modifier = modifier.fillMaxWidth(), colors = colors, content = body)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

/** Round tinted badge holding an icon; the leading element of most list rows. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier.size(42.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Icon, title + subtitle on the left, value (and an optional note under it) on the right. */
@Composable
fun ListRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    value: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueNote: String? = null,
    valueNoteColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = valueColor)
                if (valueNote != null) {
                    Text(valueNote, style = MaterialTheme.typography.labelSmall, color = valueNoteColor)
                }
            }
        }
    }
}

/** Small labelled figure, used in pairs and triples. */
@Composable
fun Stat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = valueColor, maxLines = 1)
    }
}

@Composable
fun ProgressLine(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}
