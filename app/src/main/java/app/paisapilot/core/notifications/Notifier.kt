package app.paisapilot.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.paisapilot.MainActivity
import app.paisapilot.R
import app.paisapilot.navigation.PendingNavigation
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Creates the notification channels and posts notifications. */
@Singleton
class Notifier @Inject constructor(@ApplicationContext private val context: Context) {

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "EMI & bill reminders", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Alerts before an EMI, bill or card payment is due" }
        )
        // Its own category, so nudges can be silenced in system settings without losing due-date alerts.
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_NUDGES, "Tracking nudges & quotes", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "A gentle reminder to log your spending, or a money quote" }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_GENERAL, "General", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Tips, safety alerts and account updates" }
        )
    }

    fun canNotify(): Boolean {
        val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** A stable [key] means a repeat reminder replaces the earlier one instead of stacking. */
    fun show(key: String, title: String, body: String, channel: String = CHANNEL_REMINDERS, destination: String? = null) {
        if (!canNotify()) return
        // A distinct request code per notification, or Android would reuse one intent (and one destination) for all.
        val open = PendingIntent.getActivity(
            context, key.hashCode(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(PendingNavigation.EXTRA_ROUTE, destination),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(ContextCompat.getColor(context, R.color.brand_green))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(if (channel == CHANNEL_REMINDERS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        @Suppress("MissingPermission") // checked by canNotify()
        NotificationManagerCompat.from(context).notify(key.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_GENERAL = "general"
        const val CHANNEL_NUDGES = "nudges"
    }
}
