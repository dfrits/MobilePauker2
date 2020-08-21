package com.daniel.mobilepauker2.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.core.model.xmlsupport.FlashCardXMLPullFeedParser
import com.daniel.mobilepauker2.pauker_native.Log
import java.io.IOException
import java.net.URI

class NotificationService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        setAlarm()
    }

    private fun setAlarm() {
        Log.d("NotificationService::setAlarm", "Entered")
        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent: Intent
        val pendingIntent: PendingIntent
        val newestTime = newestExpireTime
        Log.d(
            "NotificationService::setAlarm",
            "Newest Time: " + newestTime + ". Now is: " + System.currentTimeMillis()
        )
        if (newestTime > -1) {
            alarmIntent = Intent(this, AlarmNotificationReceiver::class.java)
            pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0)
            alarmManager[AlarmManager.RTC_WAKEUP, newestTime] = pendingIntent
            Log.d("NotificationService::setAlarm", "Alarm set")
        }
    }

    private val newestExpireTime: Long
        get() {
            val paukerManager: PaukerManager = PaukerManager.instance()
            val files = try {
                paukerManager.listFiles(this)
            } catch (e: SecurityException) {
                null
            }

            var newestTime: Long = -1
            if (files != null) {
                var uri: URI
                var parser: FlashCardXMLPullFeedParser
                for (file in files) {
                    if (!(paukerManager.isSaveRequired && paukerManager.currentFileName == file.name)) {
                        try {
                            uri = paukerManager.getFilePath(this, file.name).toURI()
                            parser = FlashCardXMLPullFeedParser(uri.toURL())
                            val map = parser.nextExpireDate
                            if (map[0] > Long.MIN_VALUE) {
                                if (map[1, 0] > 0) {
                                    return 0
                                } else {
                                    if (newestTime == -1L || map[0] < newestTime) {
                                        newestTime = map[0]
                                    }
                                }
                            }
                        } catch (ignored: IOException) {
                            Log.d(
                                "NotificationService::setAlarm",
                                "Cannot read File"
                            )
                        }
                    }
                }
            }
            return newestTime
        }

    companion object {
        private const val JOB_ID = 0
        fun enqueueWork(context: Context?) {
            enqueueWork(
                context!!,
                NotificationService::class.java,
                JOB_ID,
                Intent()
            )
        }
    }
}