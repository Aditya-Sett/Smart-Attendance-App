package com.mckv.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScreen(navController: NavHostController) {

    val teacherId = SessionManager.teacherId ?: "Unknown"
    val department = SessionManager.department ?: "Not Specified"

    // State for selected tab
    var selectedTab by remember { mutableStateOf(0) } // 0 = Attendance, 1 = Schedule

    // State for three-dot menu expansion
    var expanded by remember { mutableStateOf(false) }

    // 🔹 Add User Interaction Handler for Token Expiry Detection
    UserInteractionHandler(navController = navController)

    Scaffold(
        modifier = Modifier.interactionDetection(),
        topBar = {
            Column {
                // CommonTopBar without the three-dot menu
                CommonTopBar(
                    title = "Smart Attendance",
                    navController = navController
                )

                // Welcome Section with three-dot menu (Like UTS top section)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2196F3))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Teacher info column
                    Column(
                        modifier = Modifier.weight(1f)
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
                    if (SessionManager.userRole == "HOD") { // You'll need to add this to SessionManager
                        Card(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { navController.navigate("hod_controls") },
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFD700) // Gold color for distinction
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "HOD Controls",
                                    tint = Color(0xFF1E3A8A), // Dark blue for contrast
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "HOD",
                                    color = Color(0xFF1E3A8A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Three-dot menu
                    Box {
                        IconButton(
                            onClick = { expanded = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Dropdown menu with dynamic content based on selected tab
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .background(Color.White)
                                .padding(vertical = 4.dp)
                        ) {
                            // Show different menu items based on selected tab
                            if (selectedTab == 0) {
                                // Attendance Tab Menu Items
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Export All Records",
                                            color = Color.Black
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        navController.navigate("export_all_attendance")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Attendance Statistics",
                                            color = Color.Black
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        navController.navigate("attendance_stats")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.ShowChart,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Bulk Consider Absence",
                                            color = Color.Black
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        navController.navigate("bulk_consider_absence")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Group,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3)
                                        )
                                    }
                                )
                            } else {
                                // Schedule Tab Menu Items - Now with Add, Edit, Delete
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "My Schedule",
                                            color = Color.Black
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        navController.navigate("my_schedule")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Add New Schedule",
                                            color = Color.Black
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        navController.navigate("add_schedule")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Edit Schedule",
                                            color = Color.Black
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        // TODO: Navigate to Edit Schedule screen
                                        navController.navigate("edit_schedule")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Delete Schedule",
                                            color = Color.Black
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        // TODO: Navigate to Delete Schedule screen
                                        navController.navigate("delete_schedule")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3)
                                        )
                                    }
                                )
                                Divider(
                                    color = Color.LightGray,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                // Additional useful schedule options
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Calendar View",
                                            color = Color.Black
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        navController.navigate("calendar_view")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Tabs Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2CA0EE))
                        .padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 2.dp)
                ) {
                    ManagementTabsSection(
                        selectedTab = selectedTab,
                        onTabSelected = { tabIndex ->
                            selectedTab = tabIndex
                            // Close menu when switching tabs
                            expanded = false
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Content Section
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Content based on selected tab
                when (selectedTab) {
                    0 -> AttendanceManagementSection(navController)
                    1 -> ScheduleManagementSection(navController) // Now free for other work
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
            .background(Color.Transparent),
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
    // This space is now free for other work
    // You can add any other content here like:
    // - Schedule calendar view
    // - Upcoming schedule list
    // - Schedule statistics
    // - etc.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Schedule Management",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )

        Text(
            text = "Use the three-dot menu (⋮) in the top-right corner\nto manage your schedules",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 16.dp)
        )

        // You can add more content here as needed
        // For example:
        // - Upcoming schedules list
        // - Schedule calendar
        // - Quick stats
    }
}