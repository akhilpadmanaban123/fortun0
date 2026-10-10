package app.paisapilot.data.repository

import app.paisapilot.data.remote.FirebaseProvider
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

sealed interface AuthState {
    /** The app was built without Firebase; only local mode is possible. */
    data object Unavailable : AuthState
    data object SignedOut : AuthState
    data class SignedIn(
        val uid: String,
        val email: String,
        val name: String,
        val emailVerified: Boolean,
    ) : AuthState
}

@Singleton
class AuthRepository @Inject constructor(private val firebase: FirebaseProvider) {

    private val _state = MutableStateFlow(firebase.auth?.currentUser.toState())
    val state: StateFlow<AuthState> = _state

    val isAvailable: Boolean get() = firebase.isConfigured

    /** Uid of a signed-in user whose email is verified; null otherwise. */
    val verifiedUid: String?
        get() = (state.value as? AuthState.SignedIn)?.takeIf { it.emailVerified }?.uid

    init {
        firebase.auth?.addAuthStateListener { _state.value = it.currentUser.toState() }
    }

    suspend fun signUp(name: String, email: String, password: String): Result<Unit> = attempt {
        val auth = requireAuth()
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Could not create the account.")
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        user.sendEmailVerification().await()
        _state.value = user.toState()
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = attempt {
        requireAuth().signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun resendVerification(): Result<Unit> = attempt {
        requireAuth().currentUser?.sendEmailVerification()?.await() ?: error("Please sign in again.")
    }

    /** Re-reads the account from the server; returns true once the email is verified. */
    suspend fun refresh(): Result<Boolean> = attempt {
        val user = requireAuth().currentUser ?: error("Please sign in again.")
        user.reload().await()
        // The ID token is what Firestore rules see, so it must be re-minted after verification.
        if (user.isEmailVerified) user.getIdToken(true).await()
        _state.value = user.toState()
        user.isEmailVerified
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = attempt {
        requireAuth().sendPasswordResetEmail(email.trim()).await()
    }

    /** Firebase only allows deleting an account right after the user has proved who they are. */
    suspend fun reauthenticate(password: String): Result<Unit> = attempt {
        val user = requireAuth().currentUser ?: error("Please sign in again.")
        val email = user.email ?: error("Please sign in again.")
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
    }

    /** Permanently removes the sign-in itself. Call [reauthenticate] first. */
    suspend fun deleteUser(): Result<Unit> = attempt {
        requireAuth().currentUser?.delete()?.await() ?: error("Please sign in again.")
    }

    fun signOut() {
        firebase.auth?.signOut()
    }

    private fun requireAuth() = firebase.auth ?: error("Cloud accounts are not set up in this build.")

    private fun FirebaseUser?.toState(): AuthState = when {
        !firebase.isConfigured -> AuthState.Unavailable
        this == null -> AuthState.SignedOut
        else -> AuthState.SignedIn(uid, email.orEmpty(), displayName.orEmpty(), isEmailVerified)
    }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(IllegalStateException(friendlyMessage(e), e))
    }

    private fun friendlyMessage(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Choose a stronger password (at least 6 characters)."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists. Try signing in."
        is FirebaseAuthInvalidUserException -> "No account found with this email."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a few minutes and try again."
        is FirebaseNetworkException -> "No internet connection. Please try again."
        else -> e.message ?: "Something went wrong. Please try again."
    }
}
