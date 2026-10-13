package com.example.llmchat.util

import android.util.Log
import com.example.llmchat.BuildConfig

/** Centralized debug logging so the Logcat tag lives in one place. */
object AppLog {
    const val TAG = "LLMChat"

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }
}
