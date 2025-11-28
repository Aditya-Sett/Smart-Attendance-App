package com.mckv.attendance.utils


import TokenExpiryDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.mckv.attendance.data.local.SessionManager
import com.mckv.attendance.data.local.TokenExpiryManager


@Composable
fun UserInteractionHandler(navController: NavController) {
    var showExpiryDialog by remember { mutableStateOf(false) }

    // Check token expiry when composable first loads
    LaunchedEffect(Unit) {
        if (TokenExpiryManager.shouldShowExpiryDialog()) {
            showExpiryDialog = true
        }
    }

    // Function to immediately show dialog
    val showDialogImmediately = {
        if (TokenExpiryManager.shouldShowExpiryDialog()) {
            showExpiryDialog = true
        }
    }

    // Show expiry dialog if needed
    if (showExpiryDialog) {
        TokenExpiryDialog(
            navController = navController,
            onDismiss = {
                showExpiryDialog = false
                TokenExpiryManager.setDialogShowing(false)
            }
        )
    }

    // Make the function available via composition
    CompositionLocalProvider(
        LocalTokenExpiryHandler provides showDialogImmediately
    ) {}
}

val LocalTokenExpiryHandler = staticCompositionLocalOf<() -> Unit> { { } }

//@Composable
//fun UserInteractionHandler(
//    navController: NavController
//) {
//    var showExpiryDialog by remember { mutableStateOf(false) }
//
//    // Check token expiry when composable first loads
//    LaunchedEffect(Unit) {
//        val token = SessionManager.authToken
//        if (!token.isNullOrEmpty() && JwtUtils.isTokenExpired(token)) {
//            showExpiryDialog = true
//        }
//    }
//
//    // Show expiry dialog if needed
//    if (showExpiryDialog && TokenExpiryManager.shouldShowExpiryDialog()) {
//        TokenExpiryDialog(
//            navController = navController,
//            onDismiss = { showExpiryDialog = false }
//        )
//    }
//
//    // Return the interaction detection modifier
//    Modifier.interactionDetection {
//        // This will be called on user interactions
//        val token = SessionManager.authToken
//        if (!token.isNullOrEmpty() && JwtUtils.isTokenExpired(token)) {
//            showExpiryDialog = true
//        }
//    }
//}

// Extension function for interaction detection
//fun Modifier.interactionDetection(onInteraction: () -> Unit): Modifier = composed {
//    this.pointerInput(Unit) {
//        awaitPointerEventScope {
//            while (true) {
//                awaitPointerEvent().also { pointerEvent ->
//                    // Check for user interaction
//                    if (pointerEvent.changes.any { it.pressed }) {
//                        onInteraction()
//                    }
//                }
//            }
//        }
//    }
//}

// Enhanced interaction detection that BLOCKS navigation
fun Modifier.interactionDetection(): Modifier = composed {
    val showDialogImmediately = LocalTokenExpiryHandler.current

    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                // Check for ANY pointer press
                if (event.changes.any { it.pressed }) {
                    // If token expired, show dialog and CONSUME the event
                    if (TokenExpiryManager.isTokenExpired()) {
                        showDialogImmediately()
                        // 🔹 CRITICAL: Consume ALL events to prevent button clicks
                        event.changes.forEach { change ->
                            change.consume()
                        }
                    }
                }
            }
        }
    }
}