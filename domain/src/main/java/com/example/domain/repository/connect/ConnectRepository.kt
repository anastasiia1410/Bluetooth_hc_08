package com.example.domain.repository.connect

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.StateFlow

interface ConnectRepository {

    val isConnected : StateFlow<Boolean>

    suspend fun connect(device: BluetoothDevice)

    fun onSerialConnectError(e: Exception)

    fun doAfterConnection()

    fun getDevice(macAddress: String): BluetoothDevice

    fun onSerialRead(datas: ArrayDeque<ByteArray>)

}