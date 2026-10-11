package app.paisapilot.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
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
import app.paisapilot.core.designsystem.AvatarBadge
import app.paisapilot.core.designsystem.IconBadge
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.designsystem.TabScaffold
import app.paisapilot.core.designsystem.tabListPadding
import app.paisapilot.core.premium.Feature
import app.paisapilot.feature.premium.LockedFeatureCard
import app.paisapilot.navigation.Routes

@Composable
fun MoreScreen(onNavigate: (String) -> Unit, viewModel: MoreViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TabScaffold(title = "More") { inner ->
        LazyColumn(
            contentPadding = tabListPadding(inner),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state?.let { current ->
                item { ProfileCard(current, onClick = { onNavigate(Routes.PROFILE) }) }
                item {
                    if (current.isPremium) {
                        MenuCard(Icons.Rounded.Forum, "Money coach", "Ask about your loans, bills and spending", MaterialTheme.colorScheme.primary) {
                            onNavigate(Routes.COACH)
                        }
                    } else {
                        LockedFeatureCard(Feature.AI_COACH, onClick = { onNavigate(Routes.PREMIUM) })
                    }
                }
            }
            item {
                MenuCard(Icons.Rounded.Insights, "Financial health", "Your score out of 100 and how to improve it", MaterialTheme.colorScheme.primary) {
                    onNavigate(Routes.INSIGHTS)
                }
            }
            item {
                MenuCard(Icons.Rounded.Shield, "Stay safe from scams", "Spot fraud calls, fake links and UPI tricks", MaterialTheme.colorScheme.secondary) {
                    onNavigate(Routes.SAFETY)
                }
            }
            item {
                MenuCard(Icons.Rounded.WorkspacePremium, "Premium", "AI coach, statement analysis, insurance vault and more", MaterialTheme.colorScheme.tertiary) {
                    onNavigate(Routes.PREMIUM)
                }
            }
            item {
                MenuCard(Icons.Rounded.Settings, "Settings", "App lock, reminders, backup", MaterialTheme.colorScheme.onSurfaceVariant) {
                    onNavigate(Routes.SETTINGS)
                }
            }
        }
    }
}

/** The user's avatar and money personality; tap to edit the profile behind it. */
@Composable
private fun ProfileCard(state: MoreUiState, onClick: () -> Unit) {
    val persona = state.persona
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarBadge(state.profile.avatar, size = 64.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(state.profile.name.ifBlank { "Your profile" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(persona.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Text("Edit", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(12.dp))
        Text(persona.summary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Text("Next step: ${persona.nextStep}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        persona.lifeStage?.let {
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MenuCard(icon: ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint = tint)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
