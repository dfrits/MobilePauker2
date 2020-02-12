package com.daniel.mobilepauker2.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.daniel.mobilepauker2.core.notification.NotificationService
import com.daniel.mobilepauker2.pauker_native.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver::onReceive", "Entered")
        val action = intent.action
        if (action == null || action != Intent.ACTION_BOOT_COMPLETED) {
            Log.d(
                "BootReceiver::onReceive",
                "Wrong Action = $action"
            )
            return
        }
        Log.d("BootReceiver::onReceive", "Correct Action")
        // Notification neu setzen
        NotificationService.Companion.enqueueWork(context)
        Log.d(
            "BootReceiver::onReceive",
            "NotificationService started"
        )
        // Autosync
// TODO implementieren
    }
}