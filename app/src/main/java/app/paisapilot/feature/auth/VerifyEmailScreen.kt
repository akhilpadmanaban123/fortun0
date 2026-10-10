package app.paisapilot.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/** Shown after sign-up until the user opens the verification link sent to their inbox. */
@Composable
fun VerifyEmailScreen(email: String, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Re-check quietly, so the app moves on by itself once the link has been opened.
    LaunchedEffect(Unit) {
        while (true) {
            delay(4_000)
            viewModel.checkVerified(silent = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Rounded.MarkEmailUnread, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
        Text("Verify your email", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "We sent a verification link to\n$email\n\nOpen it to confirm this is you. This screen will continue automatically.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Message(state.error, isError = true)
        Message(state.info, isError = false)
        Button(
            onClick = { viewModel.checkVerified() },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("I've verified", style = MaterialTheme.typography.titleMedium) }
        OutlinedButton(
            onClick = viewModel::resendVerification,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Resend email") }
        TextButton(onClick = viewModel::signOut) { Text("Use a different account") }
    }
}
