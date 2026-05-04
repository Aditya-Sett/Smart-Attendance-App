package com.mckv.attendance.ui.screens.student

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.utils.ensureBluetoothPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    init {
        loadSessionData()
    }

    private fun loadSessionData() {
        val user = SessionManager.userDetails
        _uiState.update {
            it.copy(
                studentId = user?.userId ?: "Unknown",
                department = user?.department ?: "Unknown",
                academicYear = user?.studentProfile?.academicYear ?: "Unknown",
                sem = user?.studentProfile?.semester ?: "Unknown",
                admissionYear = user?.studentProfile?.admissionYear ?: "Unknown"
            )
        }
    }

    fun checkBluetoothStatus(context: Context, activity: Activity) {
        if (!ensureBluetoothPermissions(activity)) return

        viewModelScope.launch {
            _uiState.update { it.copy(bluetoothChecking = true) }

            val bluetoothAdapter: BluetoothAdapter? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(BluetoothManager::class.java)?.adapter
                } else {
                    @Suppress("DEPRECATION")
                    BluetoothAdapter.getDefaultAdapter()
                }

            val supported = bluetoothAdapter != null
            val enabled = bluetoothAdapter?.isEnabled == true

            Log.d("StudentVM", "BT supported=$supported enabled=$enabled")

            _uiState.update {
                it.copy(
                    bluetoothSupported = supported,
                    bluetoothEnabled = enabled,
                    bluetoothChecking = false
                )
            }
        }
    }

    fun onBluetoothStateChanged(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_ON -> _uiState.update {
                it.copy(bluetoothEnabled = true, bluetoothChecking = false)
            }
            BluetoothAdapter.STATE_OFF -> _uiState.update {
                it.copy(bluetoothEnabled = false, bluetoothChecking = false)
            }
            BluetoothAdapter.STATE_TURNING_ON,
            BluetoothAdapter.STATE_TURNING_OFF -> _uiState.update {
                it.copy(bluetoothChecking = true)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun requestBluetoothEnable(activity: Activity) {
        if (!_uiState.value.bluetoothEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(intent, 1001)
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }
}