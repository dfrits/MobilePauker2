package com.daniel.mobilepauker2.model;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.ankushgrover.hourglass.Hourglass;
import com.daniel.mobilepauker2.utils.Log;

import java.security.InvalidParameterException;

public class TimerService extends Service {
    public static final String USTM_TOTAL_TIME = "USTM_TOTAL_TIME";
    public static final String STM_TOTAL_TIME = "STM_TOTAL_TIME";
    private final IBinder binder = new LocalBinder();
    private Callback callback;
    private boolean ustmTimerPaused = false;
    private boolean ustmTimerFinished = true;
    private boolean stmTimerPaused = false;
    private boolean stmTimerFinished = true;
    private int ustmTotalTime;
    private int stmTotalTime;
    private Hourglass ustmTimer;
    private Hourglass stmTimer;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ustmTotalTime = intent.getIntExtra(USTM_TOTAL_TIME, -1);
        stmTotalTime = intent.getIntExtra(STM_TOTAL_TIME, -1);
        if (ustmTotalTime == -1 || stmTotalTime == -1) {
            Log.d("TimerService::onStartCommand", "Invalid total time: USTM= "
                    + ustmTotalTime + "; STM= " + stmTotalTime);
            stopSelf();
        }
        initTimer();
        return START_NOT_STICKY;
    }

    public void registerClient(Activity activity) {
        callback = (Callback) activity;
    }

    private void initTimer() {
        if (ustmTimer == null) {
            ustmTimer = new Hourglass(ustmTotalTime * 1000) {
                @Override
                public void onTimerTick(long timeRemaining) {
                    if (timeRemaining > 0) {
                        if (!ustmTimerFinished) {
                            int totalSec = (int) (timeRemaining / 1000);
                            int timeElapsed = ustmTotalTime - totalSec;

                            callback.onUstmTimerUpdate(timeElapsed);
                        }
                    } else {
                        stopUstmTimer();
                        callback.onUstmTimerFinish();
                    }
                }

                @Override
                public void onTimerFinish() {
                    Log.d("TimerService::USTM-Timer finished", "Timer finished");
                }
            };
        }
        if (stmTimer == null) {
            stmTimer = new Hourglass(stmTotalTime * 60 * 1000) {
                @Override
                public void onTimerTick(long timeRemaining) {
                    if (timeRemaining > 0 && !stmTimerFinished) {
                        int totalSec = (int) (timeRemaining / 1000);
                        int timeElapsed = stmTotalTime * 60 - totalSec;

                        callback.onStmTimerUpdate(timeElapsed);
                    } else {
                        stopStmTimer();
                        callback.onStmTimerFinish();
                    }
                }

                @Override
                public void onTimerFinish() {
                    Log.d("TimerService::STM-Timer finished", "Timer finished");
                    callback.onStmTimerFinish();
                }
            };
        }
    }

    public void startUstmTimer() {
        if (ustmTimer != null && ustmTimerFinished) {
            ustmTimer.startTimer();
            ustmTimerFinished = false;
        }
    }

    public void startStmTimer() {
        if (stmTimer != null && stmTimerFinished) {
            stmTimer.startTimer();
            stmTimerFinished = false;
        }
    }

    public void pauseTimers() {
        if (ustmTimer != null && !ustmTimer.isPaused() && !ustmTimerFinished) {
            ustmTimer.pauseTimer();
        }
        if (stmTimer != null && !stmTimer.isPaused() && !stmTimerFinished) {
            stmTimer.pauseTimer();
        }
    }

    public void restartTimers() {
        if (ustmTimer != null && ustmTimer.isPaused() && !ustmTimerFinished) {
            ustmTimer.resumeTimer();
        }
        if (stmTimer != null && stmTimer.isPaused() && !stmTimerFinished) {
            stmTimer.resumeTimer();
        }
    }

    public void stopUstmTimer() {
        if (ustmTimer != null && !ustmTimerFinished) {
            ustmTimer.stopTimer();
            ustmTimerFinished = true;
        }
    }

    public void stopStmTimer() {
        if (stmTimer != null && !stmTimerFinished) {
            stmTimer.stopTimer();
            stmTimerFinished = true;
        }
    }

    public boolean isUstmTimerPaused() {
        return ustmTimer != null && ustmTimer.isPaused();
    }

    public boolean isStmTimerPaused() {
        return stmTimer != null && stmTimer.isPaused();
    }

    public boolean isUstmTimerFinished() {
        return ustmTimerFinished;
    }

    public boolean isStmTimerFinished() {
        return stmTimerFinished;
    }

    public int getUstmTotalTime() {
        return ustmTotalTime;
    }

    public int getStmTotalTime() {
        return stmTotalTime;
    }

    public class LocalBinder extends Binder {
        public TimerService getServiceInstance() {
            return TimerService.this;
        }
    }

    public interface Callback {
        void onUstmTimerUpdate(int elapsedTime);

        void onStmTimerUpdate(int elapsedTime);

        void onUstmTimerFinish();

        void onStmTimerFinish();
    }
}
