package com.daniel.mobilepauker2.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.lessonimport.LessonImportActivity
import com.daniel.mobilepauker2.main.MainMenu
import com.daniel.mobilepauker2.settings.SettingsManager
import com.daniel.mobilepauker2.settings.SettingsManager.Keys
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.pauker_native.Log

class AlarmNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(
            "AlamNotificationReceiver::onReceive",
            "Alarm received"
        )
        val builder =
            NotificationCompat.Builder(
                context,
                Constants.NOTIFICATION_CHANNEL_ID
            )
        val intents = arrayOf(
            Intent(context, MainMenu::class.java),
            Intent(context, LessonImportActivity::class.java)
        )
        val pendingIntent = PendingIntent.getActivities(
            context,
            0,
            intents,
            PendingIntent.FLAG_ONE_SHOT
        )
        builder.setAutoCancel(true)
            .setSmallIcon(R.drawable.notify_icon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentIntent(pendingIntent)
            .setContentText(context.getString(R.string.cards_expire_notify_msg))
        Log.d(
            "AlamNotificationReceiver::onReceive",
            "Notification build"
        )
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Log.d(
            "AlamNotificationReceiver::onReceive",
            "PaukerActive: " + MainMenu.isPaukerActive
        )
        val showNotify: Boolean =
            SettingsManager.instance().getBoolPreference(context, Keys.SHOW_CARD_NOTIFY)
        if (!MainMenu.isPaukerActive && showNotify) {
            notificationManager.notify(
                Constants.NOTIFICATION_ID,
                builder.build()
            )
            Log.d(
                "AlamNotificationReceiver::onReceive",
                "Notification send"
            )
        }
    }
}