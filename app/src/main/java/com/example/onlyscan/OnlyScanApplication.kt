package com.example.onlyscan

import android.app.Application
import android.util.Log

class OnlyScanApplication : Application() {
    companion object {
        private const val TAG = "OnlyScanApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "App启动")
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "App终止")
    }
}