package com.example.data.service

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.security.InvalidParameterException
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
class GattCallback(private val context: Context) : BluetoothGattCallback(), CoroutineScope {
    private var serialService: SerialService? = null
    private val pairedReceiver: BroadcastReceiver
    private val disconnectedReceiver: BroadcastReceiver
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var delegate: BluetoothDelegate? = null
    private var device: BluetoothDevice? = null
    private var pairedIntentFilter: IntentFilter? = null
    private var isConnected: Boolean? = false
    private var isCanceled: Boolean? = null
    private var writePending: Boolean? = null
    private var gatt: BluetoothGatt? = null
    private var writeBuffer: ArrayList<Byte>? = null
    private var payloadSize = DEFAULT_MTU - 3
    private val writeBufferMutex = Mutex()

    init {
        if (context is Activity) {
            throw InvalidParameterException("expected non UI context")
        }
        writeBuffer = ArrayList()
        pairedIntentFilter = IntentFilter()
        pairedIntentFilter?.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        pairedIntentFilter?.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        pairedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onPairingBroadcastReceiver(intent)
            }
        }
        disconnectedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                serialService?.onSerialIoError(IOException("background disconnect"))
                disconnect()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    suspend fun connect(service: SerialService, device: BluetoothDevice) {
        if (isConnected!! || gatt != null) {
            throw IOException("Bluetooth device already connected!")
        }
        isCanceled = false
        this.serialService = service
        context.registerReceiver(disconnectedReceiver, IntentFilter(ACTION_DISCONNECT))
        Log.d(TAG, "connect $device")
        context.registerReceiver(pairedReceiver, pairedIntentFilter)
        this.device = device
        Log.d(TAG, "connectGatt,LE")
        gatt = device.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE)
        suspendCancellableCoroutine { continuation ->
            if (gatt != null) {
                continuation.resume(Unit)
            } else {
                continuation.invokeOnCancellation {
                    throw IOException("gatt is not connected")

                }
            }
        }
    }

    fun disconnect() {
        Log.d(TAG, "disconnect")
        serialService = null
        device = null
        isCanceled = true
        launch {
            writeBufferMutex.withLock {
                writePending = false
                writeBuffer?.clear()
            }
        }
        readCharacteristic = null
        writeCharacteristic = null
        if (delegate != null) {
            delegate?.disconnect()
        }
        if (gatt != null) {
            gatt?.disconnect()
            try {
                gatt?.close()
            } catch (e: Exception) {
                Log.d(TAG, "${e.message}")
            }
            gatt = null
            isConnected = false
        }
        try {
            context.unregisterReceiver(pairedReceiver)
        } catch (e: Exception) {
           e.printStackTrace()
        }
        try {
            context.unregisterReceiver(disconnectedReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun write(address: Byte, data: Byte) {
        if (isCanceled!! || !isConnected!! || writeCharacteristic == null) {
            throw IOException("No connection is available")
        }
        val byteArray = byteArrayOf(address, data)
        writeCharacteristic?.value = byteArray
        if (!gatt?.writeCharacteristic(writeCharacteristic)!!) {
            onSerialIoError(IOException("Something went wrong"))
        } else {
            serialService?.isSend?.emit(true)
        }
        serialService?.isSend?.emit(false)
    }


    fun getName(): String {
        return device?.name ?: device?.address ?: ""
    }

    private fun onPairingBroadcastReceiver(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val device =
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            if (device == null || device != this.device)
                return
            when (intent.action) {
                BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                    val pairingVariant = intent.getIntExtra(BluetoothDevice.EXTRA_DEVICE, -1)
                    Log.d(TAG, "pairing request $pairingVariant")
                    launch { onSerialConnectError(IOException()) }

                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    val previousBondState =
                        intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                    Log.d(TAG, "bond state $previousBondState->$bondState")
                }
            }
        }
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "connect status $status, discoverServices")
            if (!gatt?.discoverServices()!!) {
                onSerialConnectError(IOException())
            }
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            if (isConnected!!) {
                onSerialIoError(IOException())
            } else {
                onSerialConnectError(IOException())
            }
        } else {
            Log.d(TAG, "unknown connect state $newState $status")
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        Log.d(TAG, "servicesDiscovered, status $status")
        if (isCanceled!!) {
            return
        }
        connectCharacteristics1(gatt!!)
    }

    private fun connectCharacteristics1(gatt: BluetoothGatt) {
        var sync = true
        writePending = false
        for (gattService in gatt.services) {
            when (gattService.uuid) {
                BLUETOOTH_LE_CC254X_SERVICE -> delegate = Cc245XDelegate()
                BLUETOOTH_LE_MICROCHIP_SERVICE -> delegate = MicrochipDelegate()
                BLUETOOTH_LE_NRF_SERVICE -> delegate = NrfDelegate()
                BLUETOOTH_LE_TIO_SERVICE -> delegate = TelitDelegate()
            }
            if (delegate != null) {
                sync = delegate?.isConnectedCharacteristic(gattService)!!
                break
            }
        }
        if (isCanceled!!) {
            return
        }
        if (delegate == null || readCharacteristic == null || writeCharacteristic == null) {
            for (gattService in gatt.services) {
                Log.d(TAG, "service " + gattService.uuid)
                for (characteristic in gattService.characteristics) {
                    Log.d(TAG, "characteristic " + characteristic.uuid)
                }
            }
            onSerialConnectError(IOException("no serial profile found"))
            return
        }
        if (sync) {
            connectCharacteristics2(gatt)
        }
    }


    private fun connectCharacteristics2(gatt: BluetoothGatt) {
        if (!gatt.requestMtu(MAX_MTU)) {
            onSerialConnectError(IOException())
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            payloadSize = mtu - 3
            Log.d(TAG, "payload size $payloadSize")
        }
        connectCharacteristics3(gatt!!)
    }

    private fun connectCharacteristics3(gatt: BluetoothGatt) {
        val writeProperties = writeCharacteristic!!.properties
        if ((writeProperties and (BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0
        ) {
            onSerialConnectError(IOException("write characteristic not writable"))
            return
        }
        if (!gatt.setCharacteristicNotification(readCharacteristic, true)) {
            onSerialConnectError(IOException("no notification for read characteristic"))
            return
        }
        val readDescriptor = readCharacteristic?.getDescriptor(BLUETOOTH_LE_CCCD)
        if (readDescriptor == null) {
            onSerialConnectError(IOException("no CCCD descriptor for read characteristic"))
            return
        }
        val readProperties = readCharacteristic!!.properties
        if ((readProperties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
            Log.d(TAG, "enable read indication")
            readDescriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else if ((readProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            Log.d(TAG, "enable read notification")
            readDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            onSerialConnectError(IOException("no indication/notification for read characteristic ($readProperties)"))
            return
        }
        Log.d(TAG, "writing read characteristic descriptor")
        if (!gatt.writeDescriptor(readDescriptor)) {
            onSerialConnectError(IOException("read characteristic CCCD descriptor not writable"))
        }
    }


    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int,
    ) {
        delegate?.onDescriptorWrite(gatt!!, descriptor!!, status)
        if (descriptor?.characteristic == readCharacteristic) {
            Log.d(TAG, "writing read characteristic descriptor finished, status= $status")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                onSerialConnectError(IOException("write descriptor failed"))
            } else {
                onSerialConnect()
                isConnected = true
                Log.d(TAG, "connected")
            }
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        if (isCanceled!!) {
            return
        }
        delegate?.onCharacteristicChanged(gatt, characteristic)
        if (isCanceled!!) {
            return
        }
        if (characteristic == readCharacteristic) {
            val data = readCharacteristic!!.value
            onSerialRead(data)

            Log.d(TAG, "read, len=" + data.size)
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int,
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        if (isCanceled!! || !isConnected!! || writeCharacteristic == null) {
            return
        }
        if (status != BluetoothGatt.GATT_SUCCESS) {
            onSerialIoError(IOException("write failed"))
            return
        }
        delegate?.onCharacteristicWrite(gatt!!, characteristic!!, status)
        if (isCanceled!!) {
            return
        }
        if (characteristic == writeCharacteristic) {
            Log.d(TAG, "write finished, ${writeCharacteristic!!.value.toDecimalValue()}")
            //writeBuffer?.clear()
            //writeNext()
        }
    }

    private fun ByteArray.toDecimalValue(): String {
        val value = joinToString("") { "%02x".format(it) }
        val decimalValue = Integer.parseInt(value, 16)
        return String.format("%02X", decimalValue)

    }

    private fun writeNext() {
        launch {
            val data: ByteArray?
            writeBufferMutex.withLock {
                if (writeBuffer?.isNotEmpty() == true && delegate?.canWrite() == true) {
                    writePending = true
                    data = byteArrayOf(writeBuffer!!.removeAt(0))
                } else {
                    writePending = false
                    data = null
                }
            }

            data?.let { byteArray ->
                writeCharacteristic?.value = byteArray
                if (!gatt?.writeCharacteristic(writeCharacteristic)!!) {
                    onSerialIoError(IOException("write failed"))
                } else {
                    Log.d(TAG, "write started, len=${data.size}")
                }
            }
        }
    }


    private fun onSerialConnect() {
        if (serialService != null)
            serialService?.onSerialConnect()
    }

    private fun onSerialConnectError(e: Exception) {
        isCanceled = true
        if (serialService != null)
            serialService?.onSerialConnectError(e)
    }

     fun onSerialRead(data: ByteArray) {
        if (serialService != null)
            serialService?.onSerialRead(data)
    }

    private fun onSerialIoError(e: Exception) {
        writePending = false
        isCanceled = true
        if (serialService != null)
            serialService?.onSerialIoError(e)
    }

    private inner class Cc245XDelegate : BluetoothDelegate() {
        override fun isConnectedCharacteristic(service: BluetoothGattService): Boolean {
            Log.d(TAG, "service cc254x uart")
            readCharacteristic = service.getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW)
            writeCharacteristic = service.getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW)
            return true
        }
    }

    private inner class MicrochipDelegate : BluetoothDelegate() {
        override fun isConnectedCharacteristic(service: BluetoothGattService): Boolean {
            readCharacteristic = service.getCharacteristic(BLUETOOTH_LE_MICROCHIP_CHAR_RW)
            writeCharacteristic = service.getCharacteristic(BLUETOOTH_LE_MICROCHIP_CHAR_W)
            if (writeCharacteristic == null) {
                writeCharacteristic = service.getCharacteristic(BLUETOOTH_LE_MICROCHIP_CHAR_RW)
            }
            return true
        }
    }

    private inner class NrfDelegate : BluetoothDelegate() {
        override fun isConnectedCharacteristic(service: BluetoothGattService): Boolean {
            val rw2 = service.getCharacteristic(BLUETOOTH_LE_NRF_CHAR_RW2)
            val rw3 = service.getCharacteristic(BLUETOOTH_LE_NRF_CHAR_RW3)
            if (rw2 != null && rw3 != null) {
                val rw2prop = rw2.properties
                val rw3prop = rw3.properties
                val rw2write = (rw2prop and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
                val rw3write = (rw3prop and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
                if (rw2write && rw3write) {
                    onSerialConnectError(IOException("multiple write characteristics $rw2prop/$rw3prop"))
                } else if (rw2write) {
                    writeCharacteristic = rw2
                    readCharacteristic = rw3
                } else if (rw3write) {
                    writeCharacteristic = rw3
                    readCharacteristic = rw2
                } else {
                    onSerialConnectError(IOException("no write characteristic $rw2prop/$rw3prop"))
                }
            }
            return true
        }
    }

    private inner class TelitDelegate : BluetoothDelegate() {
        private var readCreditsCharacteristic: BluetoothGattCharacteristic? = null
        private var writeCreditsCharacteristic: BluetoothGattCharacteristic? = null
        private var readCredits: Int? = null
        private var writeCredits: Int? = null

        override fun isConnectedCharacteristic(service: BluetoothGattService): Boolean {
            readCredits = 0
            writeCredits = 0
            readCharacteristic = service.getCharacteristic(BLUETOOTH_LE_TIO_CHAR_RX)
            writeCharacteristic = service.getCharacteristic(BLUETOOTH_LE_TIO_CHAR_TX)
            readCreditsCharacteristic =
                service.getCharacteristic(BLUETOOTH_LE_TIO_CHAR_RX_CREDITS)
            writeCreditsCharacteristic =
                service.getCharacteristic(BLUETOOTH_LE_TIO_CHAR_TX_CREDITS)
            if (readCharacteristic == null) {
                onSerialConnectError(IOException("read characteristic not found"))
                return false
            }
            if (writeCharacteristic == null) {
                onSerialConnectError(IOException("write characteristic not found"))
                return false
            }
            if (readCreditsCharacteristic == null) {
                onSerialConnectError(IOException("read credits characteristic not found"))
                return false
            }
            if (writeCreditsCharacteristic == null) {
                onSerialConnectError(IOException("write credits characteristic not found"))
            }
            if (!gatt?.setCharacteristicNotification(readCreditsCharacteristic, true)!!) {
                onSerialConnectError(IOException("no notification for read credits characteristic"))
                return false
            }
            val readCreditDescriptor =
                readCreditsCharacteristic?.getDescriptor(BLUETOOTH_LE_CCCD)
            if (readCreditDescriptor == null) {
                onSerialConnectError(IOException("no CCCD descriptor for read credits characteristic"))
                return false
            }
            readCreditDescriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            if (!gatt?.writeDescriptor(readCreditDescriptor)!!) {
                onSerialConnectError(IOException("read credits characteristic CCCD descriptor not writable"))
                return false
            }
            return false
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (descriptor.characteristic == readCreditsCharacteristic) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    onSerialConnectError(IOException("write credits descriptor failed"))
                } else {
                    connectCharacteristics2(gatt)
                }
            }
            if (descriptor.characteristic == readCharacteristic) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    readCharacteristic?.writeType =
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    writeCharacteristic?.writeType =
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    grantReadCredits()
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (characteristic == readCreditsCharacteristic) {
                val newCredits = readCreditsCharacteristic!!.value[0]
                launch {
                    writeBufferMutex.withLock {
                        writeCredits = writeCredits?.plus(newCredits)
                    }
                }

                if (!writePending!! && writeBuffer!!.isNotEmpty()) {
                    writeNext()
                }
            }
            if (characteristic == readCharacteristic) {
                grantReadCredits()
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (characteristic == writeCharacteristic) {
                launch {
                    writeBufferMutex.withLock {
                        if (writeCredits!! > 0) {
                            writeCredits = writeCredits!!.minus(1)
                        }
                    }
                }
            }
            if (characteristic == writeCharacteristic) {
                Log.d(TAG, "write credits finished, status: $status")
            }
        }

        override fun canWrite(): Boolean {
            return writeCredits!! > 0
        }

        override fun disconnect() {
            readCreditsCharacteristic = null
            writeCreditsCharacteristic = null
        }

        private fun grantReadCredits() {
            val minReadCredits = 16
            val maxReadCredits = 64
            if (readCredits!! > 0) {
                readCredits = readCredits!!.minus(1)
            }
            if (readCredits!! <= minReadCredits) {
                val newCredits = maxReadCredits - readCredits!!
                readCredits = readCredits!!.plus(newCredits)
                val data = byteArrayOf(newCredits.toByte())
                writeCreditsCharacteristic?.value = data
                if (!gatt?.writeCharacteristic(writeCreditsCharacteristic)!!) {
                    if (isConnected!!) {
                        onSerialIoError(IOException("write read credits failed"))
                    } else {
                        onSerialConnectError(IOException("write read credits failed"))
                    }
                }
            }
        }
    }

    private open class BluetoothDelegate {
        open fun isConnectedCharacteristic(service: BluetoothGattService): Boolean {
            return true
        }

        open fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
        }

        open fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
        }

        open fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
        }

        open fun canWrite(): Boolean {
            return true
        }

        open fun disconnect() {}
    }

    companion object {
        val BLUETOOTH_LE_CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val BLUETOOTH_LE_CC254X_SERVICE: UUID =
            UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val BLUETOOTH_LE_CC254X_CHAR_RW: UUID =
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val BLUETOOTH_LE_NRF_SERVICE: UUID =
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val BLUETOOTH_LE_NRF_CHAR_RW2: UUID =
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        val BLUETOOTH_LE_NRF_CHAR_RW3: UUID =
            UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val BLUETOOTH_LE_MICROCHIP_SERVICE: UUID =
            UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455")
        val BLUETOOTH_LE_MICROCHIP_CHAR_RW: UUID =
            UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616")
        val BLUETOOTH_LE_MICROCHIP_CHAR_W: UUID =
            UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3")
        val BLUETOOTH_LE_TIO_SERVICE: UUID =
            UUID.fromString("0000FEFB-0000-1000-8000-00805F9B34FB")
        val BLUETOOTH_LE_TIO_CHAR_TX: UUID =
            UUID.fromString("00000001-0000-1000-8000-008025000000")
        val BLUETOOTH_LE_TIO_CHAR_RX: UUID =
            UUID.fromString("00000002-0000-1000-8000-008025000000")
        val BLUETOOTH_LE_TIO_CHAR_TX_CREDITS: UUID =
            UUID.fromString("00000003-0000-1000-8000-008025000000")
        val BLUETOOTH_LE_TIO_CHAR_RX_CREDITS: UUID =
            UUID.fromString("00000004-0000-1000-8000-008025000000")

        const val MAX_MTU = 512
        const val ACTION_DISCONNECT = "com.example.bluetoothhc_08.Disconnect"
        const val NOTIFICATION_CHANNEL = "com.example.bluetoothhc_08.NotificationChannel"
        const val INTENT_CLASS_MAIN_ACTIVITY = "com.example.bluetoothhc_08.MainActivity"
        const val NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001
        const val TAG = "SerialSocket"
        const val DEFAULT_MTU = 23
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main
}


