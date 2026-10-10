package app.paisapilot.feature.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.model.AccountType
import app.paisapilot.core.model.TxnCategory
import app.paisapilot.core.model.TxnType
import app.paisapilot.core.util.Money
import app.paisapilot.data.local.entity.Account
import app.paisapilot.data.local.entity.Txn
import app.paisapilot.data.repository.TxnRepository
import app.paisapilot.data.repository.WalletRepository
import app.paisapilot.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MoneyKind { BANK, CASH, CARD }

/** Somewhere money can come from or go to: a bank account, cash in hand or a credit card. */
data class MoneyOption(
    val name: String,
    val kind: MoneyKind,
    /** Balance for accounts, amount used for cards; shown under the name. */
    val detail: String = "",
    val accountId: String? = null,
    val cardId: String? = null,
) {
    val isCard: Boolean get() = kind == MoneyKind.CARD

    /** "Cash in hand" offered before the user has a cash account; one is created when it is used. */
    val isNewCash: Boolean get() = kind == MoneyKind.CASH && accountId == null

    val label: String get() = if (detail.isEmpty()) name else "$name · $detail"
}

data class TxnForm(
    val type: TxnType = TxnType.EXPENSE,
    val amount: String = "",
    val category: TxnCategory = TxnCategory.FOOD,
    val from: MoneyOption? = null,
    val to: MoneyOption? = null,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    /** Bank accounts and wallets first, then cash. */
    val accounts: List<MoneyOption> = emptyList(),
    val cards: List<MoneyOption> = emptyList(),
    val errors: Map<String, String> = emptyMap(),
) {
    /** Where an expense was paid from, or an income received. Only expenses can go on a card. */
    val sources: List<MoneyOption>
        get() = if (type == TxnType.EXPENSE) accounts + cards else accounts

    /** A transfer can also go to a credit card, which is how a card bill gets paid. */
    val destinations: List<MoneyOption>
        get() = accounts.filter { it != from } + cards.map { it.copy(name = "Pay ${it.name}") }
}

@HiltViewModel
class TxnAddViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val txns: TxnRepository,
    private val wallet: WalletRepository,
) : ViewModel() {

    private val initialType = savedState.get<String>(Routes.ARG_TYPE)
        ?.let { name -> TxnType.entries.firstOrNull { it.name == name } } ?: TxnType.EXPENSE
    private val payCardId: String? = savedState[Routes.ARG_CARD]

    private val _form = MutableStateFlow(TxnForm(type = initialType, category = TxnCategory.of(initialType).first()))
    val form: StateFlow<TxnForm> = _form

    init {
        viewModelScope.launch {
            val liquid = wallet.accounts.first().filter { it.type.liquid }
            val banks = liquid.filter { !it.type.isCash }
                .map { MoneyOption(it.name, MoneyKind.BANK, Money.format(it.balanceMinor), accountId = it.id) }
            val cash = liquid.filter { it.type.isCash }
                .map { MoneyOption(it.name, MoneyKind.CASH, Money.format(it.balanceMinor), accountId = it.id) }
                // Paying by cash should always be possible, even before a cash account exists.
                .ifEmpty { listOf(MoneyOption(NEW_CASH_NAME, MoneyKind.CASH, Money.format(0))) }
            val cardEntities = wallet.cards.first()
            val cards = cardEntities.map {
                MoneyOption(it.name, MoneyKind.CARD, "${Money.format(it.outstandingMinor)} used", cardId = it.id)
            }
            val payCard = cardEntities.firstOrNull { it.id == payCardId }
            _form.update { f ->
                f.copy(
                    accounts = banks + cash,
                    cards = cards,
                    // A card bill is paid from a bank account, so pre-pick one if there is exactly one.
                    from = if (payCard != null) banks.singleOrNull() else null,
                    to = cards.firstOrNull { it.cardId == payCardId }?.let { it.copy(name = "Pay ${it.name}") },
                    amount = payCard?.let { Money.toInput(it.outstandingMinor) } ?: f.amount,
                )
            }
        }
    }

    fun update(change: TxnForm.() -> TxnForm) = _form.update { it.change().copy(errors = emptyMap()) }

    fun onType(type: TxnType) = update {
        copy(
            type = type,
            category = TxnCategory.of(type).first(),
            // Income cannot land on a credit card.
            from = from?.takeUnless { it.isCard && type != TxnType.EXPENSE },
            to = null,
        )
    }

    fun save(onSaved: () -> Unit) {
        val f = _form.value
        val amount = Money.parse(f.amount) ?: 0
        val errors = buildMap {
            if (amount <= 0) put("amount", "Enter an amount.")
            if (f.from == null) {
                put(
                    "from",
                    when (f.type) {
                        TxnType.EXPENSE -> "Choose where you paid from: a bank account, cash or a card."
                        TxnType.INCOME -> "Choose where you received it: a bank account or cash."
                        TxnType.TRANSFER -> "Choose the account the money leaves."
                    },
                )
            }
            if (f.type == TxnType.TRANSFER && f.to == null) put("to", "Choose where the money goes.")
        }
        if (errors.isNotEmpty()) {
            _form.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            val from = f.from?.let { resolve(it) }
            val to = f.to?.let { resolve(it) }
            val txn = when (f.type) {
                TxnType.EXPENSE, TxnType.INCOME -> Txn(
                    type = f.type,
                    amountMinor = amount,
                    category = f.category.name,
                    accountId = from?.accountId,
                    cardId = from?.cardId,
                    note = f.note.trim(),
                    dateEpochDay = f.date.toEpochDay(),
                )
                TxnType.TRANSFER -> Txn(
                    type = f.type,
                    amountMinor = amount,
                    category = (if (to?.isCard == true) TxnCategory.CARD_PAYMENT else TxnCategory.TRANSFER).name,
                    accountId = from?.accountId,
                    toAccountId = to?.accountId,
                    cardId = to?.cardId,
                    note = f.note.trim(),
                    dateEpochDay = f.date.toEpochDay(),
                )
            }
            txns.add(txn)
            onSaved()
        }
    }

    /** Turns the placeholder cash option into a real "Cash in hand" account the first time it is used. */
    private suspend fun resolve(option: MoneyOption): MoneyOption {
        if (!option.isNewCash) return option
        val cash = Account(name = NEW_CASH_NAME, type = AccountType.CASH)
        wallet.save(cash)
        return option.copy(accountId = cash.id)
    }

    private companion object {
        const val NEW_CASH_NAME = "Cash in hand"
    }
}
