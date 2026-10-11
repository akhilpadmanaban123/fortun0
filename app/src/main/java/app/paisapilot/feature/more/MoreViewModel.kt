package app.paisapilot.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.finance.MoneyPersona
import app.paisapilot.core.finance.MoneyPersonas
import app.paisapilot.core.premium.Entitlements
import app.paisapilot.data.prefs.UserProfile
import app.paisapilot.domain.ObserveOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MoreUiState(val profile: UserProfile, val persona: MoneyPersona, val isPremium: Boolean)

@HiltViewModel
class MoreViewModel @Inject constructor(observeOverview: ObserveOverview, entitlements: Entitlements) : ViewModel() {
    val state: StateFlow<MoreUiState?> = combine(observeOverview(), entitlements.isPremium) { o, premium ->
        // Real EMIs from tracked loans win over the rough figure given during sign-up.
        val emi = o.snapshot.monthlyEmiMinor.takeIf { it > 0 } ?: o.profile.emiEstimateMinor
        MoreUiState(
            profile = o.profile,
            persona = MoneyPersonas.of(o.profile.totalIncomeMinor, emi, o.profile.age, o.profile.confidence),
            isPremium = premium,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
