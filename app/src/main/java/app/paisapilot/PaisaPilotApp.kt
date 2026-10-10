package app.paisapilot

import android.app.Application
import android.util.Log
import app.paisapilot.data.remote.FirebaseProvider
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import app.paisapilot.core.notifications.Notifier
import app.paisapilot.core.notifications.ReminderScheduler
import app.paisapilot.core.security.AppLockManager
import app.paisapilot.data.sync.SyncScheduler
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PaisaPilotApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var reminders: ReminderScheduler
    @Inject lateinit var appLock: AppLockManager
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var firebase: FirebaseProvider

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Crash reports come only from real releases, not from development builds.
        if (firebase.isConfigured) {
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
        }
        notifier.createChannels()
        appLock.start()
        reminders.scheduleDaily()
        syncScheduler.requestSync()
        // Registers this device for push straight away, so campaigns reach users who have not signed in.
        firebase.messaging?.token?.addOnSuccessListener { token ->
            if (BuildConfig.DEBUG) Log.d("PushToken", token)
        }
    }
}
