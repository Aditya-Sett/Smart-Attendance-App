package com.mckv.attendance.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.remote.RetrofitClient
import com.mckv.attendance.ui.screens.AddClassroomScreen
import com.mckv.attendance.ui.screens.AddScheduleScreen
import com.mckv.attendance.ui.screens.ApproveAbsenceScreen
import com.mckv.attendance.ui.screens.AttendanceDetailsScreen
import com.mckv.attendance.ui.screens.AttendanceRecordsScreen
import com.mckv.attendance.ui.screens.AttendanceSummaryScreen
import com.mckv.attendance.ui.screens.ClassroomListScreen
import com.mckv.attendance.ui.screens.ConsiderAbsenceScreen
import com.mckv.attendance.ui.screens.CurriculumDetailsScreen
import com.mckv.attendance.ui.screens.CurriculumSummaryScreen
import com.mckv.attendance.ui.screens.DynamicDashboardScreen
import com.mckv.attendance.ui.screens.EditCurriculumScreen
import com.mckv.attendance.ui.screens.ExportAttendanceScreen
import com.mckv.attendance.ui.screens.HodControlsScreen
import com.mckv.attendance.ui.screens.HomeScreen
import com.mckv.attendance.ui.screens.LoginScreen
import com.mckv.attendance.ui.screens.MainHomeScreen
import com.mckv.attendance.ui.screens.ManageRoleScreen
import com.mckv.attendance.ui.screens.MyScheduleScreen
import com.mckv.attendance.ui.screens.ProfileScreen
import com.mckv.attendance.ui.screens.ReportScreen
import com.mckv.attendance.ui.screens.ReportViewModel
import com.mckv.attendance.ui.screens.ScheduleScreen
import com.mckv.attendance.ui.screens.SplashScreen
import com.mckv.attendance.ui.screens.StudentsAttendanceSummaryScreen

import com.mckv.attendance.ui.screens.take_attendance.TakeAttendanceScreen2
//import com.mckv.attendance.ui.screens.TakeAttendanceScreen
import com.mckv.attendance.ui.screens.UploadCurriculumScreen
import com.mckv.attendance.ui.screens.take_attendance.TakeAttendanceScreen2


@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "splash_screen") {

        composable("splash_screen") {
            SplashScreen(navController)
        }

        composable("main_home") {
            MainHomeScreen(navController)
        }
        composable("login_screen") {
            LoginScreen(navController)
        }
        composable("home") {
            HomeScreen(navController)
        }
        composable("view_analytics") {
            val viewModel: ReportViewModel = viewModel()
            ReportScreen(navController,viewModel = viewModel)
        }
        composable("schedule") {
            //ScheduleScreen(department = "CSE")
            val department = SessionManager.userDetails?.department
            if (department.isNullOrEmpty()) {
                Text("Department info not available. Please re-login.")
            } else {
                ScheduleScreen(department = department)
            }
        }

//        composable("teacher") {
//            TeacherScreen(navController)
//        }
//        composable("admin_dashboard") {
//            AdminDashboard(navController)
//        }

        composable("classroomList") {
            ClassroomListScreen(navController)
        }
        composable(
            route = "curriculumDetails/{className}/{department}/{effectiveYear}"
        ) {
            val className = it.arguments?.getString("className") ?: ""
            val department = it.arguments?.getString("department") ?: ""
            val effectiveYear = it.arguments?.getString("effectiveYear") ?: ""

            CurriculumDetailsScreen(
                className = className,
                department = department,
                effectiveYear = effectiveYear,
                navController = navController
            )
        }

        composable("curriculumSummary") {
            CurriculumSummaryScreen(navController = navController)
        }
        composable("uploadCurriculum") {
            UploadCurriculumScreen(navController)
        }
        composable("hod_controls") {
            HodControlsScreen(navController)
        }
        composable("my_schedule") {
            MyScheduleScreen(navController)
        }
        composable("editCurriculum/{id}") {
            val id = it.arguments?.getString("id") ?: ""
            EditCurriculumScreen(navController, id)
        }
        composable("addClassroom") {
            AddClassroomScreen(navController)
        }
        composable("add_schedule") {
            AddScheduleScreen(navController)
        }
        composable(
            route = "attendance_details/{teacherId}/{code}/{gen}/{exp}"
        ) { backStack ->

            val teacherId = backStack.arguments?.getString("teacherId")!!
            val code = backStack.arguments?.getString("code")!!
            val gen = backStack.arguments?.getString("gen")!!
            val exp = backStack.arguments?.getString("exp")!!

            AttendanceDetailsScreen(
                teacherId = teacherId,
                code = code,
                generatedAt = gen,
                expiresAt = exp,
                navController = navController
            )
        }

        composable("take_attendance") {
            TakeAttendanceScreen2(navController)
        }
        composable("attendance_records") {
            AttendanceRecordsScreen(navController)
        }
        composable("export_attendance") {
            ExportAttendanceScreen(navController)
        }
        composable("consider_absence") {
            ConsiderAbsenceScreen(navController)
        }
        composable("approve_absence") { ApproveAbsenceScreen(navController) }
        composable("attendance_summary") {
            //val context = LocalContext.current
            //val sessionManager = SessionManager(context.applicationContext)
            val studentId = SessionManager.userDetails?.userId ?: ""
            val department = SessionManager.userDetails?.department ?: ""
            AttendanceSummaryScreen(
                studentId = studentId,
                department = department,
                apiService = RetrofitClient.instance
            )
        }
        composable("students_attendance_summary") {
            StudentsAttendanceSummaryScreen(
                apiService = RetrofitClient.instance,
                navController = navController
            )
        }

        composable("dynamic_dashboard") {
            DynamicDashboardScreen(navController)
        }

        composable("manage_roles") {
            ManageRoleScreen(navController)
        }


        composable("profile") {
            ProfileScreen(navController)
        }


    }
}
