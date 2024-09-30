package com.example.presentation.screens.device

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.scan.ScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceViewModel(private val scanRepository: ScanRepository) : ViewModel() {

    val scanResult = MutableSharedFlow<List<BluetoothDevice>>()
    val isScanning = MutableStateFlow(false)


    fun scan() = viewModelScope.launch(Dispatchers.IO) {
        scanRepository.scanLeDevice()
        isScanning.emit(true)
        withContext(Dispatchers.Main) {
            scanRepository.scannedDevices.collect {
                scanResult.emit(it)
            }
        }
    }

    fun stopScan() = viewModelScope.launch(Dispatchers.IO) {
        isScanning.emit(false)
        scanRepository.stopScanLeDevice()
    }
}