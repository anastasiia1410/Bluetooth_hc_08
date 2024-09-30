package com.example.bluetoothhc_08.core

import android.app.Application
import com.example.bluetoothhc_08.core.di.initKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}