package com.liam.kaptalismusaufhalter.security

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.liam.kaptalismusaufhalter.MainActivity
import com.liam.kaptalismusaufhalter.R
import com.liam.kaptalismusaufhalter.work.NotificationHelper

/** Posts the actual warning notification for a [SecurityEvent]. INFO-level events stay in the log only. */
object SecurityNotifier {

    fun notify(context: Context, event: SecurityEvent) {
        if (event.severity == Severity.INFO) return

        val app = context.applicationContext
        NotificationHelper.ensureChannel(app)
        val manager = NotificationManagerCompat.from(app)
        if (!manager.areNotificationsEnabled()) return

        val critical = event.severity == Severity.CRITICAL
        val notificationId = (event.id % Int.MAX_VALUE).toInt()

        val openApp = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationHelper.EXTRA_OPEN_SECURITY, true)
            putExtra(IntentGuard.EXTRA_NAV_TOKEN, IntentGuard.tokenFor(app))
        }
        val openAppPending = PendingIntent.getActivity(
            app, notificationId, openApp, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(
            app,
            if (critical) NotificationHelper.SECURITY_CRITICAL_CHANNEL_ID else NotificationHelper.SECURITY_INFO_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(event.title)
            .setContentText(event.details)
            .setStyle(NotificationCompat.BigTextStyle().bigText(event.details))
            .setAutoCancel(true)
            .setContentIntent(openAppPending)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(if (critical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

        event.settingsAction?.let { action ->
            val settings = Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            val settingsPending = PendingIntent.getActivity(
                app, notificationId + 1, settings, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Einstellungen öffnen", settingsPending)
        }

        try {
            manager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS revoked between the check and the call - the event is still in the log.
        }
    }
}
