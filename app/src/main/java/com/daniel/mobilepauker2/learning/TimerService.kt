package com.daniel.mobilepauker2.learning

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.daniel.mobilepauker2.pauker_native.Log
import java.util.*

class TimerService : Service() {
    private val binder: IBinder = LocalBinder()
    private var callback: Callback? = null
    var ustmTotalTime = 0
        private set
    private var ustm_timeRemaining: Long = 0
    private var ustm_timeout = Date(0)
    private var ustm_timer: Timer? = null
    private var ustm_timerPaused = false
    var isUstmTimerFinished = true
        private set
    var stmTotalTime = 0
        private set
    private var stm_timeRemaining: Long = 0
    private var stm_timeout = Date(0)
    private var stm_timer: Timer? = null
    private var stm_timerPaused = false
    var isStmTimerFinished = true
        private set

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        ustmTotalTime = intent.getIntExtra(USTM_TOTAL_TIME, -1)
        stmTotalTime = intent.getIntExtra(STM_TOTAL_TIME, -1)
        if (ustmTotalTime == -1 || stmTotalTime == -1) {
            Log.d(
                "TimerService::onStartCommand", "Invalid total time: USTM= "
                        + ustmTotalTime + "; STM= " + stmTotalTime
            )
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

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        stopSelf()
        return false
    }

    fun registerClient(activity: Activity?) {
        callback = activity as Callback?
    }

    private fun onUstmTimerTick() {
        val timeRemaining = ustm_timeout.time - Date().time
        val totalSec = (timeRemaining / 1000).toInt()
        if (totalSec > 0) {
            if (!isUstmTimerFinished) {
                ustm_timeRemaining = timeRemaining
                val timeElapsed = ustmTotalTime - totalSec
                callback!!.onUstmTimerUpdate(timeElapsed)
            }
        } else {
            Log.d(
                "TimerService::USTM-Timer finished",
                "Timer finished"
            )
            stopUstmTimer()
        }
    }

    private fun onStmTimerTick() {
        val timeRemaining = stm_timeout.time - Date().time
        val totalSec = (timeRemaining / 1000).toInt()
        if (totalSec > 0) {
            if (!isStmTimerFinished) {
                stm_timeRemaining = timeRemaining
                val timeElapsed = stmTotalTime * 60 - totalSec
                callback!!.onStmTimerUpdate(timeElapsed)
            }
        } else {
            Log.d(
                "TimerService::STM-Timer finished",
                "Timer finished"
            )
            stopStmTimer()
        }
    }

    fun startUstmTimer() {
        if (ustm_timer != null && isUstmTimerFinished) {
            val currentTime = Date().time
            val time = currentTime + ustmTotalTime * 1000
            ustm_timeout = Date(time)
            scheduleUstmTimer()
            isUstmTimerFinished = false
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
        if (stm_timer != null && isStmTimerFinished) {
            val currentTime = Date().time
            val time = currentTime + stmTotalTime * 60 * 1000
            stm_timeout = Date(time)
            scheduleStmTimer()
            isStmTimerFinished = false
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
        if (ustm_timer != null && !ustm_timerPaused && !isUstmTimerFinished) {
            ustm_timer!!.cancel()
            ustm_timerPaused = true
        }
        if (stm_timer != null && !stm_timerPaused && !isStmTimerFinished) {
            stm_timer!!.cancel()
            stm_timerPaused = true
        }
    }

    fun restartTimers() {
        val currentTime = Date().time
        var time: Long
        if (ustm_timer != null && ustm_timerPaused && !isUstmTimerFinished) {
            time = currentTime + ustm_timeRemaining
            ustm_timeout = Date(time)
            scheduleUstmTimer()
            ustm_timerPaused = false
        }
        if (stm_timer != null && stm_timerPaused && !isStmTimerFinished) {
            time = currentTime + stm_timeRemaining
            stm_timeout = Date(time)
            scheduleStmTimer()
            stm_timerPaused = false
        }
    }

    fun stopUstmTimer() {
        if (ustm_timer != null && !isUstmTimerFinished) {
            ustm_timer!!.cancel()
            ustm_timer!!.purge()
            isUstmTimerFinished = true
            callback!!.onUstmTimerFinish()
        }
    }

    fun stopStmTimer() {
        if (stm_timer != null && !isStmTimerFinished) {
            stm_timer!!.cancel()
            stm_timer!!.purge()
            isStmTimerFinished = true
            callback!!.onStmTimerFinish()
        }
    }

    val isUstmTimerPaused: Boolean
        get() = ustm_timer != null && ustm_timerPaused

    val isStmTimerPaused: Boolean
        get() = stm_timer != null && stm_timerPaused

    interface Callback {
        fun onUstmTimerUpdate(elapsedTime: Int)
        fun onStmTimerUpdate(elapsedTime: Int)
        fun onUstmTimerFinish()
        fun onStmTimerFinish()
    }

    inner class LocalBinder : Binder() {
        val serviceInstance: TimerService
            get() = this@TimerService
    }

    companion object {
        const val USTM_TOTAL_TIME = "USTM_TOTAL_TIME"
        const val STM_TOTAL_TIME = "STM_TOTAL_TIME"
    }
}