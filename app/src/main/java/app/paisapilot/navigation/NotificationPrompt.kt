package app.paisapilot.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.paisapilot.data.prefs.ProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationPromptViewModel @Inject constructor(private val profileStore: ProfileStore) : ViewModel() {

    /** Null until the saved answer has loaded, so the explanation never flashes up by mistake. */
    val alreadyAsked: StateFlow<Boolean?> = profileStore.profile
        .map<_, Boolean?> { it.notificationPromptSeen }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun markAsked() {
        viewModelScope.launch { profileStore.setNotificationPromptSeen() }
    }
}

/**
 * Android 13+ needs the user's permission before any notification can be shown. Android's own
 * prompt gives no reason and can only be shown twice, so we explain what the notifications
 * are for first and only raise the system prompt if the user says yes. Asked once; after
 * "Not now" the way back is the notification settings link in Settings.
 */
@Composable
fun NotificationPermissionPrompt(viewModel: NotificationPromptViewModel = hiltViewModel()) {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val alreadyAsked by viewModel.alreadyAsked.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    if (granted || alreadyAsked != false) return

    AlertDialog(
        onDismissRequest = viewModel::markAsked,
        icon = { Icon(Icons.Rounded.NotificationsActive, contentDescription = null) },
        title = { Text("Never miss a due date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Allow notifications and Fortun0 will:")
                Reason("Warn you before an EMI, bill or card payment is due")
                Reason("Tell you if something becomes overdue")
                Reason("Nudge you to log your spending, at most once a day")
                Text(
                    "No ads, no promotions. You can switch any of these off in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.markAsked()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text("Allow notifications") }
        },
        dismissButton = { TextButton(onClick = viewModel::markAsked) { Text("Not now") } },
    )
}

@Composable
private fun Reason(text: String) {
    Row {
        Text("•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text, modifier = Modifier.padding(start = 8.dp))
    }
}
