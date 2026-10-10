package app.paisapilot.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.finance.HealthScore
import app.paisapilot.core.model.TxnType
import app.paisapilot.core.util.toLocalDate
import app.paisapilot.data.local.entity.Txn
import app.paisapilot.data.repository.TxnRepository
import app.paisapilot.domain.ObserveOverview
import app.paisapilot.domain.Overview
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Income and spending totals for one calendar month. */
data class MonthTotal(val month: YearMonth, val incomeMinor: Long, val spentMinor: Long)

data class HomeUiState(
    val overview: Overview? = null,
    val recent: List<Txn> = emptyList(),
    val healthScore: Int = 0,
    /** Spending on each day of the current month; index 0 is the 1st. */
    val dailySpend: List<Long> = emptyList(),
    /** The last [HomeViewModel.TREND_MONTHS] months, oldest first, ending with the current one. */
    val trend: List<MonthTotal> = emptyList(),
) {
    /** Nothing has been added yet, so the dashboard shows a getting-started guide instead. */
    val isEmpty: Boolean
        get() = overview != null && overview.accounts.isEmpty() && overview.loans.isEmpty() &&
            overview.bills.isEmpty() && overview.cards.isEmpty() && recent.isEmpty()

    /** Original amount borrowed across loans that are still running, for the payoff bar. */
    val loanPrincipalMinor: Long
        get() = overview?.loans.orEmpty().filter { !it.closed }.sumOf { it.principalMinor }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeOverview: ObserveOverview,
    txns: TxnRepository,
) : ViewModel() {

    private val thisMonth = YearMonth.now()
    private val trendMonths = (TREND_MONTHS - 1 downTo 0).map { thisMonth.minusMonths(it.toLong()) }

    val state: StateFlow<HomeUiState> = combine(
        observeOverview(),
        txns.observeRecent(RECENT_COUNT),
        txns.observeMonths(trendMonths.first(), thisMonth),
    ) { overview, recent, history ->
        val byMonth = history.groupBy { YearMonth.from(it.dateEpochDay.toLocalDate()) }
        val daily = LongArray(thisMonth.lengthOfMonth())
        overview.monthTxns.filter { it.type == TxnType.EXPENSE }.forEach {
            daily[it.dateEpochDay.toLocalDate().dayOfMonth - 1] += it.amountMinor
        }
        HomeUiState(
            overview = overview,
            recent = recent,
            healthScore = HealthScore.evaluate(
                overview.snapshot, overview.profile.totalIncomeMinor, overview.bills,
            ).score,
            dailySpend = daily.toList(),
            trend = trendMonths.map { month ->
                val rows = byMonth[month].orEmpty()
                MonthTotal(
                    month = month,
                    incomeMinor = rows.filter { it.type == TxnType.INCOME }.sumOf { it.amountMinor },
                    spentMinor = rows.filter { it.type == TxnType.EXPENSE }.sumOf { it.amountMinor },
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        const val TREND_MONTHS = 6
        private const val RECENT_COUNT = 5
    }
}
