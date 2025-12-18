package com.mckv.attendance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

data class AdminOption(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(navController: NavHostController) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    val options = listOf(
        AdminOption("Manage Users", Icons.Default.Person, "manageUsers"),
        AdminOption("Manage Schedules", Icons.Default.Schedule, "manageSchedules"),
        AdminOption("View Reports", Icons.Default.BarChart, "viewReports"),
        AdminOption("Classroom Details", Icons.Default.LocationOn, "classroomList"),
        AdminOption("Settings", Icons.Default.Settings, "settings"),
        AdminOption("Notifications", Icons.Default.Notifications, "notifications"),
        AdminOption("Logout", Icons.Default.Logout, "logout")
    )

    val drawerOptions = listOf(
        DrawerOption("Dashboard", Icons.Default.Dashboard, "adminDashboard"),
        DrawerOption("Curriculum Details", Icons.Default.MenuBook, "curriculumSummary"),
        DrawerOption("User Management", Icons.Default.People, "manageUsers"),
        DrawerOption("System Settings", Icons.Default.Settings, "systemSettings"),
        DrawerOption("Help & Support", Icons.Default.Help, "helpSupport"),
        DrawerOption("About", Icons.Default.Info, "about")
    )

    ModalNavigationDrawer(
        //drawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet (modifier = Modifier.width(280.dp)) {
                // Drawer header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Admin",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Administrator",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "admin@mckv.edu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }


                Divider()

                // Drawer items
                drawerOptions.forEach { item ->
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = { Text(item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface) },
                        selected = false,
                        onClick = {
                            navController.navigate(item.route)
                            scope.launch {
                                drawerState.close()
                            }
                            // Close drawer after navigation
                            // You can use drawerState.close() here if you have access to it
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Dashboard") },
                    navigationIcon = {
                        IconButton(
                            onClick = {scope.launch {
                            drawerState.open()}
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(options) { option ->
                    DashboardCard(option) {
                        navController.navigate(option.route)
                    }
                }
            }
        }
    }
}

// Data class for drawer options
data class DrawerOption(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)

@Composable
fun DashboardCard(option: AdminOption, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = option.title,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}