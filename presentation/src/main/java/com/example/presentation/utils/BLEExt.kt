package com.example.presentation.utils

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService

val BluetoothGattService.serviceType: String
    get() = when (type) {
        BluetoothGattService.SERVICE_TYPE_PRIMARY -> "primary"
        else -> "secondary"
    }

 fun BluetoothGattCharacteristic.describeProperties(): String =
    mutableListOf<String>().run {
        if (isReadable) add("Read")
        if (isWriteable) add("Write")
        if (isNotifiable) add("Notify")
        joinToString(" ")
    }

 val BluetoothGattCharacteristic.isNotifiable: Boolean
    get() = properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

 val BluetoothGattCharacteristic.isReadable: Boolean
    get() = properties and BluetoothGattCharacteristic.PROPERTY_READ != 0

 val BluetoothGattCharacteristic.isWriteable: Boolean
    get() = properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0