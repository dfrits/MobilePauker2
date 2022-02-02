package de.daniel.mobilepauker2.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.work.*
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.data.xml.FlashCardXMLPullFeedParser
import de.daniel.mobilepauker2.models.NextExpireDateResult
import de.daniel.mobilepauker2.utils.Log
import java.io.File
import java.io.IOException
import java.net.URI
import javax.inject.Inject

class NotificationService(val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    @Inject
    lateinit var dataManager: DataManager

    init {
        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)
    }

    override fun doWork(): Result {
        return setAlarm()
    }

    private fun setAlarm(): Result {
        Log.d("NotificationService::setAlarm", "Entered")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        val alarmIntent: Intent
        val pendingIntent: PendingIntent

        val newestTime: Long = getNewestExpireTime()
        Log.d(
            "NotificationService::setAlarm",
            "Newest Time: " + newestTime + ". Now is: " + System.currentTimeMillis()
        )

        if (newestTime > -1 && alarmManager != null) {
            alarmIntent = Intent(context, AlarmNotificationReceiver::class.java)
            pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0)
            alarmManager[AlarmManager.RTC_WAKEUP, newestTime] = pendingIntent
            Log.d("NotificationService::setAlarm", "Alarm set")
        }

        return Result.success()
    }

    private fun getNewestExpireTime(): Long {
        val files: Array<File> = dataManager.listFiles()
        var newestTime: Long = -1
        var uri: URI
        var parser: FlashCardXMLPullFeedParser
        for (file in files) {
            if (!(dataManager.saveRequired && dataManager.currentFileName == file.name)
            ) {
                try {
                    uri = dataManager.getFilePathForName(file.name).toURI()
                    parser = FlashCardXMLPullFeedParser(uri.toURL())
                    val map: NextExpireDateResult = parser.getNextExpireDate()
                    if (map.timeStamp > Long.MIN_VALUE) {
                        if (map.expiredCards > 0) {
                            return 0
                        } else {
                            if (newestTime == -1L || map.timeStamp < newestTime) {
                                newestTime = map.timeStamp
                            }
                        }
                    }
                } catch (ignored: IOException) {
                    Log.d("NotificationService::setAlarm", "Cannot read File")
                }
            }
        }
        return newestTime
    }

    companion object{
        fun enqueueWork(context: Context) {
            val notificationWorkRequest : WorkRequest = OneTimeWorkRequestBuilder<NotificationService>().build()
            WorkManager.getInstance(context).enqueue(notificationWorkRequest)
        }
    }
}