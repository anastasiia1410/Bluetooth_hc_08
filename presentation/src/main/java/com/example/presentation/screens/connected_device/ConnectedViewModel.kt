package com.example.presentation.screens.connected_device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.connect.ConnectRepository
import com.example.domain.repository.read.ReadRepository
import com.example.domain.repository.write.WriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ConnectedViewModel(
    private val connectRepository: ConnectRepository,
    private val writeRepository: WriteRepository,
    private val readRepository: ReadRepository,
) : ViewModel() {


    val isConnected = MutableStateFlow(false)
    val isSend = MutableStateFlow(false)
    val valueFlow = MutableStateFlow(0)
    val data = MutableStateFlow(byteArrayOf())
    var sendingEnabledFlow = MutableStateFlow(false)
    val isDataLoadedFlow = MutableStateFlow(false)
    val errorFlow = MutableStateFlow(false)


    fun connect(macAddress: String) =
        viewModelScope.launch {
            try {
                connectRepository.connect(connectRepository.getDevice(macAddress))
            } catch (e: Exception) {
                connectRepository.onSerialConnectError(e)
                errorFlow.emit(true)
            }
            connectRepository.isConnected.collect {
                isConnected.emit(it)
            }
        }

    fun doAfterConnection() = viewModelScope.launch(Dispatchers.IO) {
        connectRepository.doAfterConnection()
    }

    private fun send(address: Byte, value: Byte) = viewModelScope.launch(Dispatchers.IO) {
        writeRepository.write(address, value)
        writeRepository.isSend.collect {
            isSend.emit(it)
        }
    }

    fun sendNonStop(address: Byte, value: Byte, time: Long) =
        viewModelScope.launch(Dispatchers.IO) {
            while (sendingEnabledFlow.value) {
                delay(time)
                writeRepository.write(address, value)
            }
        }


    fun read() = viewModelScope.launch(Dispatchers.IO) {
        readRepository.dataFlow.collect {
            data.emit(it)
        }
    }

    fun add(currentValue: Int, address: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = currentValue.plus(1)
            valueFlow.emit(newValue)
            send(address.toByte(), newValue.toByte())
        }


    fun remove(currentValue: Int, address: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            val newValue = currentValue.minus(1)
            valueFlow.emit(newValue)
            send(address.toByte(), newValue.toByte())
        }
}