package app.paisapilot.feature.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.feature.auth.AuthScreen
import app.paisapilot.feature.auth.VerifyEmailScreen
import app.paisapilot.feature.intro.IntroScreen
import app.paisapilot.feature.lock.LockScreen
import app.paisapilot.feature.profile.ProfileScreen
import app.paisapilot.navigation.MainScreen

/** Decides between sign-in, onboarding, the lock screen and the app itself. */
@Composable
fun AppRoot(viewModel: RootViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val current = state) {
            RootState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            RootState.Intro -> IntroScreen(onFinished = viewModel::finishIntro)
            RootState.Auth -> AuthScreen()
            is RootState.VerifyEmail -> VerifyEmailScreen(email = current.email)
            is RootState.Onboarding -> ProfileScreen(onboarding = true, suggestedName = current.suggestedName)
            // The app stays composed underneath the lock so unlocking returns to the same place.
            RootState.Locked, RootState.Main -> Box(Modifier.fillMaxSize()) {
                MainScreen()
                if (current == RootState.Locked) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        LockScreen(onUnlocked = viewModel::unlock)
                    }
                }
            }
        }
    }
}
