package com.mckv.attendance.ui.components.common

// CommonTopBar.kt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.widget.Toast
import androidx.compose.foundation.background
import com.mckv.attendance.utils.logoutUser
import androidx.compose.foundation.layout.*

data class DropdownMenuItem(
    val text: String,
    val onClick: () -> Unit,
    val leadingIcon: ImageVector,
    val iconTint: Color = Color.White,
    val textColor: Color = Color.White,
    val isDestructive: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBar(
    title: String,
    navController: NavController? = null,
    dropdownItems: List<DropdownMenuItem> = emptyList(),
    containerColor: Color = Color(0xFF1976D2),
    titleColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Default dropdown items if none provided
    val defaultDropdownItems = remember {
        listOf(
            DropdownMenuItem(
                text = "Profile",
                onClick = {
                    Toast.makeText(context, "Profile Info", Toast.LENGTH_SHORT).show()
                    // Add navigation logic here if needed
                    // navController?.navigate("profile_route")
                },
                leadingIcon = Icons.Default.Person,
                iconTint = Color(0xFF64FFDA)
            ),
            DropdownMenuItem(
                text = "Logout",
                onClick = {
                    logoutUser(context, navController)
                },
                leadingIcon = Icons.AutoMirrored.Filled.ExitToApp,
                iconTint = Color(0xFFFF6B6B),
                textColor = Color(0xFFFF6B6B),
                isDestructive = true
            )
        )
    }

    val menuItems = if (dropdownItems.isEmpty()) defaultDropdownItems else dropdownItems

    TopAppBar(
        title = {
            Text(
                text = title,
                color = titleColor,
                fontWeight = FontWeight.Medium
            )
        },
        actions = {
            // Profile Icon with Dropdown Menu
            Box {
                IconButton(
                    onClick = { showDropdownMenu = true }
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = titleColor
                    )
                }

                // Dropdown Menu with Attractive Styling
                DropdownMenu(
                    expanded = showDropdownMenu,
                    onDismissRequest = { showDropdownMenu = false },
                    modifier = Modifier
                        .background(
                            color = containerColor,
                            shape = RectangleShape
                        )
                ) {
                    menuItems.forEachIndexed { index, item ->
                        // Add separator between items (except after last item)
                        if (index > 0) {
                            Spacer(modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .padding(horizontal = 8.dp)
                                .background(Color(0xFF64FFDA).copy(alpha = 0.8f))
                            )
                        }

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = item.text,
                                    color = item.textColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            },
                            onClick = {
                                showDropdownMenu = false
                                item.onClick()
                            },
                            leadingIcon = {
                                Icon(
                                    item.leadingIcon,
                                    contentDescription = item.text,
                                    tint = item.iconTint
                                )
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = titleColor,
            actionIconContentColor = titleColor
        ),
        modifier = modifier
    )
}
