package app.paisapilot.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.model.TxnType
import app.paisapilot.data.local.entity.Txn
import app.paisapilot.data.repository.TxnRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityUiState(
    val month: YearMonth = YearMonth.now(),
    val loading: Boolean = true,
    val txns: List<Txn> = emptyList(),
) {
    val incomeMinor: Long get() = txns.filter { it.type == TxnType.INCOME }.sumOf { it.amountMinor }
    val spentMinor: Long get() = txns.filter { it.type == TxnType.EXPENSE }.sumOf { it.amountMinor }
    val canGoForward: Boolean get() = month.isBefore(YearMonth.now())

    /** Newest day first; the DAO already orders the rows that way. */
    val byDay: Map<Long, List<Txn>> get() = txns.groupBy { it.dateEpochDay }
}

@HiltViewModel
class ActivityViewModel @Inject constructor(private val txns: TxnRepository) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ActivityUiState> = month
        .flatMapLatest { m -> txns.observeMonth(m).map { ActivityUiState(m, loading = false, txns = it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { if (it.isBefore(YearMonth.now())) it.plusMonths(1) else it }

    fun delete(id: String) {
        viewModelScope.launch { txns.delete(id) }
    }
}
