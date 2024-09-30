package com.example.domain.di

import com.example.domain.repository.connect.ConnectRepository
import com.example.domain.repository.connect.ConnectRepositoryImpl
import com.example.domain.repository.gatt.GattRepository
import com.example.domain.repository.gatt.GattRepositoryImpl
import com.example.domain.repository.read.ReadRepository
import com.example.domain.repository.read.ReadRepositoryImpl
import com.example.domain.repository.scan.ScanRepository
import com.example.domain.repository.scan.ScanRepositoryImpl
import com.example.domain.repository.write.WriteRepository
import com.example.domain.repository.write.WriteRepositoryImpl
import org.koin.dsl.module

val domainModule = module {
    factory<ScanRepository> { ScanRepositoryImpl(get()) }
    factory<ConnectRepository> { ConnectRepositoryImpl(get()) }
    factory<WriteRepository> { WriteRepositoryImpl(get()) }
    factory<ReadRepository> { ReadRepositoryImpl(get()) }
    factory<GattRepository>{GattRepositoryImpl(get())}
}
