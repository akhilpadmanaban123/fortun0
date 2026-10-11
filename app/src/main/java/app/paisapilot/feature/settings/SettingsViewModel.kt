package app.paisapilot.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.notifications.DailySummaryScheduler
import app.paisapilot.core.notifications.Notifier
import app.paisapilot.core.notifications.NudgeScheduler
import app.paisapilot.core.notifications.ReminderScheduler
import app.paisapilot.data.prefs.ProfileStore
import app.paisapilot.data.prefs.UserProfile
import app.paisapilot.data.repository.AuthRepository
import app.paisapilot.data.repository.AuthState
import app.paisapilot.data.repository.SessionRepository
import app.paisapilot.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profile: UserProfile = UserProfile(),
    val auth: AuthState = AuthState.Unavailable,
    val lastSyncAt: Long = 0,
    val busy: Boolean = false,
    /** One-line result of the last action, shown under the section it belongs to. */
    val backupMessage: String? = null,
    val reminderMessage: String? = null,
    val nudgeMessage: String? = null,
    val summaryMessage: String? = null,
    val deleteError: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileStore: ProfileStore,
    auth: AuthRepository,
    private val session: SessionRepository,
    private val syncManager: SyncManager,
    private val reminders: ReminderScheduler,
    private val notifier: Notifier,
    private val nudges: NudgeScheduler,
    private val dailySummary: DailySummaryScheduler,
) : ViewModel() {

    private val transient = MutableStateFlow(SettingsUiState())

    val state: StateFlow<SettingsUiState> =
        combine(profileStore.profile, auth.state, profileStore.lastSyncAt, transient) { profile, authState, lastSync, t ->
            t.copy(profile = profile, auth = authState, lastSyncAt = lastSync)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch { profileStore.setBiometricLock(enabled) }
    }

    fun setReminders(enabled: Boolean) {
        viewModelScope.launch { profileStore.setReminders(enabled) }
    }

    fun setReminderDays(days: Int) {
        viewModelScope.launch { profileStore.setReminderDaysBefore(days) }
    }

    fun setDailySummary(enabled: Boolean) = changeDailySummary { profileStore.setDailySummary(enabled) }

    fun setDailySummaryHour(hour: Int) = changeDailySummary { profileStore.setDailySummaryHour(hour) }

    private fun changeDailySummary(save: suspend () -> Unit) {
        viewModelScope.launch {
            save()
            dailySummary.apply(profileStore.current(), replace = true)
            transient.update { it.copy(summaryMessage = null) }
        }
    }

    fun sendDailySummaryNow() {
        if (!notifier.canNotify()) {
            transient.update { it.copy(summaryMessage = "Notifications are turned off for this app. Allow them in your phone's settings.") }
            return
        }
        dailySummary.sendNow()
        transient.update { it.copy(summaryMessage = "Sent. Pull down your notifications to see it.") }
    }

    fun setNudges(enabled: Boolean) = changeNudges { profileStore.setNudges(enabled) }

    fun setNudgeEveryDays(days: Int) = changeNudges { profileStore.setNudgeEveryDays(days) }

    fun setNudgeHour(hour: Int) = changeNudges { profileStore.setNudgeHour(hour) }

    /** Saves a nudge setting, then reschedules so it takes effect from the next nudge. */
    private fun changeNudges(save: suspend () -> Unit) {
        viewModelScope.launch {
            save()
            nudges.apply(profileStore.current(), replace = true)
            transient.update { it.copy(nudgeMessage = null) }
        }
    }

    fun sendNudgeNow() {
        if (!notifier.canNotify()) {
            transient.update { it.copy(nudgeMessage = "Notifications are turned off for this app. Allow them in your phone's settings.") }
            return
        }
        nudges.sendNow()
        transient.update { it.copy(nudgeMessage = "Sent. Pull down your notifications to see it.") }
    }

    fun checkRemindersNow() {
        if (!notifier.canNotify()) {
            transient.update { it.copy(reminderMessage = "Notifications are turned off for this app. Allow them in your phone's settings.") }
            return
        }
        reminders.runNow()
        transient.update { it.copy(reminderMessage = "Checked. Anything due soon or overdue will appear as a notification.") }
    }

    fun syncNow() {
        viewModelScope.launch {
            transient.update { it.copy(busy = true, backupMessage = null) }
            val result = runCatching { syncManager.sync() }
            transient.update {
                it.copy(
                    busy = false,
                    backupMessage = when {
                        result.isFailure -> "Could not back up. Check your internet connection and try again."
                        result.getOrNull() == true -> "Backed up just now."
                        else -> "Sign in and verify your email to back up."
                    },
                )
            }
        }
    }

    /** Leaving local-only mode: the root screen switches to sign-in; existing data is kept and uploaded. */
    fun createAccount() {
        viewModelScope.launch { profileStore.setGuest(false) }
    }

    fun signOut(force: Boolean = false, onNeedsConfirmation: () -> Unit) {
        viewModelScope.launch {
            transient.update { it.copy(busy = true, backupMessage = null) }
            val done = session.signOut(force)
            transient.update { it.copy(busy = false) }
            if (!done) onNeedsConfirmation()
        }
    }

    /** On success the account no longer exists and the root screen returns to sign-in by itself. */
    fun deleteAccount(password: String) {
        viewModelScope.launch {
            transient.update { it.copy(busy = true, deleteError = null) }
            val result = session.deleteAccount(password)
            transient.update { it.copy(busy = false, deleteError = result.exceptionOrNull()?.message) }
        }
    }

    fun clearDeleteError() = transient.update { it.copy(deleteError = null) }

    fun eraseEverything() {
        viewModelScope.launch { session.eraseEverything() }
    }
}
