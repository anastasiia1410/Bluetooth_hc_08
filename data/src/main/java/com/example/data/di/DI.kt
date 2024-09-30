package com.example.data.di

import com.example.data.service.GattCallback
import com.example.data.service.SerialService
import com.example.data.service.SerialServiceImpl
import org.koin.dsl.module

val dataModule = module {
    single{GattCallback(get())}
    single<SerialService> { SerialServiceImpl(get(), get()) }
}