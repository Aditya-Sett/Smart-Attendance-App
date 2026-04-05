package com.mckv.attendance.ui.components.common

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BluetoothStatusIndicator() {

    val context = LocalContext.current

    var bluetoothEnabled by remember { mutableStateOf(false) }
    var bluetoothSupported by remember { mutableStateOf(true) }
    var bluetoothChecking by remember { mutableStateOf(true) }

    fun checkBluetoothStatus() {

        val bluetoothAdapter: BluetoothAdapter? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(BluetoothManager::class.java)
                manager?.adapter
            } else {
                BluetoothAdapter.getDefaultAdapter()
            }

        if (bluetoothAdapter == null) {
            bluetoothSupported = false
            bluetoothEnabled = false
        } else {
            bluetoothSupported = true
            bluetoothEnabled = bluetoothAdapter.isEnabled
        }

        bluetoothChecking = false
    }

    LaunchedEffect(Unit) {
        checkBluetoothStatus()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = when {
                        bluetoothChecking -> Color.Yellow
                        bluetoothEnabled -> Color.Green
                        else -> Color.Red
                    },
                    shape = RoundedCornerShape(50)
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = when {
                bluetoothChecking -> "Bluetooth: Checking..."
                !bluetoothSupported -> "Bluetooth: Not Supported"
                bluetoothEnabled -> "Bluetooth: ON"
                else -> "Bluetooth: OFF"
            },
            color = Color.White,
            fontSize = 12.sp
        )

        if (!bluetoothEnabled && bluetoothSupported) {

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    bluetoothChecking = true
                    checkBluetoothStatus()
                },
                modifier = Modifier.size(16.dp)
            ) {

                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color.White
                )
            }
        }
    }
}