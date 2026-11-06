package com.example

import android.app.Application
import android.util.Log
import com.example.admin.AntiCheatProtectionManager
import com.example.data.local.LuneDatabase

class LuneApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure a safety handler for any unexpected unhandled exceptions
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("LuneApplication", "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Initialize AntiCheat protection safely
        try {
            AntiCheatProtectionManager.init(this)
        } catch (e: Throwable) {
            Log.e("LuneApplication", "Failed to init AntiCheatProtectionManager", e)
        }

        // Safely initialize & verify Room database on startup
        try {
            LuneDatabase.getDatabase(this)
        } catch (e: Throwable) {
            Log.e("LuneApplication", "Failed to pre-warm LuneDatabase", e)
        }
    }
}
