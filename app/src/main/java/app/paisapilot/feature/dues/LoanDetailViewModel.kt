package app.paisapilot.feature.dues

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.finance.LoanStatus
import app.paisapilot.core.finance.PayoffIdea
import app.paisapilot.core.finance.PayoffIdeas
import app.paisapilot.core.finance.status
import app.paisapilot.data.repository.LoanRepository
import app.paisapilot.data.repository.WalletRepository
import app.paisapilot.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoanDetailUiState(
    val status: LoanStatus? = null,
    /** Name of the account EMIs are debited from, when one is linked. */
    val accountName: String? = null,
    /** What the extra payments have bought, compared with the loan's original schedule. */
    val emisSaved: Int = 0,
    val interestSavedMinor: Long = 0,
    /** Worked examples of paying extra, with what each would save. */
    val ideas: List<PayoffIdea> = emptyList(),
)

@HiltViewModel
class LoanDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val loans: LoanRepository,
    wallet: WalletRepository,
) : ViewModel() {

    private val loanId: String = checkNotNull(savedState[Routes.ARG_ID])

    val state: StateFlow<LoanDetailUiState> =
        combine(loans.observe(loanId), loans.observePrepayments(loanId), wallet.accounts) { loan, prepayments, accounts ->
            val status = loan?.status(prepayments)
            val original = loan?.status()
            LoanDetailUiState(
                status = status,
                accountName = accounts.firstOrNull { it.id == loan?.accountId }?.name,
                emisSaved = if (status != null && original != null) original.totalInstallments - status.totalInstallments else 0,
                interestSavedMinor = if (status != null && original != null) original.totalInterestMinor - status.totalInterestMinor else 0,
                ideas = status?.let(PayoffIdeas::forLoan).orEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoanDetailUiState())

    fun payNext() {
        viewModelScope.launch { loans.payNextInstallment(loanId) }
    }

    fun prepay(amountMinor: Long) {
        viewModelScope.launch { loans.prepay(loanId, amountMinor) }
    }

    fun removePrepayment(id: String) {
        viewModelScope.launch { loans.removePrepayment(id) }
    }
}
