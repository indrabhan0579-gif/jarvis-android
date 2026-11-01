package com.jarvis.assistant

import android.app.Application
import android.util.Log

class JarvisApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("JarvisApp", "J.A.R.V.I.S. Android Operating Core initialized.")
    }
}
