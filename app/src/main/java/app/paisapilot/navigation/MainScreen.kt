package app.paisapilot.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/** The signed-in app: bottom navigation around the navigation graph. */
@Composable
fun MainScreen() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val topLevel = TopLevel.entries.any { it.route == route }

    NotificationPermissionRequest()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (topLevel) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    TopLevel.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        AppNavHost(nav, Modifier.padding(padding))
    }
}

/** Switches bottom-bar tab, keeping each tab's scroll position and a single copy on the stack. */
fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Android 13+ needs the user's permission before reminders can be shown. */
@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
