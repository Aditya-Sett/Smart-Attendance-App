package com.mckv.attendance.utils


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
//import android.bluetooth.le.AdvertiseData
//import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.*
import androidx.compose.material3.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import java.util.UUID

@Composable
fun CheckBleSupport(context: Context) {
    val pm = context.packageManager
    val isBleSupported = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    if (!isBleSupported) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("BLE Not Supported") },
            text = { Text("Your device does not support Bluetooth Low Energy (BLE). The attendance system will not work on this phone.") },
            confirmButton = {
                Button(onClick = {
                    (context as? Activity)?.finish()
                }) {
                    Text("OK")
                }
            }
        )
    }
}

private fun hasBlePermissions(context: Context): Boolean {
    val permissions = arrayOf(
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    return permissions.all { perm ->
        ActivityCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}

private fun requestBlePermissions(activity: Activity) {
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        ),
        1001
    )
}



@RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
fun startBleAdvertising(
    context: Context,
    activity: Activity,
    onUuidGenerated: (String) -> Unit
) {

    // Permission check whether Bluetooth Permission are granted or not
    if (!hasBlePermissions(context)) {
        requestBlePermissions(activity)
        return
    }

    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // Get the system Bluetooth Adapter
    val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter.bluetoothLeAdvertiser // Get the BLE Advertiser

    if (advertiser == null) {
        println("BLE advertising not supported")
        return
    }

    val tempUuid = UUID.randomUUID().toString() // Convert to string

    val advertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        .setConnectable(false)
        .setTimeout(0)
        .build()

    val advertiseData = AdvertiseData.Builder()
        .addServiceUuid(ParcelUuid(UUID.fromString(tempUuid)))
        .setIncludeDeviceName(false)
        .build()

    advertiser.startAdvertising(
        advertiseSettings,
        advertiseData,
        object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                println("Advertising started with UUID = $tempUuid")

                // Send UUID back ✔
                onUuidGenerated(tempUuid)
            }

            override fun onStartFailure(errorCode: Int) {
                println("Failed: $errorCode")
            }
        }
    )
}


@SuppressLint("MissingPermission", "ServiceCast")
fun scanForTeacherUuid(
    context: Context,
    backendUuid: String,
    onResult: (Boolean) -> Unit
) {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter

    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
        onResult(false)
        return
    }

    val scanner = bluetoothAdapter.bluetoothLeScanner
    if (scanner == null) {
        onResult(false)
        return
    }

    var matchFound = false

    val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            val scanRecord = result.scanRecord ?: return
            val serviceUuids = scanRecord.serviceUuids ?: return

            // 🔍 Linear search for UUID match
            for (parcelUuid in serviceUuids) {
                if (parcelUuid.uuid.toString().equals(backendUuid, ignoreCase = true)) {
                    matchFound = true
                    break
                }
            }

            if (matchFound) {
                scanner.stopScan(this)
                onResult(true)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            onResult(false)
        }
    }

    // Start scanning
    scanner.startScan(callback)

    // Stop scanning after 5 seconds
    Handler(Looper.getMainLooper()).postDelayed({
        scanner.stopScan(callback)
        if (!matchFound) onResult(false)
    }, 5000)
}
