package com.example.domain.repository.read

import com.example.data.service.SerialService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReadRepositoryImpl(private val serialService: SerialService) : ReadRepository {
    override val dataFlow: StateFlow<ByteArray>
        get() = serialService.dataFlow



//    override fun onSerialRead(data: ByteArray): Flow<ByteArray> {
//        return serialService.onSerialRead(data)
//    }


//    override val datasFlow: MutableSharedFlow<ArrayDeque<ByteArray>>
//        get() = serialService.datasFlow
}
//    override suspend fun read() {
//        serialService.read(dataFlow.value)
//    }
//    override fun onSerialRead(datas: ArrayDeque<ByteArray>) {
//        serialService.onSerialRead(datas)
//    }
//
//
//    override suspend fun onSerialRead(data: ByteArray) {
//        serialService.onSerialRead(data)
//    }
//}