package app.paisapilot.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.data.local.entity.Account
import app.paisapilot.data.local.entity.CreditCard
import app.paisapilot.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class WalletUiState(
    val loading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val cards: List<CreditCard> = emptyList(),
) {
    val cashMinor: Long get() = accounts.filter { it.type.isCash }.sumOf { it.balanceMinor }
    val bankMinor: Long get() = accounts.filter { it.type.liquid && !it.type.isCash }.sumOf { it.balanceMinor }
    val lockedMinor: Long get() = accounts.filter { !it.type.liquid }.sumOf { it.balanceMinor }
    val cardLimitMinor: Long get() = cards.sumOf { it.limitMinor }
    val cardOutstandingMinor: Long get() = cards.sumOf { it.outstandingMinor }
}

@HiltViewModel
class WalletViewModel @Inject constructor(wallet: WalletRepository) : ViewModel() {
    val state: StateFlow<WalletUiState> = combine(wallet.accounts, wallet.cards) { accounts, cards ->
        WalletUiState(loading = false, accounts = accounts, cards = cards)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalletUiState())
}
