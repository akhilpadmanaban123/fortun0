package app.paisapilot.feature.dues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.finance.LoanStatus
import app.paisapilot.core.finance.status
import app.paisapilot.data.local.entity.Bill
import app.paisapilot.data.repository.BillRepository
import app.paisapilot.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DuesUiState(
    val loading: Boolean = true,
    val loans: List<LoanStatus> = emptyList(),
    val bills: List<Bill> = emptyList(),
) {
    val monthlyEmiMinor: Long get() = loans.filter { it.isActive }.sumOf { it.loan.emiMinor }
    val outstandingMinor: Long get() = loans.sumOf { it.outstandingMinor }
    val monthlyBillsMinor: Long
        get() = bills.filter { it.active && it.frequency.months > 0 }.sumOf { it.amountMinor / it.frequency.months }
}

@HiltViewModel
class DuesViewModel @Inject constructor(
    loans: LoanRepository,
    private val bills: BillRepository,
) : ViewModel() {

    val state: StateFlow<DuesUiState> = combine(loans.loans, loans.prepayments, bills.bills) { loanList, prepayments, billList ->
        DuesUiState(
            loading = false,
            // Running loans first, soonest EMI at the top; closed loans sink to the bottom.
            loans = loanList.map { it.status(prepayments) }
                .sortedWith(compareBy({ !it.isActive }, { it.nextDueDate })),
            bills = billList,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DuesUiState())

    fun markBillPaid(id: String) {
        viewModelScope.launch { bills.markPaid(id) }
    }
}
