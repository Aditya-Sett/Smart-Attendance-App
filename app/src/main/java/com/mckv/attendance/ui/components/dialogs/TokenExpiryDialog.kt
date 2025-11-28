import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.mckv.attendance.data.local.TokenExpiryManager
import com.mckv.attendance.utils.logoutUser

@Composable
fun TokenExpiryDialog(
    navController: NavController,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // 🔹 IMMEDIATELY set dialog as showing
    LaunchedEffect(Unit) {
        TokenExpiryManager.setDialogShowing(true)
    }

    AlertDialog(
        onDismissRequest = {
            // 🔹 DON'T allow dismiss - force logout on any click
            TokenExpiryManager.setDialogShowing(false)
            logoutUser(context, navController)
        },
        title = {
            Text(
                text = "Session Expired",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text("Your session has expired. Please login again.")
        },
        confirmButton = {
            Button(
                onClick = {
                    TokenExpiryManager.setDialogShowing(false)
                    logoutUser(context, navController)
                }
            ) {
                Text("OK, Login Again")
            }
        }
    )
}