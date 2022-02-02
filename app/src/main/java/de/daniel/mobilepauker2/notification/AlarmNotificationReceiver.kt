package de.daniel.mobilepauker2.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.lessonimport.LessonImport
import de.daniel.mobilepauker2.mainmenu.MainMenu
import de.daniel.mobilepauker2.settings.SettingsManager
import de.daniel.mobilepauker2.utils.Constants.NOTIFICATION_CHANNEL_ID
import de.daniel.mobilepauker2.utils.Constants.NOTIFICATION_ID
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Utility
import javax.inject.Inject

class AlarmNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("AlamNotificationReceiver::onReceive", "Alarm received")

        (context.applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        val intents = arrayOf(
            Intent(context, MainMenu::class.java),
            Intent(context, LessonImport::class.java)
        )
        val pendingIntent = PendingIntent.getActivities(
            context,
            0,
            intents,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )
        builder.setAutoCancel(true)
            .setSmallIcon(R.drawable.notify_icon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentIntent(pendingIntent)
            .setContentText(context.getString(R.string.cards_expire_notify_msg))
        Log.d("AlamNotificationReceiver::onReceive", "Notification build")

        val isAppRunning = Utility.isAppRunning(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Log.d("AlamNotificationReceiver::onReceive", "PaukerActive: $isAppRunning")
        val showNotify: Boolean = settingsManager.getBoolPreference(SettingsManager.Keys.SHOW_CARD_NOTIFY)
        if (!isAppRunning && showNotify) {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
            Log.d("AlamNotificationReceiver::onReceive", "Notification send")
        }
    }
}