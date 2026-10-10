package app.paisapilot

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import app.paisapilot.core.designsystem.PaisaPilotTheme
import app.paisapilot.feature.root.AppRoot
import dagger.hilt.android.AndroidEntryPoint

/** FragmentActivity (rather than ComponentActivity) because BiometricPrompt requires it. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaisaPilotTheme { AppRoot() }
        }
    }
}
