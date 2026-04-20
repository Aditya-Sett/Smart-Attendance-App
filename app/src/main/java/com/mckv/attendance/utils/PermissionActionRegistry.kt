package com.mckv.attendance.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.mckv.attendance.ui.components.UiPermissionAction

//This file maps backend permissions → UI actions.
object PermissionActionRegistry {

    val actions = mapOf(

        "MANAGE_USER" to UiPermissionAction(
            "MANAGE_USER",
            "Management",
            "Manage User",
            Icons.Default.Add,
            "manage_user"
        ),

//        "USER_UPDATE" to UiPermissionAction(
//            "USER_UPDATE",
//            "User",
//            "Update User",
//            Icons.Default.Edit,
//            "update_user"
//        ),

        "VIEW_ANALYTICS" to UiPermissionAction(
            "VIEW_ANALYTICS",
            "Analysis",
            "View Analytics",
            Icons.Default.Delete,
            "view_analytics"
        ),

        "MANAGE_ATTENDANCE" to UiPermissionAction(
            "MANAGE_ATTENDANCE",
            "Attendance",
            "Manage Attendance",
            Icons.Default.CheckCircle,
            "take_attendance"
        ),

        "VIEW_ATTENDANCE" to UiPermissionAction(
            "VIEW_ATTENDANCE",
            "Attendance",
            "View Attendance",
            Icons.Default.ShowChart,
            "view_attendance"
        ),

        "MANAGE_TIMETABLE" to UiPermissionAction(
            "MANAGE_TIMETABLE",
            "Management",
            "Manage Timetable",
            Icons.Default.CalendarMonth,
            "create_timetable"
        ),

       "MANAGE_CURRICULAM" to UiPermissionAction(
            "MANAGE_CURRICULAM",
            "Management",
            "Manage Curriculum",
            Icons.Default.Edit,
            "edit_cirriculum"
        ),

        "TAKE_ATTENDANCE" to UiPermissionAction(
            "TAKE_ATTENDANCE",
            "Attendance",
            "take attendance",
            Icons.Default.Delete,
            "take_attendance"
        ),

        "REPORT_VIEW" to UiPermissionAction(
            "REPORT_VIEW",
            "Analysis",
            "View Reports",
            Icons.Default.Download,
            "view_reports"
        ),

        "REPORT_GENERATE" to UiPermissionAction(
            "REPORT_GENERATE",
            "Analysis",
            "Generate Report",
            Icons.Default.Assessment,
            "export_attendance"
        ),

        "MANAGE_MEDICAL_LEAVE" to UiPermissionAction(
            "MANAGE_MEDICAL_LEAVE",
            "Departmental",
            "Medical Leave",
            Icons.Default.Schedule,
            "consider_absence"
        ),

        //Role Management Permissions
        "MANAGE_ROLE" to UiPermissionAction(
            "MANAGE_ROLE",
            "Management",
            "Manage Roles",
            Icons.Default.AdminPanelSettings,
            "manage_roles"
        ),

//        "DELETE_ROLE" to UiPermissionAction(
//            "DELETE_ROLE",
//            "Administration",
//            "Manage Roles",
//            Icons.Default.AdminPanelSettings,
//            "manage_roles"
//        ),

        "APPLY_MEDICAL_LEAVE" to UiPermissionAction(
            "APPLY_MEDICAL_LEAVE",
            "Departmental",
            "Apply Medical Leave",
            Icons.Default.AdminPanelSettings,
            "manage_roles"
        ),

        "VIEW_TIMETABLE" to UiPermissionAction(
            "VIEW_TIMETABLE",
            "Departmental",
            "View Timetable",
            Icons.Default.AdminPanelSettings,
            "manage_roles"
        )
    )
}