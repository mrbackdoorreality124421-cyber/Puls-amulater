package com.example

import android.app.Application
import android.util.Log
import com.example.engine.CrashLogger
import com.example.engine.SubsystemStatus

class PulsePcApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            // Initialize global crash isolation and recovery system
            CrashLogger.initialize(this)
            Log.i("PulsePcApp", "Stability and crash recovery engine initialized.")
            
            // Check if we are in Safe Mode due to previous crash loop
            if (CrashLogger.isSafeModeActive(this)) {
                Log.w("PulsePcApp", "⚠️ Safe Mode activated due to detected crash loop or manual flag.")
            }
        } catch (t: Throwable) {
            Log.e("PulsePcApp", "Failed during application initialization", t)
        }
    }
}
