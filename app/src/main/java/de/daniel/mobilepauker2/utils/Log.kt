/*
 * A Wrapper class of android.util.Log
 * Original author Prasanta Paul
 */
package de.daniel.mobilepauker2.utils

import android.util.Log

object Log {
    const val enableLog = true
    const val VERBOSE = 0xA1
    const val DEBUG = 0xA2
    const val INFO = 0xA3
    const val WARN = 0xA4
    const val ERROR = 0xA5
    const val logLevel = VERBOSE
    fun i(tag: String?, msg: String?) {
        if (!enableLog || logLevel > INFO) {
            return
        }
        Log.i(tag, msg!!)
    }

    fun i(tag: String?, msg: String?, tr: Throwable?) {
        if (!enableLog || logLevel > INFO) {
            return
        }
        Log.i(tag, msg, tr)
    }

    fun v(tag: String?, msg: String?) {
        if (!enableLog || logLevel > VERBOSE) {
            return
        }
        Log.v(tag, msg!!)
    }

    fun v(tag: String?, msg: String?, tr: Throwable?) {
        if (!enableLog || logLevel > VERBOSE) {
            return
        }
        Log.v(tag, msg, tr)
    }

    fun d(tag: String?, msg: String?) {
        if (!enableLog || logLevel > DEBUG) {
            return
        }
        Log.d(tag, msg!!)
    }

    fun d(tag: String?, msg: String?, tr: Throwable?) {
        if (!enableLog || logLevel > DEBUG) {
            return
        }
        Log.d(tag, msg, tr)
    }

    fun w(tag: String?, msg: String?) {
        if (!enableLog || logLevel > WARN) {
            return
        }
        Log.w(tag, msg!!)
    }

    fun w(tag: String?, msg: String?, tr: Throwable?) {
        if (!enableLog || logLevel > WARN) {
            return
        }
        Log.w(tag, msg, tr)
    }

    fun e(tag: String?, msg: String?) {
        if (!enableLog || logLevel > ERROR) {
            return
        }
        Log.e(tag, msg!!)
    }

    fun e(tag: String?, msg: String?, tr: Throwable?) {
        if (!enableLog || logLevel > ERROR) {
            return
        }
        Log.e(tag, msg, tr)
    }
}