package app.paisapilot.feature.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.security.AppLockManager
import app.paisapilot.data.prefs.ProfileStore
import app.paisapilot.data.repository.AuthRepository
import app.paisapilot.data.repository.AuthState
import app.paisapilot.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which top-level screen the app should be showing. */
sealed interface RootState {
    data object Loading : RootState
    data object Intro : RootState
    data object Auth : RootState
    data class VerifyEmail(val email: String) : RootState
    data class Onboarding(val suggestedName: String) : RootState
    data object Locked : RootState
    data object Main : RootState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    private val profileStore: ProfileStore,
    auth: AuthRepository,
    private val appLock: AppLockManager,
    private val syncManager: SyncManager,
) : ViewModel() {

    /** True while a freshly signed-in user's data is being fetched from the cloud. */
    private val restoring = MutableStateFlow(false)

    val state: StateFlow<RootState> =
        combine(profileStore.profile, auth.state, appLock.locked, restoring) { profile, authState, locked, restoring ->
            val user = authState as? AuthState.SignedIn
            when {
                !profile.introSeen && !profile.onboarded && user == null -> RootState.Intro
                user == null && !profile.guest -> RootState.Auth
                user != null && !user.emailVerified -> RootState.VerifyEmail(user.email)
                !profile.onboarded && restoring -> RootState.Loading
                !profile.onboarded -> RootState.Onboarding(user?.name.orEmpty())
                profile.biometricLock && locked -> RootState.Locked
                else -> RootState.Main
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootState.Loading)

    init {
        // On every verified sign-in, pull the user's existing data before deciding
        // whether they still need to go through onboarding.
        viewModelScope.launch {
            auth.state.map { (it as? AuthState.SignedIn)?.takeIf { u -> u.emailVerified }?.uid }
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid != null) {
                        restoring.value = true
                        runCatching { syncManager.sync() }
                        restoring.value = false
                    }
                }
        }
    }

    fun unlock() = appLock.unlock()

    fun finishIntro() {
        viewModelScope.launch { profileStore.setIntroSeen() }
    }
}
