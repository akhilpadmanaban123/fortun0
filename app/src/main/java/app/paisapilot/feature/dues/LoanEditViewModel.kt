package app.paisapilot.feature.dues

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.finance.EmiCalculator
import app.paisapilot.core.finance.LoanStatus
import app.paisapilot.core.finance.status
import app.paisapilot.core.model.LoanType
import app.paisapilot.core.util.Money
import app.paisapilot.core.util.toLocalDate
import app.paisapilot.data.local.entity.Account
import app.paisapilot.data.local.entity.Loan
import app.paisapilot.data.repository.LoanRepository
import app.paisapilot.data.repository.WalletRepository
import app.paisapilot.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoanForm(
    val isEdit: Boolean = false,
    val name: String = "",
    val type: LoanType = LoanType.PERSONAL,
    val lender: String = "",
    val principal: String = "",
    val rate: String = "",
    val tenure: String = "",
    val emi: String = "",
    val firstEmi: LocalDate = LocalDate.now(),
    val paid: String = "0",
    /** What the lender says is still owed today; optional, and only asked when adding. */
    val outstanding: String = "",
    val accountId: String? = null,
    val autopay: Boolean = false,
    /** Once the user types their own EMI or paid count, we stop overwriting it. */
    val emiEdited: Boolean = false,
    val paidEdited: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
) {
    val calculatedEmiMinor: Long
        get() = EmiCalculator.emi(Money.parse(principal) ?: 0, rate.toDoubleOrNull() ?: 0.0, tenure.toIntOrNull() ?: 0)

    /** The loan these answers describe, once enough is filled in to work out its schedule. */
    val draft: LoanStatus?
        get() {
            val principalMinor = Money.parse(principal) ?: 0
            val months = tenure.toIntOrNull() ?: 0
            val emiMinor = Money.parse(emi) ?: 0
            if (principalMinor <= 0 || months <= 0 || emiMinor <= 0) return null
            return Loan(
                name = name, type = type, principalMinor = principalMinor,
                annualRatePct = rate.toDoubleOrNull() ?: 0.0, tenureMonths = months, emiMinor = emiMinor,
                firstEmiEpochDay = firstEmi.toEpochDay(), paidInstallments = paid.toIntOrNull() ?: 0,
            ).status()
        }
}

@HiltViewModel
class LoanEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val loans: LoanRepository,
    wallet: WalletRepository,
) : ViewModel() {

    private val loanId: String? = savedState[Routes.ARG_ID]
    private var original: Loan? = null

    private val _form = MutableStateFlow(LoanForm(isEdit = loanId != null))
    val form: StateFlow<LoanForm> = _form

    val accounts: StateFlow<List<Account>> =
        wallet.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (loanId != null) {
            viewModelScope.launch {
                val loan = loans.loan(loanId) ?: return@launch
                original = loan
                _form.value = LoanForm(
                    isEdit = true,
                    name = loan.name,
                    type = loan.type,
                    lender = loan.lender,
                    principal = Money.toInput(loan.principalMinor),
                    rate = loan.annualRatePct.toBigDecimal().stripTrailingZeros().toPlainString(),
                    tenure = loan.tenureMonths.toString(),
                    emi = Money.toInput(loan.emiMinor),
                    firstEmi = loan.firstEmiEpochDay.toLocalDate(),
                    paid = loan.paidInstallments.toString(),
                    accountId = loan.accountId,
                    autopay = loan.autopay,
                    emiEdited = true,
                    paidEdited = true,
                )
            }
        }
    }

    /** Applies a field change, then refreshes the values we derive for the user. */
    fun update(change: LoanForm.() -> LoanForm) = _form.update { current ->
        var next = current.change().copy(errors = emptyMap())
        if (!next.emiEdited) next = next.copy(emi = Money.toInput(next.calculatedEmiMinor))
        if (!next.paidEdited) next = next.copy(paid = installmentsDueBefore(next).toString())
        next
    }

    /** For a loan that is already running: how many EMI dates have passed since the first one. */
    private fun installmentsDueBefore(form: LoanForm): Int {
        val today = LocalDate.now()
        if (!form.firstEmi.isBefore(today)) return 0
        // An EMI due today is not counted: it still needs to be marked as paid.
        val months = ChronoUnit.MONTHS.between(form.firstEmi, today.minusDays(1)).toInt() + 1
        return months.coerceIn(0, form.tenure.toIntOrNull() ?: months)
    }

    fun save(onSaved: () -> Unit) {
        val f = _form.value
        val principal = Money.parse(f.principal) ?: 0
        val rate = f.rate.toDoubleOrNull()
        val tenure = f.tenure.toIntOrNull() ?: 0
        val emi = Money.parse(f.emi) ?: 0
        val paid = f.paid.toIntOrNull() ?: 0
        // The lender's figure can only be lower than ours, which means extra was paid earlier.
        val entered = Money.parse(f.outstanding)?.takeIf { !f.isEdit && f.outstanding.isNotBlank() }
        val calculated = f.draft?.outstandingMinor

        val errors = buildMap {
            if (f.name.isBlank()) put("name", "Give this loan a name, e.g. \"Bike loan\".")
            if (principal <= 0) put("principal", "Enter the loan amount.")
            if (rate == null || rate < 0 || rate > 60) put("rate", "Enter the yearly interest rate (0 for no-cost EMI).")
            if (tenure !in 1..480) put("tenure", "Enter the number of months (1 to 480).")
            if (emi <= 0) put("emi", "Enter the monthly EMI.")
            if (paid !in 0..tenure.coerceAtLeast(0)) put("paid", "This cannot be more than the total months.")
            if (entered != null && calculated != null && entered > calculated) {
                put("outstanding", "More than the ${Money.format(calculated)} we calculate. Check the EMIs paid, or leave this blank.")
            }
        }
        if (errors.isNotEmpty()) {
            _form.update { it.copy(errors = errors) }
            return
        }

        val base = original ?: Loan(
            name = "", type = f.type, principalMinor = 0, annualRatePct = 0.0,
            tenureMonths = 0, emiMinor = 0, firstEmiEpochDay = 0,
        )
        viewModelScope.launch {
            loans.save(
                base.copy(
                    name = f.name.trim(),
                    type = f.type,
                    lender = f.lender.trim(),
                    principalMinor = principal,
                    annualRatePct = rate ?: 0.0,
                    tenureMonths = tenure,
                    emiMinor = emi,
                    firstEmiEpochDay = f.firstEmi.toEpochDay(),
                    paidInstallments = paid,
                    accountId = f.accountId,
                    autopay = f.autopay,
                    closed = paid >= tenure,
                )
            )
            // Bring the loan in line with the lender: the gap is extra principal already paid.
            if (entered != null && calculated != null && entered < calculated) {
                loans.recordEarlierPrepayment(base.id, calculated - entered, afterInstallment = paid)
            }
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = loanId ?: return
        viewModelScope.launch {
            loans.delete(id)
            onDeleted()
        }
    }
}
