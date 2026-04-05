package com.mckv.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mckv.attendance.data.local.PermissionManager

@Composable
fun ManageRoleScreen(navController: NavHostController) {

    val permissions = PermissionManager.getPermissions()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        if ("CREATE_ROLE" in permissions) {

            Button(
                onClick = { navController.navigate("create_role") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Role")
            }
        }

        if ("UPDATE_ROLE" in permissions) {

            Button(
                onClick = { navController.navigate("update_role") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Role")
            }
        }

        if ("DELETE_ROLE" in permissions) {

            Button(
                onClick = { navController.navigate("delete_role") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Role")
            }
        }

        if ("MANAGE_ROLE" in permissions) {

            Button(
                onClick = { navController.navigate("view_roles") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Roles")
            }
        }
    }
}