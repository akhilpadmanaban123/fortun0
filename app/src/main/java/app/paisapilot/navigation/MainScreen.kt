package app.paisapilot.navigation

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The signed-in app: bottom navigation around the navigation graph. */
@Composable
fun MainScreen() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val topLevel = TopLevel.entries.any { it.route == route }

    NotificationPermissionPrompt()
    OpenRequestedScreen(nav)

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

/** Opens the screen a tapped notification asked for, once, now that the main screen is showing. */
@Composable
private fun OpenRequestedScreen(nav: NavHostController, viewModel: PendingNavigationViewModel = hiltViewModel()) {
    val requested by viewModel.pending.route.collectAsStateWithLifecycle()
    LaunchedEffect(requested) {
        val route = requested ?: return@LaunchedEffect
        viewModel.pending.consume()
        if (TopLevel.entries.any { it.route == route }) nav.switchTab(route) else nav.navigate(route) { launchSingleTop = true }
    }
}

@HiltViewModel
class PendingNavigationViewModel @Inject constructor(val pending: PendingNavigation) : ViewModel()

/** Switches bottom-bar tab, keeping each tab's scroll position and a single copy on the stack. */
fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
