/*
 * A Wrapper class of android.util.Log
 * Original author Prasanta Paul
 */

package com.daniel.mobilepauker2.utils;

public class Log {

    public static final boolean enableLog = true;

    public static final int VERBOSE = 0xA1;
    public static final int DEBUG = 0xA2;
    public static final int INFO = 0xA3;
    public static final int WARN = 0xA4;
    public static final int ERROR = 0xA5;

    public static final int logLevel = VERBOSE;

    public static void i(String tag, String msg) {
        if (!enableLog || logLevel > INFO) {
            return;
        }
        android.util.Log.i(tag, msg);
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (!enableLog || logLevel > INFO) {
            return;
        }
        android.util.Log.i(tag, msg, tr);
    }

    public static void v(String tag, String msg) {
        if (!enableLog || logLevel > VERBOSE) {
            return;
        }
        android.util.Log.v(tag, msg);
    }

    public static void v(String tag, String msg, Throwable tr) {
        if (!enableLog || logLevel > VERBOSE) {
            return;
        }
        android.util.Log.v(tag, msg, tr);
    }

    public static void d(String tag, String msg) {
        if (!enableLog || logLevel > DEBUG) {
            return;
        }
        android.util.Log.d(tag, msg);
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (!enableLog || logLevel > DEBUG) {
            return;
        }
        android.util.Log.d(tag, msg, tr);
    }

    public static void w(String tag, String msg) {
        if (!enableLog || logLevel > WARN) {
            return;
        }
        android.util.Log.w(tag, msg);
    }

    public static void w(String tag, String msg, Throwable tr) {
        if (!enableLog || logLevel > WARN) {
            return;
        }
        android.util.Log.w(tag, msg, tr);
    }

    public static void e(String tag, String msg) {
        if (!enableLog || logLevel > ERROR) {
            return;
        }
        android.util.Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (!enableLog || logLevel > ERROR) {
            return;
        }
        android.util.Log.e(tag, msg, tr);
    }
}

