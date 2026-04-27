package com.mckv.attendance.ui.screens.take_attendance


import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.mckv.attendance.MainActivity
import com.mckv.attendance.R
import com.mckv.attendance.data.local.ActiveCodeManager
import java.util.UUID

class BleAdvertisingService : Service() {

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private val handler = Handler(Looper.getMainLooper())

    // 1. Define the Receiver to monitor Bluetooth State
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                    Log.d("BLE_SERVICE", "Bluetooth turned off, stopping service...")
                    stopSelf() // This will trigger onDestroy() and clear storage
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // 2. Register the receiver when the service is created
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uuidString = intent?.getStringExtra("UUID") ?: return START_NOT_STICKY
        val durationMillis = intent.getLongExtra("DURATION", 300000L) // Default 5 mins

        // 1. Start Foreground with Notification (Required by Android)
        startForeground(101, createNotification())

        // 2. Start Bluetooth Advertising
        startAdvertising(uuidString)

        // 3. Auto-stop when time is up
        handler.postDelayed({
            stopSelf()
        }, durationMillis)

        return START_STICKY
    }

    private fun startAdvertising(uuidString: String) {
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        // Check for Bluetooth Advertise permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADVERTISE)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE_SERVICE", "Missing BLUETOOTH_ADVERTISE permission")
            stopSelf() // Stop the service so the notification disappears
            return
        }

        // Check if Bluetooth is actually ON
        if (adapter == null || !adapter.isEnabled) {
            stopSelf()
            return
        }

        bluetoothLeAdvertiser = adapter.bluetoothLeAdvertiser

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(false)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(UUID.fromString(uuidString)))
            .build()

        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d("BLE_SERVICE", "Advertising started successfully")
        }
        override fun onStartFailure(errorCode: Int) {
            Log.e("BLE_SERVICE", "Advertising failed: $errorCode")
        }
    }

    private fun createNotification(): Notification {
        val channelId = "attendance_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Attendance Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Attendance System Active")
            .setContentText("Broadcasting BLE signal for students...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your icon
            .setContentIntent(pendingIntent)
            .build()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onDestroy() {

        // 3. Unregister the receiver to prevent memory leaks
        try {
            unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            Log.e("BLE_SERVICE", "Receiver already unregistered")
        }

        // Check permission again for stopAdvertising on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            }
        } else {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        }

        ActiveCodeManager.clearActiveCode(applicationContext)

        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}