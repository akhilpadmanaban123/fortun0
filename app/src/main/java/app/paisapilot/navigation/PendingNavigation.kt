package app.paisapilot.navigation

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A screen the app has been asked to open from outside, e.g. by tapping a notification.
 * The request is held here until the main screen is showing (after sign-in and the app
 * lock), which then navigates and clears it.
 */
@Singleton
class PendingNavigation @Inject constructor() {
    private val _route = MutableStateFlow<String?>(null)
    val route: StateFlow<String?> = _route

    fun request(route: String?) {
        // Only screens that make sense as an entry point may be opened this way.
        if (route in ALLOWED) _route.value = route
    }

    fun consume() {
        _route.value = null
    }

    companion object {
        /** Intent extra carrying the route. */
        const val EXTRA_ROUTE = "open_route"
        private val ALLOWED = setOf(Routes.DAILY, Routes.DUES)
    }
}
