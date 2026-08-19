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
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import java.util.UUID



import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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


//@SuppressLint("MissingPermission", "ServiceCast")
//fun scanForTeacherUuid(
//    context: Context,
//    backendUuid: String,
//    onResult: (Boolean) -> Unit
//) {
//    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//    val bluetoothAdapter = bluetoothManager.adapter
//
//    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
//        onResult(false)
//        return
//    }
//
//    val scanner = bluetoothAdapter.bluetoothLeScanner
//    if (scanner == null) {
//        onResult(false)
//        return
//    }
//
////    var matchFound = false
//
////    val callback = object : ScanCallback() {
////        override fun onScanResult(callbackType: Int, result: ScanResult) {
////
////            val scanRecord = result.scanRecord ?: return
////            val serviceUuids = scanRecord.serviceUuids ?: return
////
////            // 🔍 Linear search for UUID match
////            for (parcelUuid in serviceUuids) {
////                if (parcelUuid.uuid.toString().equals(backendUuid, ignoreCase = true)) {
////                    matchFound = true
////                    break
////                }
////            }
////
////            if (matchFound) {
////                scanner.stopScan(this)
////                onResult(true)
////            }
////        }
////
////        override fun onScanFailed(errorCode: Int) {
////            onResult(false)
////        }
////    }
////
////    // Start scanning
////    scanner.startScan(callback)
////
////    // Stop scanning after 5 seconds
////    Handler(Looper.getMainLooper()).postDelayed({
////        scanner.stopScan(callback)
////        if (!matchFound) onResult(false)
////    }, 5000)
//
////    ------------------------------RENEW---------------------------------------------------
//
//    // 1. Build the Low Latency Settings
//    val settings = ScanSettings.Builder()
//        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//        .build()
//
//    // 2. Build the Filter (so Android does the searching for you)
//    val filter = ScanFilter.Builder()
//        .setServiceUuid(ParcelUuid.fromString(backendUuid))
//        .build()
//
//    var matchFound = false
//
//    val callback = object : ScanCallback() {
//        override fun onScanResult(callbackType: Int, result: ScanResult) {
//            // Because we used a filter, if this triggers, it's a guaranteed match!
//            matchFound = true
//            scanner.stopScan(this)
//            onResult(true)
//        }
//
//        override fun onScanFailed(errorCode: Int) {
//            onResult(false)
//        }
//    }
//
//    // 3. Start scanning with Filters and Settings
//    scanner.startScan(listOf(filter), settings, callback)
//
//    // Stop scanning after 5 seconds
//    Handler(Looper.getMainLooper()).postDelayed({
//        scanner.stopScan(callback)
//        if (!matchFound) onResult(false)
//    }, 5000)
//
////    -----------------------------RENEW END---------------------------------------------
//}

@SuppressLint("MissingPermission", "ServiceCast")
suspend fun scanForTeacherUuid(context: Context, backendUuid: String): Boolean = suspendCancellableCoroutine { cont ->
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val scanner = bluetoothManager.adapter?.bluetoothLeScanner

    if (scanner == null || bluetoothManager.adapter?.isEnabled == false) {
        cont.resume(false)
        return@suspendCancellableCoroutine
    }

    val settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    val filter = try {
        ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString(backendUuid))
            .build()
    } catch (e: IllegalArgumentException) {
        cont.resume(false)
        return@suspendCancellableCoroutine
    }

    val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (cont.isActive) {
                // Instantly stop scanning the moment we find the teacher
                scanner.stopScan(this)
                cont.resume(true)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            if (cont.isActive) {
                cont.resume(false)
            }
        }
    }

    // If the ViewModel cancels the job (e.g., student leaves the screen), stop the scanner automatically
    cont.invokeOnCancellation {
        scanner.stopScan(callback)
    }

    // Start the continuous scan
    scanner.startScan(listOf(filter), settings, callback)
}

//@SuppressLint("MissingPermission", "ServiceCast")
//fun scanForTeacherUuid(
//    context: Context,
//    backendUuid: String,
//    onResult: (Boolean) -> Unit
//) {
//    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//    val bluetoothAdapter = bluetoothManager.adapter
//
//    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
//        onResult(false)
//        return
//    }
//
//    val scanner = bluetoothAdapter.bluetoothLeScanner
//    if (scanner == null) {
//        onResult(false)
//        return
//    }
//
//    // 1. Build the Low Latency Settings
//    val settings = ScanSettings.Builder()
//        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//        .build()
//
//    // 2. Build the Filter
//    val filter = try {
//        ScanFilter.Builder()
//            .setServiceUuid(ParcelUuid.fromString(backendUuid))
//            .build()
//    } catch (e: IllegalArgumentException) {
//        // Safety check: Prevents crash if backend sends a badly formatted UUID string
//        onResult(false)
//        return
//    }
//
//    // 🛡️ Safety Flag: Ensures we only return ONE result per scan
//    var isFinished = false
//
//    val callback = object : ScanCallback() {
//        override fun onScanResult(callbackType: Int, result: ScanResult) {
//            if (isFinished) return // Prevent double-firing
//            isFinished = true
//
//            scanner.stopScan(this)
//            onResult(true)
//        }
//
//        override fun onScanFailed(errorCode: Int) {
//            if (isFinished) return // Prevent double-firing
//            isFinished = true
//
//            onResult(false)
//        }
//    }
//
//    // 3. Start scanning with Filters and Settings
//    scanner.startScan(listOf(filter), settings, callback)
//
//    // 4. Stop scanning after 5 seconds
//    Handler(Looper.getMainLooper()).postDelayed({
//        if (!isFinished) {
//            isFinished = true // Mark as finished so late callbacks are ignored
//            scanner.stopScan(callback)
//            onResult(false)
//        }
//    }, 5000)
//}



fun ensureBluetoothPermissions(activity: Activity): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        val missing = permissions.any {
            ActivityCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing) {
            ActivityCompat.requestPermissions(activity, permissions, 2001)
            return false   // stop, wait for permission
        }
    }
    return true
}