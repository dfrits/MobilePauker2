package com.daniel.mobilepauker2.model.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import android.util.SparseLongArray;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.model.xmlsupport.FlashCardXMLPullFeedParser;
import com.daniel.mobilepauker2.utils.Log;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class NotificationService extends JobIntentService {
    private static final int JOB_ID = 0;

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        setAlarm();
    }

    private void setAlarm() {
        Log.d("NotificationService::setAlarm", "Entered");
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent;
        PendingIntent pendingIntent;

        long newestTime = getNewestExpireTime();
        Log.d("NotificationService::setAlarm", "Newest Time: " + newestTime + ". Now is: " + System.currentTimeMillis());

        if (newestTime > -1 && alarmManager != null) {
            alarmIntent = new Intent(this, AlarmNotificationReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP, newestTime, pendingIntent);
            Log.d("NotificationService::setAlarm", "Alarm set");
        }
    }

    private long getNewestExpireTime() {
        PaukerManager paukerManager = PaukerManager.instance();
        File[] files;
        try {
            files = paukerManager.listFiles(this);
        } catch (SecurityException e) {
            files = new File[0];
        }

        long newestTime = -1;
        URI uri;
        FlashCardXMLPullFeedParser parser;

        for (File file : files) {
            if (!(paukerManager.isSaveRequired() && paukerManager.getCurrentFileName().equals(file.getName()))) {
                try {
                    uri = paukerManager.getFilePath(this, file.getName()).toURI();
                    parser = new FlashCardXMLPullFeedParser(uri.toURL());
                    SparseLongArray map = parser.getNextExpireDate();
                    if (map.get(0) > Long.MIN_VALUE) {
                        if (map.get(1, 0) > 0) {
                            return 0;
                        } else {
                            if (newestTime == -1 || map.get(0) < newestTime) {
                                newestTime = map.get(0);
                            }
                        }
                    }
                } catch (IOException ignored) {
                    Log.d("NotificationService::setAlarm", "Cannot read File");
                }
            }
        }

        return newestTime;
    }

    public static void enqueueWork(Context context) {
        enqueueWork(context, NotificationService.class, JOB_ID, new Intent());
    }
}
