package com.example.aether.util

import android.util.Log
import com.example.aether.BuildConfig

/** Debug logs only in debug builds; errors always reach logcat. */
object AetherLog {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
