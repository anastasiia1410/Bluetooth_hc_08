package com.example.domain.repository.gatt

import com.example.data.service.GattCallback
import kotlinx.coroutines.flow.MutableSharedFlow

class GattRepositoryImpl(private val gattCallback: GattCallback) : GattRepository {
//    override val dataFlow: MutableSharedFlow<ByteArray>
//        get() = gattCallback.dataFlow

//    override fun onSerialRead(data: ByteArray) {
//        gattCallback.onSerialRead(data)
//    }
}