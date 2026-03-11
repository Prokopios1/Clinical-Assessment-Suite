package com.clinical.assessment

import android.app.Application
import android.content.Context
import android.util.Log

class ClinicalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("ClinicalApplication", "Uncaught exception", exception)
            val stackTrace = Log.getStackTraceString(exception)
            val prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("last_crash", stackTrace).commit()
            
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}
