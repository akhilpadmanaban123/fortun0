package app.paisapilot.data.remote

import android.content.Context
import app.paisapilot.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only place that touches Firebase singletons. When the app is built without
 * google-services.json, [isConfigured] is false and everything cloud-related is null,
 * so the rest of the app runs in local-only mode instead of crashing.
 */
@Singleton
class FirebaseProvider @Inject constructor(@ApplicationContext context: Context) {

    val isConfigured: Boolean = FirebaseApp.getApps(context).isNotEmpty()

    val auth: FirebaseAuth? by lazy {
        if (!isConfigured) null
        else FirebaseAuth.getInstance().also {
            if (BuildConfig.FIREBASE_EMULATOR) it.useEmulator(EMULATOR_HOST, 9099)
        }
    }

    val firestore: FirebaseFirestore? by lazy {
        if (!isConfigured) null
        else FirebaseFirestore.getInstance().also {
            if (BuildConfig.FIREBASE_EMULATOR) it.useEmulator(EMULATOR_HOST, 8080)
        }
    }

    val messaging: FirebaseMessaging? by lazy {
        if (!isConfigured) null else runCatching { FirebaseMessaging.getInstance() }.getOrNull()
    }

    private companion object {
        /** The development machine as seen from the Android emulator. */
        const val EMULATOR_HOST = "10.0.2.2"
    }
}
