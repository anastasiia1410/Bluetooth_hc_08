package com.example.domain.repository.connect

import android.bluetooth.BluetoothDevice
import com.example.data.service.SerialService
import kotlinx.coroutines.flow.StateFlow

class ConnectRepositoryImpl(private val serialService: SerialService) : ConnectRepository {
    override val isConnected: StateFlow<Boolean>
        get() = serialService.isConnected

    override suspend fun connect(device: BluetoothDevice) {
        serialService.connect(device)
    }

    override fun onSerialConnectError(e: Exception) {
        serialService.onSerialConnectError(e)
    }

    override fun doAfterConnection() {
        serialService.doAfterConnection()
    }

    override fun getDevice(macAddress: String): BluetoothDevice {
        return serialService.getDevice(macAddress)
    }

    override fun onSerialRead(datas: ArrayDeque<ByteArray>) {
        serialService.onSerialRead(datas)
    }


}