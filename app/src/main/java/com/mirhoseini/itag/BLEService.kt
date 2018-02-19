package com.mirhoseini.itag

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import com.polidea.rxandroidble.NotificationSetupMode
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleDevice
import com.polidea.rxandroidble.scan.ScanFilter
import com.polidea.rxandroidble.scan.ScanSettings
import java.util.*

/**
 * BLE Service
 */
class BLEService : Service() {

    private val TAG: String = BLEService::class.java.simpleName

    private lateinit var rxBleClient: RxBleClient
    private val serviceUUID: ParcelUuid = ParcelUuid.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val characteristicUUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    private var view: BLEView? = null
    private val serviceBinder = ServiceBinder()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service onCreate")

        rxBleClient = RxBleClient.create(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service onStartCommand")

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "Service onBind")
        return serviceBinder
    }

    fun bindView(view: BLEView) {
        this.view = view
    }

    fun unbindView() {
        this.view = null
    }

    fun scan() {
        view?.onScanning()

        rxBleClient.scanBleDevices(scanSettings(), scanFilter())
                .first()
                .subscribe(
                        { scanResult ->
                            connect(scanResult.bleDevice)
                        }, onError())
    }

    private fun scanSettings(): ScanSettings =
            ScanSettings.Builder()
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

    private fun scanFilter(): ScanFilter =
            ScanFilter.Builder()
                    .setServiceUuid(serviceUUID)
                    .build()

    private fun connect(bleDevice: RxBleDevice) {
        view?.onConnecting(bleDevice)

        bleDevice.establishConnection(true)
                .subscribe({ rxBleConnection ->
                    view?.onConnected(bleDevice)

                    rxBleConnection.setupIndication(characteristicUUID, NotificationSetupMode.COMPAT)
                            .subscribe({ observable ->
                                view?.onRegistered()
                                observable.subscribe({ _ ->
                                    view?.onKeyPressed()
                                    pressKey()
                                }, onError())
                            }, onError())
                }, onError())
    }

    fun pressKey() {
        Log.i(TAG, "Button Pressed!!!")
        val intent = Intent("com.mirhoseini.itag.button_pressed")
        sendBroadcast(intent)
    }

    private fun onError(): (Throwable) -> Unit {
        return { throwable ->
            Log.e(TAG, throwable.message)
            view?.onError(throwable)
        }
    }

    inner class ServiceBinder : Binder() {
        internal val service: BLEService
            get() = this@BLEService
    }
}