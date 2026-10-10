package app.paisapilot.feature.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import app.paisapilot.core.security.BiometricAuthenticator
import app.paisapilot.feature.auth.BrandMark

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val activity = LocalContext.current.findActivity()
    val unlock = {
        if (activity != null) {
            BiometricAuthenticator.prompt(
                activity = activity,
                title = "Unlock Fortun0",
                subtitle = "Confirm it's you to see your money",
                onSuccess = onUnlocked,
            )
        }
    }
    LaunchedEffect(Unit) { unlock() }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        BrandMark()
        Text("Fortun0 is locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "Your financial details are protected.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = unlock, modifier = Modifier.height(52.dp)) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
            Text("  Unlock", style = MaterialTheme.typography.titleMedium)
        }
    }
}

fun Context.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}
