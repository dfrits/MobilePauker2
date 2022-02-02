package de.daniel.mobilepauker2.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.daniel.mobilepauker2.utils.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {
        Log.d("BootReceiver::onReceive", "Entered")
        val action = intent.action
        if (action == null || action != Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver::onReceive", "Wrong Action = $action")
            return
        }
        Log.d("BootReceiver::onReceive", "Correct Action")

        // Notification neu setzen
        NotificationService.enqueueWork(context!!)
        Log.d("BootReceiver::onReceive", "NotificationService started")
    }
}