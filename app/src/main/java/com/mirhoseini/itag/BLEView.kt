package com.mirhoseini.itag

import com.polidea.rxandroidble.RxBleDevice

/**
 * BLE View
 */
interface BLEView {
    fun onScanning()
    fun onConnecting(bleDevice: RxBleDevice)
    fun onConnected(bleDevice: RxBleDevice)
    fun onRegistered()
    fun onKeyPressed()
    fun onError(throwable: Throwable)
}