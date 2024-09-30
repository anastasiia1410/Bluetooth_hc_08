package com.example.domain.repository.scan

import android.bluetooth.BluetoothDevice
import com.example.data.service.SerialService
import kotlinx.coroutines.flow.StateFlow

class ScanRepositoryImpl(private val serialService: SerialService) : ScanRepository {

    override val scannedDevices: StateFlow<List<BluetoothDevice>>
        get() = serialService.scannedDevices

    override suspend fun scanLeDevice() {
        serialService.scanLeDevice()
    }

    override fun stopScanLeDevice() {
        serialService.stopScanLeDevice()
    }

}