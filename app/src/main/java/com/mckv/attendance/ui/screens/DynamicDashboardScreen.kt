package com.mckv.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mckv.attendance.data.local.PermissionManager
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.ui.components.UiPermissionAction
import com.mckv.attendance.ui.components.cards.ActionCard
import com.mckv.attendance.ui.components.common.CommonTopBar
import com.mckv.attendance.utils.PermissionActionRegistry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun DynamicDashboardScreen(navController: NavHostController) {

    val permissions = PermissionManager.getPermissions()

    val actions = permissions
        .mapNotNull { PermissionActionRegistry.actions[it] }
        .distinctBy { it.route }

    val groupedActions = actions.groupBy { it.category }

    val categories = groupedActions.keys.toList()

    var selectedTab by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(categories) {
        if (selectedTab >= categories.size) {
            selectedTab = 0
        }
    }

//    val userRole = SessionManager.userRole
    val userId = SessionManager.userDetails?.userId ?: ""
    val department = SessionManager.userDetails?.department ?: ""

    Scaffold(
        topBar = {

            Column {

                CommonTopBar(
                    title = "Smart Attendance",
                    navController = navController
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2196F3))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(modifier = Modifier.weight(1f)) {

                        Text(
                            "Welcome, $userId",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Department: $department",
                            color = Color.White
                        )
                    }

//                    if (userRole == "ROLE_STUDENT") {
//                        BluetoothStatusIndicator()
//                    }
                }

                if (categories.isNotEmpty()) {
                    DynamicTabsSection(
                        categories = categories,
                        selectedTab = selectedTab
                    ) {
                        selectedTab = it
                    }
                }
            }
        }

    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

//            if (userRole == "ROLE_STUDENT") {
//
//                StudentQuickActions(navController)
//
//                Spacer(modifier = Modifier.height(20.dp))
//            }

            val selectedCategory = categories.getOrNull(selectedTab)

            val tabActions = groupedActions[selectedCategory] ?: emptyList()

            PermissionActionGrid(tabActions, navController)

        }
    }
}

@Composable
fun DynamicTabsSection(
    categories: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2CA0EE))
            .padding(vertical = 6.dp),

        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {

        itemsIndexed(categories) { index, category ->

            ManagementTabCompact(
                title = category,
                isSelected = selectedTab == index
            ) {
                onTabSelected(index)
            }

        }
    }
}

@Composable
fun ManagementTabCompact(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(25.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable { onClick() }   // 🔴 THIS WAS MISSING
            .padding(horizontal = 18.dp, vertical = 10.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = title,
            color = if (isSelected) Color(0xFF2196F3) else Color.White,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun PermissionActionGrid(
    actions: List<UiPermissionAction>,
    navController: NavHostController
) {

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        items(actions) { action ->

            ActionCard(
                title = action.title,
                icon = action.icon
            ) {
                navController.navigate(action.route)
            }

        }
    }
}

@Composable
fun StudentQuickActions(navController: NavHostController) {

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        androidx.compose.material3.Button(
            onClick = { navController.navigate("schedule") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📅 View Schedule")
        }

        androidx.compose.material3.Button(
            onClick = { navController.navigate("attendance_summary") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📊 Attendance History")
        }
    }
}