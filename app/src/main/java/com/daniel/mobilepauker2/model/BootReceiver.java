package com.daniel.mobilepauker2.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.daniel.mobilepauker2.model.notification.NotificationService;
import com.daniel.mobilepauker2.utils.Log;

public class BootReceiver extends BroadcastReceiver { // TODO

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BootReceiver::onReceive", "Entered");
        String action = intent.getAction();

        if (action == null || !action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d("BootReceiver::onReceive", "Wrong Action = " + action);
            return;
        }

        Log.d("BootReceiver::onReceive", "Correct Action");

        // Notification neu setzen
        NotificationService.enqueueWork(context);
        Log.d("BootReceiver::onReceive", "NotificationService started");

        // Autosync
        // TODO implementieren
    }
}
