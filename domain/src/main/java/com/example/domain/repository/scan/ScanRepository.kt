package com.example.domain.repository.scan

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.StateFlow

interface ScanRepository {

    val scannedDevices : StateFlow<List<BluetoothDevice>>

    suspend fun scanLeDevice()
    fun stopScanLeDevice()
}