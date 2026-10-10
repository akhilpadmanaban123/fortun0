package app.paisapilot.feature.dues

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.model.BillCategory
import app.paisapilot.core.model.BillFrequency
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.toLocalDate
import app.paisapilot.data.local.entity.Account
import app.paisapilot.data.local.entity.Bill
import app.paisapilot.data.repository.BillRepository
import app.paisapilot.data.repository.WalletRepository
import app.paisapilot.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillForm(
    val isEdit: Boolean = false,
    val name: String = "",
    val category: BillCategory = BillCategory.ELECTRICITY,
    val amount: String = "",
    val frequency: BillFrequency = BillFrequency.MONTHLY,
    val nextDue: LocalDate = LocalDate.now(),
    val accountId: String? = null,
    val autopay: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
)

@HiltViewModel
class BillEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val bills: BillRepository,
    wallet: WalletRepository,
) : ViewModel() {

    private val billId: String? = savedState[Routes.ARG_ID]
    private var original: Bill? = null

    private val _form = MutableStateFlow(BillForm(isEdit = billId != null))
    val form: StateFlow<BillForm> = _form

    val accounts: StateFlow<List<Account>> =
        wallet.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (billId != null) {
            viewModelScope.launch {
                val bill = bills.bill(billId) ?: return@launch
                original = bill
                _form.value = BillForm(
                    isEdit = true,
                    name = bill.name,
                    category = bill.category,
                    amount = Money.toInput(bill.amountMinor),
                    frequency = bill.frequency,
                    nextDue = bill.nextDueEpochDay.toLocalDate(),
                    accountId = bill.accountId,
                    autopay = bill.autopay,
                )
            }
        }
    }

    fun update(change: BillForm.() -> BillForm) = _form.update { it.change().copy(errors = emptyMap()) }

    /** Picking a category names the bill for the user if they have not typed a name yet. */
    fun onCategory(category: BillCategory) = update {
        val untouchedName = name.isBlank() || BillCategory.entries.any { it.label == name }
        copy(category = category, name = if (untouchedName && category != BillCategory.OTHER) category.label else name)
    }

    fun save(onSaved: () -> Unit) {
        val f = _form.value
        val amount = Money.parse(f.amount) ?: 0
        val errors = buildMap {
            if (f.name.isBlank()) put("name", "Give this bill a name.")
            if (amount <= 0) put("amount", "Enter the amount.")
        }
        if (errors.isNotEmpty()) {
            _form.update { it.copy(errors = errors) }
            return
        }
        val base = original ?: Bill(name = "", category = f.category, amountMinor = 0, nextDueEpochDay = 0)
        viewModelScope.launch {
            bills.save(
                base.copy(
                    name = f.name.trim(),
                    category = f.category,
                    amountMinor = amount,
                    frequency = f.frequency,
                    nextDueEpochDay = f.nextDue.toEpochDay(),
                    accountId = f.accountId,
                    autopay = f.autopay,
                    active = true,
                )
            )
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = billId ?: return
        viewModelScope.launch {
            bills.delete(id)
            onDeleted()
        }
    }
}
