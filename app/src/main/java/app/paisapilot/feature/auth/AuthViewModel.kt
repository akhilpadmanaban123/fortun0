package app.paisapilot.feature.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.data.prefs.ProfileStore
import app.paisapilot.data.repository.AuthRepository
import app.paisapilot.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_IN, SIGN_UP }

data class AuthUiState(
    val cloudAvailable: Boolean,
    val mode: AuthMode = AuthMode.SIGN_IN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val profileStore: ProfileStore,
    private val session: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState(cloudAvailable = auth.isAvailable))
    val state: StateFlow<AuthUiState> = _state

    fun onName(value: String) = _state.update { it.copy(name = value, error = null) }
    fun onEmail(value: String) = _state.update { it.copy(email = value.trim(), error = null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun toggleMode() = _state.update {
        it.copy(
            mode = if (it.mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN,
            error = null,
            info = null,
        )
    }

    fun submit() {
        val s = _state.value
        val problem = when {
            s.mode == AuthMode.SIGN_UP && s.name.isBlank() -> "Please enter your name."
            !Patterns.EMAIL_ADDRESS.matcher(s.email).matches() -> "Please enter a valid email address."
            s.mode == AuthMode.SIGN_UP && s.password.length < MIN_PASSWORD -> "Use at least $MIN_PASSWORD characters for your password."
            s.password.isEmpty() -> "Please enter your password."
            else -> null
        }
        if (problem != null) {
            _state.update { it.copy(error = problem) }
            return
        }
        perform {
            val result = if (s.mode == AuthMode.SIGN_UP) auth.signUp(s.name, s.email, s.password)
            else auth.signIn(s.email, s.password)
            // This ViewModel outlives the sign-in screen, so wipe what was typed: the password
            // must not sit in memory, or reappear in the form after signing out.
            if (result.isSuccess) _state.value = AuthUiState(cloudAvailable = auth.isAvailable)
            result
        }
    }

    fun forgotPassword() {
        val email = _state.value.email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.update { it.copy(error = "Enter your email above, then tap Forgot password.") }
            return
        }
        perform(successInfo = "Password reset link sent to $email.") { auth.sendPasswordReset(email) }
    }

    fun continueWithoutAccount() {
        viewModelScope.launch { profileStore.setGuest(true) }
    }

    // --- Email verification screen ---

    fun resendVerification() =
        perform(successInfo = "Verification email sent again. Check spam too.") { auth.resendVerification() }

    /** [silent] is used by the automatic re-check, which should not flash errors. */
    fun checkVerified(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _state.update { it.copy(loading = true, error = null, info = null) }
            val result = auth.refresh()
            if (!silent) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = result.exceptionOrNull()?.message
                            ?: "Not verified yet. Open the link in the email we sent, then try again."
                                .takeIf { result.getOrNull() == false },
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { session.signOut(force = true) }
    }

    private fun perform(successInfo: String? = null, action: suspend () -> Result<*>) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            val result = action()
            _state.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message,
                    info = successInfo.takeIf { result.isSuccess },
                )
            }
        }
    }

    private companion object {
        const val MIN_PASSWORD = 8
    }
}
