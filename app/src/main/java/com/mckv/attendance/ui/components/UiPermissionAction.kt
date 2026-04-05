package com.mckv.attendance.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

data class UiPermissionAction(
    val permission: String,
    val category: String,
    val title: String,
    val icon: ImageVector,
    val route: String
)
