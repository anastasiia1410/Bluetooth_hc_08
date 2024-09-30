package com.example.bluetoothhc_08.core.di

import com.example.bluetoothhc_08.core.App
import com.example.data.di.dataModule
import com.example.domain.di.domainModule
import com.example.presentation.di.presentationModule
import org.koin.core.context.startKoin
import org.koin.android.ext.koin.androidContext



fun App.initKoin() {
    startKoin {
        androidContext(this@initKoin)
        modules(presentationModule, domainModule, dataModule)
    }
}