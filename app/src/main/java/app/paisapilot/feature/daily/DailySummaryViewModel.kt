package app.paisapilot.feature.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.finance.DailySummary
import app.paisapilot.core.finance.Feeling
import app.paisapilot.data.repository.CheckinRepository
import app.paisapilot.domain.ObserveOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One day in the "last 7 days" strip; [feeling] is null when the user did not answer. */
data class DayFeeling(val date: LocalDate, val feeling: Feeling?)

data class DailySummaryUiState(
    val summary: DailySummary,
    val todayFeeling: Feeling?,
    val week: List<DayFeeling>,
)

@HiltViewModel
class DailySummaryViewModel @Inject constructor(
    observeOverview: ObserveOverview,
    private val checkins: CheckinRepository,
) : ViewModel() {

    private val weekStart: LocalDate = LocalDate.now().minusDays(WEEK_DAYS - 1L)

    val state: StateFlow<DailySummaryUiState?> =
        combine(observeOverview(), checkins.observeSince(weekStart)) { overview, rows ->
            val summary = DailySummary.of(overview.snapshot, overview.monthTxns, overview.profile.totalIncomeMinor)
            val byDay = rows.associate { it.dayEpochDay to Feeling.entries.firstOrNull { f -> f.name == it.feeling } }
            DailySummaryUiState(
                summary = summary,
                todayFeeling = byDay[summary.date.toEpochDay()],
                week = (0 until WEEK_DAYS).map { weekStart.plusDays(it.toLong()) }
                    .map { DayFeeling(it, byDay[it.toEpochDay()]) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun answer(feeling: Feeling) {
        val summary = state.value?.summary ?: return
        viewModelScope.launch { checkins.save(summary, feeling) }
    }

    private companion object {
        const val WEEK_DAYS = 7
    }
}
