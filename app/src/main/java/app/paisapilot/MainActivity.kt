package app.paisapilot

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import app.paisapilot.core.designsystem.PaisaPilotTheme
import app.paisapilot.feature.root.AppRoot
import app.paisapilot.navigation.PendingNavigation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** FragmentActivity (rather than ComponentActivity) because BiometricPrompt requires it. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var pendingNavigation: PendingNavigation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on a fresh launch; after a rotation the same intent would otherwise re-open the screen.
        if (savedInstanceState == null) pendingNavigation.request(intent?.getStringExtra(PendingNavigation.EXTRA_ROUTE))
        enableEdgeToEdge()
        setContent {
            PaisaPilotTheme { AppRoot() }
        }
    }

    /** A notification was tapped while the app was already open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingNavigation.request(intent.getStringExtra(PendingNavigation.EXTRA_ROUTE))
    }
}
