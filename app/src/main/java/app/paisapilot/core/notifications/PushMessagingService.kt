package app.paisapilot.core.notifications

import android.util.Log
import app.paisapilot.BuildConfig
import app.paisapilot.data.sync.SyncScheduler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives Firebase Cloud Messaging pushes (campaigns from the Firebase console today,
 * server-sent alerts later). Due-date reminders do not depend on this; see [ReminderWorker].
 */
@AndroidEntryPoint
class PushMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notifier: Notifier
    @Inject lateinit var syncScheduler: SyncScheduler

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        notifier.show(
            key = message.messageId ?: title,
            title = title,
            body = body,
            channel = Notifier.CHANNEL_GENERAL,
        )
    }

    /** A sync run stores the new token under the user's devices. */
    override fun onNewToken(token: String) {
        // Debug builds print the token so a test push can be sent to this device from the Firebase console.
        if (BuildConfig.DEBUG) Log.d("PushToken", token)
        syncScheduler.requestSync()
    }
}
