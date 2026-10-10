package app.paisapilot.feature.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.model.AccountType
import app.paisapilot.core.util.Money
import app.paisapilot.data.local.entity.Account
import app.paisapilot.data.repository.WalletRepository
import app.paisapilot.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountForm(
    val isEdit: Boolean = false,
    val type: AccountType = AccountType.SAVINGS,
    val name: String = "",
    val institution: String = "",
    val last4: String = "",
    val balance: String = "",
    val purpose: String = "",
    val nameError: String? = null,
)

@HiltViewModel
class AccountEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wallet: WalletRepository,
) : ViewModel() {

    private val accountId: String? = savedState[Routes.ARG_ID]
    private var original: Account? = null

    private val _form = MutableStateFlow(AccountForm(isEdit = accountId != null))
    val form: StateFlow<AccountForm> = _form

    init {
        if (accountId != null) {
            viewModelScope.launch {
                val account = wallet.account(accountId) ?: return@launch
                original = account
                _form.value = AccountForm(
                    isEdit = true,
                    type = account.type,
                    name = account.name,
                    institution = account.institution,
                    last4 = account.last4,
                    balance = Money.toInput(account.balanceMinor),
                    purpose = account.purpose,
                )
            }
        }
    }

    fun update(change: AccountForm.() -> AccountForm) = _form.update { it.change().copy(nameError = null) }

    /** Cash needs no bank details, so it gets a sensible name straight away. */
    fun onType(type: AccountType) = update {
        copy(type = type, name = if (type == AccountType.CASH && name.isBlank()) "Cash in hand" else name)
    }

    fun save(onSaved: () -> Unit) {
        val f = _form.value
        if (f.name.isBlank()) {
            _form.update { it.copy(nameError = "Give this account a name.") }
            return
        }
        val base = original ?: Account(name = "", type = f.type)
        viewModelScope.launch {
            wallet.save(
                base.copy(
                    name = f.name.trim(),
                    type = f.type,
                    institution = f.institution.trim(),
                    last4 = f.last4,
                    balanceMinor = Money.parse(f.balance) ?: 0,
                    purpose = f.purpose.trim(),
                )
            )
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = accountId ?: return
        viewModelScope.launch {
            wallet.deleteAccount(id)
            onDeleted()
        }
    }
}
