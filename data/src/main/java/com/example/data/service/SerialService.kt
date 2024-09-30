package com.example.data.service

import android.bluetooth.BluetoothDevice
import android.location.Address
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface SerialService {

    val isConnected: StateFlow<Boolean>
    val isSend: MutableStateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDevice>>
    val dataFlow: StateFlow<ByteArray>
    val datasFlow: MutableSharedFlow<ArrayDeque<ByteArray>>



    suspend fun scanLeDevice()
    fun stopScanLeDevice()

    suspend fun connect(device: BluetoothDevice)
    fun doAfterConnection()

    fun getDevice(macAddress: String): BluetoothDevice

    suspend fun write(address: Byte, data: Byte)
    suspend fun read(data: ByteArray)
    //fun dataFlow(): Flow<ByteArray>


    fun onSerialConnect()

    fun onSerialConnectError(e: Exception)

    fun onSerialRead(data: ByteArray)

    fun onSerialRead(datas: ArrayDeque<ByteArray>)

    fun onSerialIoError(e: Exception)

    fun detach()
    suspend fun disconnect()
}