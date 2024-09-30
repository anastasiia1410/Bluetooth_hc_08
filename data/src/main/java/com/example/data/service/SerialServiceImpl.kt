package com.example.data.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.location.Address
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.data.R
import com.example.data.service.GattCallback.Companion.ACTION_DISCONNECT
import com.example.data.service.GattCallback.Companion.INTENT_CLASS_MAIN_ACTIVITY
import com.example.data.service.GattCallback.Companion.NOTIFICATION_CHANNEL
import com.example.data.service.GattCallback.Companion.NOTIFY_MANAGER_START_FOREGROUND_SERVICE
import com.example.data.service.GattCallback.Companion.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.Charset
import kotlin.coroutines.CoroutineContext

@SuppressLint("MissingPermission")
class SerialServiceImpl(
    context: Context, private var gatt: GattCallback?,

    ) : LifecycleService(),
    SerialService, CoroutineScope {
    private val queue1: ArrayDeque<ActionItem> = ArrayDeque()
    private val queue2: ArrayDeque<ActionItem> = ArrayDeque()
    private val lastRead: ActionItem = ActionItem(ActionType.Read)
    private val serviceMutex = Mutex()
    private val lastReadMutex = Mutex()
    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }
    private val bluetoothLeScanner by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }
    private var isScanning = false
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()


    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (isScanning) {
                val newDevice = result.device
                if (newDevice != null) {
                    _scannedDevices.update { devices ->
                        if (result.device in devices) devices else devices + newDevice
                    }
                }
            }
        }
    }

    private var serialService: SerialService? = null
    private var _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()
    private var _isSend = MutableStateFlow(false)
    override val isSend: MutableStateFlow<Boolean>
        get() = _isSend
    private var _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDevice>>
        get() = _scannedDevices.asStateFlow()

    private var _dataFlow = MutableStateFlow(byteArrayOf())
    override val dataFlow: StateFlow<ByteArray>
        get() = _dataFlow.asStateFlow()
    private var _datasFlow = MutableSharedFlow<ArrayDeque<ByteArray>>()
    override val datasFlow: MutableSharedFlow<ArrayDeque<ByteArray>>
        get() = _datasFlow


    override suspend fun scanLeDevice() {
        isScanning = true
        bluetoothLeScanner?.startScan(null, scanSettings, leScanCallback)
    }

    override fun stopScanLeDevice() {
        isScanning = false
        bluetoothLeScanner?.stopScan(leScanCallback)
    }

    override fun getDevice(macAddress: String): BluetoothDevice {
        return bluetoothAdapter?.getRemoteDevice(macAddress)!!
    }

    override suspend fun connect(device: BluetoothDevice) {
        if (!isScanning) {
            gatt?.connect(this, device)
            _isConnected.emit(true)
        }
    }

    override suspend fun disconnect() {
        _isConnected.emit(false)
        stopNotification()
        if (gatt != null) {
            gatt?.disconnect()
            gatt = null
        }
    }

    override suspend fun write(address: Byte, data: Byte) {
        if (!_isConnected.value) {
            throw IOException("Відсутнє підключення")
        }
        gatt?.write(address, data)
    }

    override suspend fun read(data: ByteArray) {
        if (!_isConnected.value) {
            throw IOException("Відсутнє підключення")
        }
        gatt?.onSerialRead(data)
    }


    override fun doAfterConnection() {
        //cancelNotification()

        for (item in queue1) {
            when (item.type) {
                ActionType.Connected -> serialService?.onSerialConnect()
                ActionType.ConnectedWithError -> serialService?.onSerialConnectError(item.e!!)
                ActionType.Read -> serialService?.onSerialRead(item.datas!!)
                ActionType.IoError -> serialService?.onSerialIoError(item.e!!)
            }
        }

        for (item in queue2) {
            when (item.type) {
                ActionType.Connected -> serialService?.onSerialConnect()
                ActionType.ConnectedWithError -> serialService?.onSerialConnectError(item.e!!)
                ActionType.Read -> serialService?.onSerialRead(item.datas!!)
                ActionType.IoError -> serialService?.onSerialIoError(item.e!!)
            }
        }

        queue1.clear()
        queue2.clear()
    }

    override fun detach() {
        if (_isConnected.value) {
            createNotification()
            serialService = null
        }
    }

//    private fun cancelNotification() {
//        stopForeground(STOP_FOREGROUND_REMOVE)
//    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                "Background service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.setShowBadge(false)
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val disconnectIntent = Intent()
            .setAction(ACTION_DISCONNECT)
        val restartIntent = Intent()
            .setClassName(this, INTENT_CLASS_MAIN_ACTIVITY)
            .setAction(Intent.CATEGORY_LAUNCHER)
        val flags = PendingIntent.FLAG_IMMUTABLE
        val disconnectPendingIntent = PendingIntent.getBroadcast(this, 1, disconnectIntent, flags)
        val restartPendingIntent = PendingIntent.getActivity(this, 1, restartIntent, flags)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(androidx.core.R.drawable.notification_bg)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(if (gatt != null) "Connected to ${gatt?.getName()}" else "Background Service")
            .setContentIntent(restartPendingIntent)
            .setOngoing(true)
            .addAction(
                NotificationCompat.Action(
                    com.google.android.material.R.drawable.notification_bg,
                    "Віключити",
                    disconnectPendingIntent
                )
            )
        val notification = builder.build()
        startForeground(NOTIFY_MANAGER_START_FOREGROUND_SERVICE, notification)

    }

    private fun stopNotification() {

    }

    override fun onSerialConnect() {
        if (_isConnected.value) {
            launch {
                serviceMutex.withLock {
                    if (serialService != null) {
                        withContext(Dispatchers.Main) {
                            if (serialService != null) {
                                serialService?.onSerialConnect()
                            } else {
                                queue1.add(ActionItem(ActionType.Connected))
                            }
                        }

                    } else {
                        queue2.add(ActionItem(ActionType.Connected))
                    }
                }
            }
        }
    }

    override fun onSerialConnectError(e: Exception) {
        if (_isConnected.value) {
            launch {
                serviceMutex.withLock {
                    if (serialService != null) {
                        withContext(Dispatchers.Main) {
                            if (serialService != null) {
                                serialService?.onSerialConnectError(e)
                            } else {
                                queue1.add(ActionItem(ActionType.ConnectedWithError, null, e))
                                disconnect()
                            }
                        }
                    } else {
                        queue2.add(ActionItem(ActionType.ConnectedWithError, null, e))
                    }
                }
            }
        }
    }

    override fun onSerialRead(data: ByteArray) {
        if (_isConnected.value) {
            Log.d(TAG, String(data, Charset.forName("UTF-8")))
            launch {
                _dataFlow.emit(data)
            }
        }
    }


//    override fun dataFlow(): Flow<ByteArray> {
//        return serialService?.dataFlow!!.asStateFlow()
//    }

    override fun onSerialRead(datas: ArrayDeque<ByteArray>) {
        throw UnsupportedOperationException()
    }

    override fun onSerialIoError(e: Exception) {
        if (_isConnected.value) {
            launch {
                serviceMutex.withLock {
                    if (serialService != null) {
                        withContext(Dispatchers.Main) {
                            if (serialService != null) {
                                serialService?.onSerialIoError(e)
                            } else {
                                queue1.add(ActionItem(ActionType.IoError, null, e))
                                disconnect()
                            }
                        }
                    } else {
                        queue2.add(ActionItem(ActionType.IoError, null, e))
                        disconnect()
                    }
                }
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main
}


enum class ActionType {
    Connected, ConnectedWithError, Read, IoError
}

data class ActionItem(
    val type: ActionType,
    var datas: ArrayDeque<ByteArray>? = null,
    val e: Exception? = null,
) {

    fun init() {
        datas = ArrayDeque()
    }

    fun add(data: ByteArray) {
        datas?.add(data)
    }
}
