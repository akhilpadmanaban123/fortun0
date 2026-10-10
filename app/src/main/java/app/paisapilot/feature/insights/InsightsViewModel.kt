package app.paisapilot.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.finance.HealthReport
import app.paisapilot.core.finance.HealthScore
import app.paisapilot.core.finance.MoneySnapshot
import app.paisapilot.domain.ObserveOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class InsightsUiState(val report: HealthReport, val snapshot: MoneySnapshot)

@HiltViewModel
class InsightsViewModel @Inject constructor(observeOverview: ObserveOverview) : ViewModel() {
    val state: StateFlow<InsightsUiState?> = observeOverview().map { o ->
        InsightsUiState(
            report = HealthScore.evaluate(o.snapshot, o.profile.totalIncomeMinor, o.bills),
            snapshot = o.snapshot,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
