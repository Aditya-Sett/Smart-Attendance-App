package com.mckv.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.ui.components.common.CommonTopBar
import com.mckv.attendance.utils.UserInteractionHandler
import com.mckv.attendance.utils.interactionDetection

@Composable
fun TeacherScreen(navController: NavHostController) {

    val teacherId = SessionManager.teacherId ?: "Unknown"
    val department = SessionManager.department ?: "Not Specified"

    // State for selected tab
    var selectedTab by remember { mutableStateOf(0) } // 0 = Attendance, 1 = Schedule

    // 🔹 Add User Interaction Handler for Token Expiry Detection
    UserInteractionHandler(navController = navController)

    Scaffold(
        modifier = Modifier.interactionDetection(),
        topBar = {
            Column {
                CommonTopBar(
                    title = "Smart Attendance",
                    navController = navController
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Welcome Section (Like UTS top section)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2196F3))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Welcome, $teacherId",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Department: $department",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2CA0EE))
                    .padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 2.dp)
            ) {
                // Using the new ManagementTabsSection instead of the old Row
                ManagementTabsSection(
                    selectedTab = selectedTab,
                    onTabSelected = { tabIndex -> selectedTab = tabIndex }
                )
            }

            // Content Section with title (Like UTS "NORMAL BOOKING" section)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    //.background(Color(0xFFD51111))
                    .padding(horizontal = 16.dp)
            ) {
                // Section Title (Like UTS "NORMAL BOOKING")
                /*Text(
                    text = if (selectedTab == 0) "ATTENDANCE OPERATIONS" else "SCHEDULE OPERATIONS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp)
                )*/

                // Content based on selected tab
                when (selectedTab) {
                    0 -> AttendanceManagementSection(navController)
                    1 -> ScheduleManagementSection(navController)
                }
            }
        }
    }
}

@Composable
fun ManagementTabsSection(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        TabItem("Attendance", Icons.Filled.CheckCircle),
        TabItem("Schedule", Icons.Filled.CalendarMonth)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            //.padding(vertical = 8.dp)
            .background(Color.Transparent),
            //.padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, tab ->
            ManagementTabCompact(
                tab = tab,
                isSelected = selectedTab == index,
                onClick = { onTabSelected(index) }
            )
        }
    }
}

@Composable
fun ManagementTabCompact(
    tab: TabItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(25.dp))
            .clickable { onClick() }
            .background(if (isSelected) Color.White else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.title,
            tint = if (isSelected) Color(0xFF2196F3) else Color.White,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = tab.title,
            color = if (isSelected) Color(0xFF2196F3) else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

data class TabItem(val title: String, val icon: ImageVector)

// The rest of your composables remain the same
@Composable
fun ManagementTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(48.dp),
        shape = RoundedCornerShape(6.dp),
        colors = if (isSelected) {
            ButtonDefaults.buttonColors(containerColor = Color.White)
        } else {
            ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        },
        elevation = null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color(0xFF2196F3) else Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                color = if (isSelected) Color(0xFF2196F3) else Color.White,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun AttendanceManagementSection(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { navController.navigate("take_attendance") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Take Attendance")
        }

        Button(
            onClick = { navController.navigate("attendance_records") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Attendance Records")
        }

        Button(
            onClick = { navController.navigate("students_attendance_summary") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Students Attendance History")
        }

        Button(
            onClick = { navController.navigate("export_attendance") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Show Attendance Records")
        }

        Button(
            onClick = { navController.navigate("consider_absence") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Consider Absence")
        }
    }
}

@Composable
fun ScheduleManagementSection(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                navController.navigate("add_schedule")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Add New Schedule")
        }

        Button(
            onClick = { /* TODO: Navigate to Edit Schedule screen */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Edit Schedule")
        }

        Button(
            onClick = { /* TODO: Navigate to Delete Schedule screen */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Delete Schedule")
        }
    }
}