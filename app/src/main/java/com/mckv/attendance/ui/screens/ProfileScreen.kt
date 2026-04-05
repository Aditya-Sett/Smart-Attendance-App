package com.mckv.attendance.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.lazy.LazyColumn

import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.ui.components.common.CommonTopBar
import com.mckv.attendance.ui.components.common.ProfileItem
import com.mckv.attendance.ui.components.common.SectionTitle
import com.mckv.attendance.ui.components.cards.ModernProfileCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {

    val user = SessionManager.userDetails

    if (user == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("User not logged in", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val isStudent = user.role.any { it.contains("STUDENT", true) }

    // ✅ Reactive route detection
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "Profile",
                navController = navController,
                currentRoute = currentRoute
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // 🔹 Profile Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = user.username ?: "Unknown User",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = user.email ?: "No Email",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // 🔹 Basic Info Section
            item {
                SectionTitle("Basic Information")
            }

            item {
                ModernProfileCard {
                    ProfileItem("User ID", user.userId)
                    ProfileItem("Department", user.department)
                    ProfileItem("Contact", user.contact)
                }
            }

            // 🔹 Student Section
            if (isStudent && user.studentProfile != null) {

                item {
                    SectionTitle("Student Details")
                }

                item {
                    ModernProfileCard(
                        background = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        ProfileItem("Admission Year", user.studentProfile.admissionYear)
                        ProfileItem("College Roll", user.studentProfile.collegeRoll)
                        ProfileItem("Academic Year", user.studentProfile.academicYear)
                        ProfileItem(
                            "Semester",
                            user.studentProfile.semester?.toString() ?: "N/A"
                        )
                    }
                }
            }

            // 🔹 Bottom Spacer (prevents cutoff on some devices)
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}