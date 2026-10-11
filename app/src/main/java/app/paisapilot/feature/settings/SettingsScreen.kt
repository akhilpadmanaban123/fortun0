package app.paisapilot.feature.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.BuildConfig
import app.paisapilot.core.designsystem.ConfirmDialog
import app.paisapilot.core.designsystem.DetailScaffold
import app.paisapilot.core.designsystem.DropdownField
import app.paisapilot.core.designsystem.SectionCard
import app.paisapilot.core.designsystem.Stat
import app.paisapilot.core.designsystem.SwitchRow
import app.paisapilot.core.security.BiometricAuthenticator
import app.paisapilot.core.security.BiometricAuthenticator.Availability
import app.paisapilot.core.util.Links
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.ordinal
import app.paisapilot.data.repository.AuthState
import app.paisapilot.feature.lock.findActivity

private enum class Confirm { SIGN_OUT, SIGN_OUT_UNSYNCED, ERASE }

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var lockMessage by remember { mutableStateOf<String?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    val profile = state.profile

    DetailScaffold(title = "Settings", onBack = onBack) {
        Section("Profile") {
            Row(Modifier.fillMaxWidth()) {
                Stat("Name", profile.name.ifBlank { "Not set" }, Modifier.weight(1f))
                Stat("Payday", if (profile.payday in 1..31) ordinal(profile.payday) else "Not set", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Stat("Monthly income", if (profile.monthlyIncomeMinor > 0) Money.format(profile.monthlyIncomeMinor) else "Not set")
            TextButton(onClick = onEditProfile) { Text("Edit profile") }
        }

        Section("Security") {
            SwitchRow(
                title = "App lock",
                subtitle = "Ask for fingerprint, face or your phone's PIN when opening the app.",
                checked = profile.biometricLock,
                onCheckedChange = { enable ->
                    lockMessage = null
                    val activity = context.findActivity()
                    when {
                        activity == null -> Unit
                        BiometricAuthenticator.availability(context) != Availability.READY ->
                            lockMessage = "Set up a screen lock or fingerprint in your phone's settings first."
                        // Confirm identity both ways, so nobody else can switch the lock off.
                        else -> BiometricAuthenticator.prompt(
                            activity = activity,
                            title = if (enable) "Turn on app lock" else "Turn off app lock",
                            subtitle = "Confirm it's you",
                            onSuccess = { viewModel.setBiometricLock(enable) },
                        )
                    }
                },
            )
            Note(lockMessage, error = true)
        }

        Section("Reminders") {
            SwitchRow(
                title = "EMI & bill reminders",
                subtitle = "A notification before each due date, on the day, and if it becomes overdue.",
                checked = profile.reminders,
                onCheckedChange = viewModel::setReminders,
            )
            if (profile.reminders) {
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    label = "First reminder",
                    selected = profile.reminderDaysBefore,
                    options = listOf(2, 3, 5, 7),
                    optionLabel = { "$it days before" },
                    onSelected = viewModel::setReminderDays,
                )
                TextButton(onClick = viewModel::checkRemindersNow) { Text("Check reminders now") }
                Note(state.reminderMessage)
            }
        }

        Section("Phone permission") {
            Text(
                "Reminders and nudges only appear if notifications are allowed for Fortun0 on this phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { openNotificationSettings(context) }) { Text("Open notification settings") }
        }

        Section("Daily summary") {
            SwitchRow(
                title = "End-of-day summary",
                subtitle = "What you spent today against today's limit, and a quick check on how you feel about it.",
                checked = profile.dailySummary,
                onCheckedChange = viewModel::setDailySummary,
            )
            if (profile.dailySummary) {
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    label = "Around what time",
                    selected = profile.dailySummaryHour,
                    options = (18..23).toList(),
                    optionLabel = { hourLabel(it) },
                    onSelected = viewModel::setDailySummaryHour,
                    hint = "Your phone may deliver it a little later to save battery.",
                )
                TextButton(onClick = viewModel::sendDailySummaryNow) { Text("Send today's summary now") }
                Note(state.summaryMessage)
            }
        }

        Section("Nudges & quotes") {
            SwitchRow(
                title = "Tracking nudges",
                subtitle = "A short reminder to log your spending, or a money quote. Skipped on days you have already logged spending and get a daily summary.",
                checked = profile.nudges,
                onCheckedChange = viewModel::setNudges,
            )
            if (profile.nudges) {
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    label = "How often",
                    selected = profile.nudgeEveryDays,
                    options = listOf(1, 2, 3, 7),
                    optionLabel = { nudgeFrequencyLabel(it) },
                    onSelected = viewModel::setNudgeEveryDays,
                )
                Spacer(Modifier.height(8.dp))
                DropdownField(
                    label = "Around what time",
                    selected = profile.nudgeHour,
                    options = (7..22).toList(),
                    optionLabel = { hourLabel(it) },
                    onSelected = viewModel::setNudgeHour,
                    hint = "Your phone may deliver it a little later to save battery.",
                )
                TextButton(onClick = viewModel::sendNudgeNow) { Text("Send one now") }
                Note(state.nudgeMessage)
            }
        }

        Section("Backup & account") {
            when (val auth = state.auth) {
                is AuthState.SignedIn -> {
                    Text("Signed in as ${auth.email}", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (state.lastSyncAt > 0) "Last backed up ${DateUtils.getRelativeTimeSpanString(state.lastSyncAt)}" else "Not backed up yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Note(state.backupMessage)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = viewModel::syncNow, enabled = !state.busy) { Text("Back up now") }
                        TextButton(onClick = { confirm = Confirm.SIGN_OUT }, enabled = !state.busy) { Text("Sign out") }
                    }
                    TextButton(onClick = { showDelete = true }, enabled = !state.busy) {
                        Text("Delete account", color = MaterialTheme.colorScheme.error)
                    }
                }
                AuthState.SignedOut -> {
                    Text("Your data is only on this phone", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Create a free account to back it up and use it on another phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = viewModel::createAccount) { Text("Create account or sign in") }
                }
                AuthState.Unavailable -> {
                    Text("Your data is only on this phone", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Cloud backup is not set up in this build of the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.auth !is AuthState.SignedIn) {
                TextButton(onClick = { confirm = Confirm.ERASE }) {
                    Text("Erase all data", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        TextButton(onClick = { Links.open(context, Links.PRIVACY_POLICY) }) { Text("Privacy policy") }
        Text(
            "Fortun0 ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    when (confirm) {
        Confirm.SIGN_OUT -> ConfirmDialog(
            title = "Sign out?",
            message = "Your data is backed up first, then removed from this phone. Sign in again to get it back.",
            confirmLabel = "Sign out",
            onConfirm = {
                confirm = null
                viewModel.signOut(onNeedsConfirmation = { confirm = Confirm.SIGN_OUT_UNSYNCED })
            },
            onDismiss = { confirm = null },
        )
        Confirm.SIGN_OUT_UNSYNCED -> ConfirmDialog(
            title = "Recent changes are not backed up",
            message = "We could not reach the server. If you sign out now, changes made since the last backup will be lost.",
            confirmLabel = "Sign out anyway",
            destructive = true,
            onConfirm = {
                confirm = null
                viewModel.signOut(force = true, onNeedsConfirmation = {})
            },
            onDismiss = { confirm = null },
        )
        Confirm.ERASE -> ConfirmDialog(
            title = "Erase all data?",
            message = "Every account, loan, bill and transaction on this phone will be deleted permanently. There is no backup to restore from.",
            confirmLabel = "Erase everything",
            destructive = true,
            onConfirm = {
                confirm = null
                viewModel.eraseEverything()
            },
            onDismiss = { confirm = null },
        )
        null -> Unit
    }

    val signedIn = state.auth as? AuthState.SignedIn
    if (showDelete && signedIn != null) {
        DeleteAccountDialog(
            email = signedIn.email,
            busy = state.busy,
            error = state.deleteError,
            onConfirm = viewModel::deleteAccount,
            onDismiss = {
                showDelete = false
                viewModel.clearDeleteError()
            },
        )
    }
}

/** The phone's own notification page for this app, where a refused permission can be turned back on. */
private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    runCatching { context.startActivity(intent) }
}

private fun nudgeFrequencyLabel(days: Int) = when (days) {
    1 -> "Every day"
    7 -> "Once a week"
    else -> "Every $days days"
}

/** 20 -> "8 PM". */
private fun hourLabel(hour: Int): String {
    val twelve = if (hour % 12 == 0) 12 else hour % 12
    return "$twelve ${if (hour < 12) "AM" else "PM"}"
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    SectionCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

@Composable
private fun Note(text: String?, error: Boolean = false) {
    if (text == null) return
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
}
