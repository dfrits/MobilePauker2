package com.daniel.mobilepauker2.model.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.activities.LessonImportActivity;
import com.daniel.mobilepauker2.activities.MainMenu;
import com.daniel.mobilepauker2.model.SettingsManager;
import com.daniel.mobilepauker2.utils.Log;

import static android.app.PendingIntent.FLAG_ONE_SHOT;
import static com.daniel.mobilepauker2.utils.Constants.NOTIFICATION_CHANNEL_ID;
import static com.daniel.mobilepauker2.utils.Constants.NOTIFICATION_ID;

public class AlarmNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlamNotificationReceiver::onReceive", "Alarm received");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);

        Intent[] intents = new Intent[]{new Intent(context, MainMenu.class),
                new Intent(context, LessonImportActivity.class)};
        PendingIntent pendingIntent = PendingIntent.getActivities(
                context,
                0,
                intents,
                FLAG_ONE_SHOT);

        builder.setAutoCancel(true)
                .setSmallIcon(R.drawable.notify_icon)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentIntent(pendingIntent)
                .setContentText(context.getString(R.string.cards_expire_notify_msg));

        Log.d("AlamNotificationReceiver::onReceive", "Notification build");

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Log.d("AlamNotificationReceiver::onReceive", "PaukerActive: " + MainMenu.isPaukerActive);
        boolean showNotify = SettingsManager.instance().getBoolPreference(context, SettingsManager.Keys.SHOW_CARD_NOTIFY);
        if (notificationManager != null && !MainMenu.isPaukerActive && showNotify) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d("AlamNotificationReceiver::onReceive", "Notification send");
        }
    }
}
