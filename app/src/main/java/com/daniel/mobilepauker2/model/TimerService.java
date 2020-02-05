package com.daniel.mobilepauker2.model;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.daniel.mobilepauker2.utils.Log;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class TimerService extends Service {
    public static final String USTM_TOTAL_TIME = "USTM_TOTAL_TIME";
    public static final String STM_TOTAL_TIME = "STM_TOTAL_TIME";
    private final IBinder binder = new LocalBinder();
    private Callback callback;

    private int ustm_totalTime;
    private long ustm_timeRemaining = 0;
    private Date ustm_timeout = new Date(0);
    private Timer ustm_timer;
    private boolean ustm_timerPaused = false;
    private boolean ustm_timerFinished = true;

    private int stm_totalTime;
    private long stm_timeRemaining = 0;
    private Date stm_timeout = new Date(0);
    private Timer stm_timer;
    private boolean stm_timerPaused = false;
    private boolean stm_timerFinished = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ustm_totalTime = intent.getIntExtra(USTM_TOTAL_TIME, -1);
        stm_totalTime = intent.getIntExtra(STM_TOTAL_TIME, -1);

        if (ustm_totalTime == -1 || stm_totalTime == -1) {
            Log.d("TimerService::onStartCommand", "Invalid total time: USTM= "
                    + ustm_totalTime + "; STM= " + stm_totalTime);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ustm_timer == null) {
            ustm_timer = new Timer();
        }

        if (stm_timer == null) {
            stm_timer = new Timer();
        }

        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopSelf();
        return false;
    }

    public void registerClient(Activity activity) {
        callback = (Callback) activity;
    }

    private void onUstmTimerTick() {
        long timeRemaining = ustm_timeout.getTime() - new Date().getTime();
        int totalSec = (int) (timeRemaining / 1000);
        if (totalSec > 0) {
            if (!ustm_timerFinished) {
                ustm_timeRemaining = timeRemaining;
                int timeElapsed = ustm_totalTime - totalSec;

                callback.onUstmTimerUpdate(timeElapsed);
            }
        } else {
            Log.d("TimerService::USTM-Timer finished", "Timer finished");
            stopUstmTimer();
        }
    }

    private void onStmTimerTick() {
        long timeRemaining = stm_timeout.getTime() - new Date().getTime();
        int totalSec = (int) (timeRemaining / 1000);
        if (totalSec > 0) {
            if (!stm_timerFinished) {
                stm_timeRemaining = timeRemaining;
                int timeElapsed = stm_totalTime * 60 - totalSec;

                callback.onStmTimerUpdate(timeElapsed);
            }
        } else {
            Log.d("TimerService::STM-Timer finished", "Timer finished");
            stopStmTimer();
        }
    }

    public void startUstmTimer() {
        if (ustm_timer != null && ustm_timerFinished) {
            long currentTime = new Date().getTime();
            long time = currentTime + ustm_totalTime * 1000;
            ustm_timeout = new Date(time);
            scheduleUstmTimer();
            ustm_timerFinished = false;
            ustm_timerPaused = false;
        }
    }

    private void scheduleUstmTimer() {
        ustm_timer = new Timer();
        ustm_timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                onUstmTimerTick();
            }
        }, 0, 1000);
    }

    public void startStmTimer() {
        if (stm_timer != null && stm_timerFinished) {
            long currentTime = new Date().getTime();
            long time = currentTime + stm_totalTime * 60 * 1000;
            stm_timeout = new Date(time);
            scheduleStmTimer();
            stm_timerFinished = false;
            stm_timerPaused = false;
        }
    }

    private void scheduleStmTimer() {
        stm_timer = new Timer();
        stm_timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                onStmTimerTick();
            }
        }, 0, 1000);
    }

    public void pauseTimers() {
        if (ustm_timer != null && !ustm_timerPaused && !ustm_timerFinished) {
            ustm_timer.cancel();
            ustm_timerPaused = true;
        }
        if (stm_timer != null && !stm_timerPaused && !stm_timerFinished) {
            stm_timer.cancel();
            stm_timerPaused = true;
        }
    }

    public void restartTimers() {
        long currentTime = new Date().getTime();
        long time;
        if (ustm_timer != null && ustm_timerPaused && !ustm_timerFinished) {
            time = currentTime + ustm_timeRemaining;
            ustm_timeout = new Date(time);
            scheduleUstmTimer();
            ustm_timerPaused = false;
        }
        if (stm_timer != null && stm_timerPaused && !stm_timerFinished) {
            time = currentTime + stm_timeRemaining;
            stm_timeout = new Date(time);
            scheduleStmTimer();
            stm_timerPaused = false;
        }
    }

    public void stopUstmTimer() {
        if (ustm_timer != null && !ustm_timerFinished) {
            ustm_timer.cancel();
            ustm_timer.purge();
            ustm_timerFinished = true;
            callback.onUstmTimerFinish();
        }
    }

    public void stopStmTimer() {
        if (stm_timer != null && !stm_timerFinished) {
            stm_timer.cancel();
            stm_timer.purge();
            stm_timerFinished = true;
            callback.onStmTimerFinish();
        }
    }

    public boolean isUstmTimerPaused() {
        return ustm_timer != null && ustm_timerPaused;
    }

    public boolean isStmTimerPaused() {
        return stm_timer != null && stm_timerPaused;
    }

    public boolean isUstmTimerFinished() {
        return ustm_timerFinished;
    }

    public boolean isStmTimerFinished() {
        return stm_timerFinished;
    }

    public int getUstmTotalTime() {
        return ustm_totalTime;
    }

    public int getStmTotalTime() {
        return stm_totalTime;
    }

    public interface Callback {
        void onUstmTimerUpdate(int elapsedTime);

        void onStmTimerUpdate(int elapsedTime);

        void onUstmTimerFinish();

        void onStmTimerFinish();
    }

    public class LocalBinder extends Binder {
        public TimerService getServiceInstance() {
            return TimerService.this;
        }
    }
}
