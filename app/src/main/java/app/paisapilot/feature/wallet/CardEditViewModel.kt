package app.paisapilot.feature.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.util.Money
import app.paisapilot.data.local.entity.CreditCard
import app.paisapilot.data.repository.WalletRepository
import app.paisapilot.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CardForm(
    val isEdit: Boolean = false,
    val name: String = "",
    val issuer: String = "",
    val last4: String = "",
    val limit: String = "",
    val outstanding: String = "",
    val statementDay: Int = 1,
    val dueDay: Int = 20,
    val errors: Map<String, String> = emptyMap(),
)

@HiltViewModel
class CardEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wallet: WalletRepository,
) : ViewModel() {

    private val cardId: String? = savedState[Routes.ARG_ID]
    private var original: CreditCard? = null

    private val _form = MutableStateFlow(CardForm(isEdit = cardId != null))
    val form: StateFlow<CardForm> = _form

    init {
        if (cardId != null) {
            viewModelScope.launch {
                val card = wallet.card(cardId) ?: return@launch
                original = card
                _form.value = CardForm(
                    isEdit = true,
                    name = card.name,
                    issuer = card.issuer,
                    last4 = card.last4,
                    limit = Money.toInput(card.limitMinor),
                    outstanding = Money.toInput(card.outstandingMinor),
                    statementDay = card.statementDay,
                    dueDay = card.dueDay,
                )
            }
        }
    }

    fun update(change: CardForm.() -> CardForm) = _form.update { it.change().copy(errors = emptyMap()) }

    fun save(onSaved: () -> Unit) {
        val f = _form.value
        val limit = Money.parse(f.limit) ?: 0
        val errors = buildMap {
            if (f.name.isBlank()) put("name", "Give this card a name.")
            if (limit <= 0) put("limit", "Enter the card's total credit limit.")
        }
        if (errors.isNotEmpty()) {
            _form.update { it.copy(errors = errors) }
            return
        }
        val base = original ?: CreditCard(name = "")
        viewModelScope.launch {
            wallet.save(
                base.copy(
                    name = f.name.trim(),
                    issuer = f.issuer.trim(),
                    last4 = f.last4,
                    limitMinor = limit,
                    outstandingMinor = Money.parse(f.outstanding) ?: 0,
                    statementDay = f.statementDay,
                    dueDay = f.dueDay,
                )
            )
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = cardId ?: return
        viewModelScope.launch {
            wallet.deleteCard(id)
            onDeleted()
        }
    }
}
