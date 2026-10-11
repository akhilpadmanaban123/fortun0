package app.paisapilot.feature.dues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.finance.LoanStatus
import app.paisapilot.core.finance.PayoffIdeas
import app.paisapilot.core.finance.status
import app.paisapilot.data.local.entity.Bill
import app.paisapilot.data.repository.BillRepository
import app.paisapilot.data.repository.LoanRepository
import app.paisapilot.data.repository.WalletRepository
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
    val accountNames: Map<String, String> = emptyMap(),
    /** The loan where extra payments save the most, when there is a clear choice. */
    val payFirst: LoanStatus? = null,
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
    wallet: WalletRepository,
) : ViewModel() {

    val state: StateFlow<DuesUiState> = combine(loans.loans, loans.prepayments, bills.bills, wallet.accounts) { loanList, prepayments, billList, accounts ->
        val statuses = loanList.map { it.status(prepayments) }
        DuesUiState(
            loading = false,
            // Running loans first, soonest EMI at the top; closed loans sink to the bottom.
            loans = statuses.sortedWith(compareBy({ !it.isActive }, { it.nextDueDate })),
            bills = billList,
            accountNames = accounts.associate { it.id to it.name },
            payFirst = PayoffIdeas.payFirst(statuses),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DuesUiState())

    fun markBillPaid(id: String) {
        viewModelScope.launch { bills.markPaid(id) }
    }
}
