package com.mckv.attendance

import android.app.Application
import com.mckv.attendance.data.local.AttendanceManager
import com.mckv.attendance.data.local.PermissionManager
import com.mckv.attendance.data.local.SessionManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        //INITIALIZE APP MEMORY
        SessionManager.init(this)
        PermissionManager.init(this)
        AttendanceManager.init(this)
//        ActiveCodeManager.init(this)
    }
}