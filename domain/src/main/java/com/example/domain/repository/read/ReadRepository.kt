package com.example.domain.repository.read

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ReadRepository {
        val dataFlow : StateFlow<ByteArray>
        //fun getDataFlow(): Flow<ByteArray>
       // fun onSerialRead(data: ByteArray) : Flow<ByteArray>
  //  val datasFlow : MutableSharedFlow<ArrayDeque<ByteArray>>
//    fun onSerialRead(datas: ArrayDeque<ByteArray>)
//     suspend fun onSerialRead(data: ByteArray)
   //suspend fun read()

}