package com.example.domain.repository.write

import kotlinx.coroutines.flow.StateFlow

interface WriteRepository {
    val isSend : StateFlow<Boolean>
    suspend fun write(address : Byte, data: Byte)
}