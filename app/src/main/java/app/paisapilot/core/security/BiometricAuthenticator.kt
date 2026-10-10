package app.paisapilot.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Fingerprint / face unlock, falling back to the phone's own PIN, pattern or password.
 * The fallback means a failed sensor never locks someone out of their own data.
 */
object BiometricAuthenticator {

    private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    enum class Availability { READY, NOT_SET_UP, UNSUPPORTED }

    fun availability(context: Context): Availability =
        when (BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NOT_SET_UP
            else -> Availability.UNSUPPORTED
        }

    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit = {},
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                onFailure(errString.toString())
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info)
    }
}
