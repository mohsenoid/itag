package com.mirhoseini.itag


import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Toast
import com.polidea.rxandroidble.RxBleDevice
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), BLEView {

    private lateinit var dateFormat: SimpleDateFormat
    private var bleService: BLEService? = null
    private var bleServiceBound = false

    //connect to the service
    private val bleServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? BLEService.ServiceBinder
            bleService = binder?.service

            bleService?.bindView(this@MainActivity)

            bleServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleServiceBound = false
        }
    }

    fun onHeyChrisClick(view: View) {
        bleService?.pressKey()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dateFormat = SimpleDateFormat("hh:mm:ss", Locale.US)

        // set scrolling method for log TextView
        log.movementMethod = ScrollingMovementMethod()


        action.setOnClickListener { bleService?.scan() }

        printLog("app started")
    }

    override fun onResume() {
        super.onResume()
        if (checkAllRequiredPermissions()) {
            if (!bleServiceBound) {
                val bleServiceIntent = Intent(applicationContext, BLEService::class.java)
                applicationContext.bindService(bleServiceIntent, bleServiceConnection, Context.BIND_AUTO_CREATE)

//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        context.startForegroundService(mainServiceIntent)
//                    } else {
                applicationContext.startService(bleServiceIntent)
//                    }
            }
        }
    }

    private fun checkAllRequiredPermissions(): Boolean {

        val requiredPermissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
        )

        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(applicationContext, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_ALL_PERMISSIONS)
                return false
            }
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_ALL_PERMISSIONS -> finishIfRequiredPermissionsNotGranted(grantResults)
            else -> {
            }
        }
    }

    private fun finishIfRequiredPermissionsNotGranted(grantResults: IntArray) {
        if (grantResults.isNotEmpty()) {
            for (grantResult in grantResults) {
                // If request is cancelled, the result arrays are empty.
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Required permissions not granted! We need them all!!!", Toast.LENGTH_LONG).show()
                    finish()
                    break
                }
            }
        } else {
            Toast.makeText(this, "Required permissions not granted! We need them all!!!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onScanning() {
        setAction("Scanning...")
        action.setOnClickListener(null)
        printLog("start scanning...")
    }

    override fun onConnecting(bleDevice: RxBleDevice) {
        printLog("found ${bleDevice.nameToString()})")
        setAction("Connecting...")
        printLog("connecting to ${bleDevice.nameToString()}")
    }

    override fun onConnected(bleDevice: RxBleDevice) {
        setAction("Connected")
        printLog("connected to ${bleDevice.nameToString()})")
    }

    override fun onRegistered() {
        printLog("registered!")
    }

    override fun onKeyPressed() {
        printLog("Button pressed!!")
    }

    override fun onError(throwable: Throwable) {
        setAction("Scan")
        action.setOnClickListener { bleService?.scan() }

        printLog(throwable.message ?: throwable.toString())
    }

    private fun setAction(actionText: String) {
        action.post {
            action.text = actionText
        }
    }

    private fun printLog(message: String) {
        val date = Date()
        log.post {
            log.text = "${dateFormat.format(date)} - $message\n--------\n${log.text}"
        }
    }

    private fun RxBleDevice.nameToString() =
            "${this.name?.trim()}(${this.macAddress?.trim()})"

    companion object {
        private const val REQUEST_ALL_PERMISSIONS = 1001
    }
}
