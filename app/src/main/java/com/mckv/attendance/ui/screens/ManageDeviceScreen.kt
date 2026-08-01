package com.mckv.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.remote.RetrofitClient
import com.mckv.attendance.data.remote.dto.request.DeviceActionRequestBody
import com.mckv.attendance.data.remote.dto.request.DeviceRequestItem
import com.mckv.attendance.ui.components.common.CommonTopBar
import kotlinx.coroutines.launch

enum class ActionState { PENDING, APPROVED, REJECTED }

data class TableColumnConfig(
    val headerName: String,
    val width: Dp,
    val getValue: (DeviceRequestItem) -> String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDeviceScreen(navController: NavHostController) {

    val scope = rememberCoroutineScope()
    val token = SessionManager.authToken

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var requestList by remember { mutableStateOf<List<DeviceRequestItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    val localActionStates = remember { mutableStateMapOf<String, ActionState>() }
    var pendingDialogData by remember { mutableStateOf<Triple<DeviceRequestItem, Boolean, String>?>(null) }

    // Column Config List - Define fixed Dp widths for scroll stability
    val columns = remember {
        listOf(
            TableColumnConfig("Name", width = 130.dp) { it.username },
            TableColumnConfig("Roll No", width = 170.dp) { it.rollNo },
            TableColumnConfig("Semester", width = 90.dp) { it.semester },
            // Add or uncomment future columns with fixed widths seamlessly:
            // TableColumnConfig("Department", width = 110.dp) { it.department }
        )
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                val response = RetrofitClient.authInstance.getPendingDeviceRequests(
                    page = 0, size = 10,
                    token = "Bearer $token"
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    requestList = response.body()?.data?.content ?: emptyList()
                } else {
                    errorMessage = response.body()?.message ?: "Failed to load device requests."
                }
            } catch (e: Exception) {
                errorMessage = "Network error: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "Manage Device",
                navController = navController
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = { Text("Search by name or roll number...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
                    }
                }

                else -> {
                    val filteredList = requestList.filter { item ->
                        item.username.contains(searchQuery, ignoreCase = true) ||
                                item.rollNo.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No pending requests found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        DeviceRequestTable(
                            items = filteredList,
                            columns = columns,
                            localStates = localActionStates,
                            onApproveClick = { item ->
                                pendingDialogData = Triple(
                                    item,
                                    true,
                                    "Do you want to Approve the Device Changing Request of ${item.username} (${item.rollNo})?"
                                )
                            },
                            onRejectClick = { item ->
                                pendingDialogData = Triple(
                                    item,
                                    false,
                                    "Do you want to Reject the Device Changing Request of ${item.username} (${item.rollNo})?"
                                )
                            }
                        )
                    }
                }
            }
        }

        // Confirmation Dialog
        pendingDialogData?.let { (item, isApprove, dialogText) ->
            AlertDialog(
                onDismissRequest = { pendingDialogData = null },
                title = { Text(text = if (isApprove) "Approve Request" else "Reject Request") },
                text = { Text(dialogText) },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isApprove) Color(0xFF4CAF50) else Color(0xFFF44336)
                        ),
                        onClick = {
                            val currentItem = item
                            pendingDialogData = null
                            scope.launch {
                                try {
                                    val body = DeviceActionRequestBody(studentUserId = currentItem.userId)
                                    val response = if (isApprove) {
                                        RetrofitClient.authInstance.approveDeviceRequest(token = "Bearer $token",body)
                                    } else {
                                        RetrofitClient.authInstance.rejectDeviceRequest(token = "Bearer $token",body)
                                    }

                                    if (response.isSuccessful && response.body()?.success == true) {
                                        localActionStates[currentItem.userId] =
                                            if (isApprove) ActionState.APPROVED else ActionState.REJECTED
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                    ) {
                        Text("Yes", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDialogData = null }) {
                        Text("No")
                    }
                }
            )
        }
    }
}

@Composable
fun DeviceRequestTable(
    items: List<DeviceRequestItem>,
    columns: List<TableColumnConfig>,
    localStates: Map<String, ActionState>,
    onApproveClick: (DeviceRequestItem) -> Unit,
    onRejectClick: (DeviceRequestItem) -> Unit
) {
    val horizontalScrollState = rememberScrollState()
    val actionColumnWidth = 110.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Horizontal Scroll surrounds LazyColumn for strict 2D layout alignment
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxHeight()
            ) {
                // Fixed Header Row
                item {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF2196F3))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        columns.forEach { col ->
                            Text(
                                text = col.headerName,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.width(col.width).padding(horizontal = 4.dp)
                            )
                        }
                        Text(
                            text = "Action",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(actionColumnWidth)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                // Data Rows
                items(items) { item ->
                    val actionState = localStates[item.userId] ?: ActionState.PENDING

                    Row(
                        modifier = Modifier
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        columns.forEach { col ->
                            Text(
                                text = col.getValue(item),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(col.width).padding(horizontal = 4.dp)
                            )
                        }

                        // Action Column
                        Box(
                            modifier = Modifier.width(actionColumnWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            when (actionState) {
                                ActionState.PENDING -> {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { onApproveClick(item) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Approve",
                                                tint = Color(0xFF4CAF50)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRejectClick(item) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Reject",
                                                tint = Color(0xFFF44336)
                                            )
                                        }
                                    }
                                }

                                ActionState.APPROVED -> {
                                    Text(
                                        text = "Approved",
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                ActionState.REJECTED -> {
                                    Text(
                                        text = "Rejected",
                                        color = Color(0xFFF44336),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}