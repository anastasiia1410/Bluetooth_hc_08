package com.example.domain.repository.write

import android.location.Address
import com.example.data.service.SerialService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WriteRepositoryImpl(private val service: SerialService) : WriteRepository {
    override val isSend: StateFlow<Boolean>
        get() = service.isSend.asStateFlow()

    override suspend fun write(address: Byte, data: Byte) {
        service.write(address,data)
    }
}