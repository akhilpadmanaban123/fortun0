package app.paisapilot.feature.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.premium.Entitlements
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PremiumViewModel @Inject constructor(private val entitlements: Entitlements) : ViewModel() {

    val isPremium: StateFlow<Boolean> =
        entitlements.isPremium.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Only debug builds offer the test unlock; see [Entitlements]. */
    val testUnlockAvailable: Boolean get() = entitlements.testUnlockAvailable

    fun setTestUnlock(enabled: Boolean) {
        viewModelScope.launch { entitlements.setTestUnlock(enabled) }
    }
}
