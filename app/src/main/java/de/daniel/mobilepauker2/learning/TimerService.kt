package de.daniel.mobilepauker2.learning

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import de.daniel.mobilepauker2.BuildConfig.APPLICATION_ID
import de.daniel.mobilepauker2.utils.Log
import java.util.*

class TimerService: Service() {
    private val binder: IBinder = LocalBinder()
    private var ustm_totalTime = 0
    private var ustm_timeRemaining: Long = 0
    private var ustm_timeout = Date(0)
    private var ustm_timer: Timer? = null
    private var ustm_timerPaused = false
    private var ustm_timerFinished = true

    private var stm_totalTime = 0
    private var stm_timeRemaining: Long = 0
    private var stm_timeout = Date(0)
    private var stm_timer: Timer? = null
    private var stm_timerPaused = false
    private var stm_timerFinished = true

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PeriSecure:MyWakeLock")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        wakeLock?.acquire(stm_totalTime + 6000L)

        ustm_totalTime = intent.getIntExtra(Companion.USTM_TOTAL_TIME, -1)
        stm_totalTime = intent.getIntExtra(Companion.STM_TOTAL_TIME, -1)
        if (ustm_totalTime == -1 || stm_totalTime == -1) {
            Log.d("TimerService::onStartCommand", "Invalid total time: USTM= "
                + ustm_totalTime + "; STM= " + stm_totalTime)
            stopSelf()
            return START_NOT_STICKY
        }
        if (ustm_timer == null) {
            ustm_timer = Timer()
        }
        if (stm_timer == null) {
            stm_timer = Timer()
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopUstmTimer()
        stopStmTimer()
        stopSelf()
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.release()
    }

    private fun onUstmTimerTick() {
        val timeRemaining = ustm_timeout.time - Date().time
        val totalSec = (timeRemaining / 1000).toInt()
        if (totalSec > 0) {
            if (!ustm_timerFinished) {
                ustm_timeRemaining = timeRemaining
                val timeElapsed = ustm_totalTime - totalSec
                onUstmTimerUpdate(timeElapsed)
            }
        } else {
            Log.d("TimerService::USTM-Timer finished", "Timer finished")
            stopUstmTimer()
        }
    }

    private fun onStmTimerTick() {
        val timeRemaining = stm_timeout.time - Date().time
        val totalSec = (timeRemaining / 1000).toInt()
        if (totalSec > 0) {
            if (!stm_timerFinished) {
                stm_timeRemaining = timeRemaining
                val timeElapsed = stm_totalTime * 60 - totalSec
                onStmTimerUpdate(timeElapsed)
            }
        } else {
            Log.d("TimerService::STM-Timer finished", "Timer finished")
            stopStmTimer()
        }
    }

    fun startUstmTimer() {
        if (ustm_timer != null && ustm_timerFinished) {
            val currentTime = Date().time
            val time = currentTime + ustm_totalTime * 1000
            ustm_timeout = Date(time)
            scheduleUstmTimer()
            ustm_timerFinished = false
            ustm_timerPaused = false
        }
    }

    private fun scheduleUstmTimer() {
        ustm_timer = Timer()
        ustm_timer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                onUstmTimerTick()
            }
        }, 0, 1000)
    }

    fun startStmTimer() {
        if (stm_timer != null && stm_timerFinished) {
            val currentTime = Date().time
            val time = currentTime + stm_totalTime * 60 * 1000
            stm_timeout = Date(time)
            scheduleStmTimer()
            stm_timerFinished = false
            stm_timerPaused = false
        }
    }

    private fun scheduleStmTimer() {
        stm_timer = Timer()
        stm_timer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                onStmTimerTick()
            }
        }, 0, 1000)
    }

    fun pauseTimers() {
        if (ustm_timer != null && !ustm_timerPaused && !ustm_timerFinished) {
            ustm_timer!!.cancel()
            ustm_timerPaused = true
        }
        if (stm_timer != null && !stm_timerPaused && !stm_timerFinished) {
            stm_timer!!.cancel()
            stm_timerPaused = true
        }
    }

    fun restartTimers() {
        val currentTime = Date().time
        var time: Long
        if (ustm_timer != null && ustm_timerPaused && !ustm_timerFinished) {
            time = currentTime + ustm_timeRemaining
            ustm_timeout = Date(time)
            scheduleUstmTimer()
            ustm_timerPaused = false
        }
        if (stm_timer != null && stm_timerPaused && !stm_timerFinished) {
            time = currentTime + stm_timeRemaining
            stm_timeout = Date(time)
            scheduleStmTimer()
            stm_timerPaused = false
        }
    }

    fun stopUstmTimer() {
        if (ustm_timer != null && !ustm_timerFinished) {
            ustm_timer!!.cancel()
            ustm_timer!!.purge()
            ustm_timerFinished = true
            onUstmTimerFinish()
        }
    }

    fun stopStmTimer() {
        if (stm_timer != null && !stm_timerFinished) {
            stm_timer!!.cancel()
            stm_timer!!.purge()
            stm_timerFinished = true
            onStmTimerFinish()
        }
    }

    private fun onUstmTimerUpdate(timeElapsed: Int) {
        val intent = Intent(Companion.ustm_receiver)
        intent.putExtra(Companion.ustm_time, timeElapsed)
        sendBroadcast(intent)
    }

    private fun onStmTimerUpdate(timeElapsed: Int) {
        val intent = Intent(Companion.stm_receiver)
        intent.putExtra(Companion.stm_time, timeElapsed)
        sendBroadcast(intent)
    }

    private fun onUstmTimerFinish() {
        val intent = Intent(Companion.ustm_finished_receiver)
        sendBroadcast(intent)
    }

    private fun onStmTimerFinish() {
        val intent = Intent(Companion.stm_finished_receiver)
        sendBroadcast(intent)
    }

    fun isUstmTimerPaused(): Boolean {
        return ustm_timer != null && ustm_timerPaused
    }

    fun isStmTimerPaused(): Boolean {
        return stm_timer != null && stm_timerPaused
    }

    fun isUstmTimerFinished(): Boolean {
        return ustm_timerFinished
    }

    fun isStmTimerFinished(): Boolean {
        return stm_timerFinished
    }

    fun getUstmTotalTime(): Int {
        return ustm_totalTime
    }

    fun getStmTotalTime(): Int {
        return stm_totalTime
    }

    inner class LocalBinder : Binder() {
        val serviceInstance: TimerService
            get() = this@TimerService
    }

    companion object {
        val USTM_TOTAL_TIME = "USTM_TOTAL_TIME"
        val STM_TOTAL_TIME = "STM_TOTAL_TIME"

        //Broadcast
        val ustm_receiver = "com.paukertimerservice.ustm_time_receiver"
        val stm_receiver = "com.paukertimerservice.stm_time_receiver"
        val ustm_finished_receiver = "com.paukertimerservice.ustm_finished_receiver"
        val stm_finished_receiver = "com.paukertimerservice.stm_finished_receiver"
        val ustm_time = "USTM_TIME"
        val stm_time = "STM_TIME"
    }
}