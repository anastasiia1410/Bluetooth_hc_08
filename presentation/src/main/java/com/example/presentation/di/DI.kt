package com.example.presentation.di

import com.example.presentation.screens.connected_device.ConnectedViewModel
import com.example.presentation.screens.device.DeviceViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { DeviceViewModel(get()) }
    viewModel { ConnectedViewModel(get(), get(), get()) }
}