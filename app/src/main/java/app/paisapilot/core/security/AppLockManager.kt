package app.paisapilot.core.security

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks whether the app is currently locked. It starts locked on a cold start and
 * re-locks after the app has been in the background longer than [GRACE_MS], so a quick
 * hop to copy an OTP or check a message does not ask for a fingerprint again.
 * Whether the lock is enforced at all is the user's setting (UserProfile.biometricLock).
 */
@Singleton
class AppLockManager @Inject constructor() : DefaultLifecycleObserver {

    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked

    private var backgroundedAt = 0L

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun unlock() {
        _locked.value = false
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart(owner: LifecycleOwner) {
        if (backgroundedAt != 0L && SystemClock.elapsedRealtime() - backgroundedAt > GRACE_MS) {
            _locked.value = true
        }
    }

    private companion object {
        const val GRACE_MS = 30_000L
    }
}
